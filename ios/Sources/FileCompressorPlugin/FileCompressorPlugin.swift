import Foundation
import Capacitor
import UIKit
import ImageIO
import UniformTypeIdentifiers

@objc(FileCompressorPlugin)
public class FileCompressorPlugin: CAPPlugin, CAPBridgedPlugin {
    private let pluginVersion: String = "8.0.34"
    private let minimumQuality: CGFloat = 0.1
    private let qualityStep: CGFloat = 0.05
    public let identifier = "FileCompressorPlugin"
    public let jsName = "FileCompressor"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "compressImage", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    @objc func compressImage(_ call: CAPPluginCall) {
        guard let path = call.getString("path") else {
            call.reject("path is required")
            return
        }

        let quality = call.getFloat("quality") ?? 0.6
        let width = call.getInt("width")
        let height = call.getInt("height")
        let mimeType = call.getString("mimeType") ?? "image/jpeg"

        guard let outputFormat = ImageOutputFormat(mimeType: mimeType) else {
            call.reject("Unsupported output mimeType: \(mimeType). Supported: \(ImageOutputFormat.supportedMimeTypes.joined(separator: ", "))")
            return
        }

        if quality < 0.0 || quality > 1.0 {
            call.reject("quality must be between 0.0 and 1.0")
            return
        }

        guard let sourceURL = resolveFileURL(from: path) else {
            call.reject("Failed to resolve image path")
            return
        }

        guard let sourceImage = loadCGImage(from: sourceURL) else {
            call.reject("Failed to load image from path")
            return
        }

        let targetSize = calculateTargetSize(
            width: CGFloat(sourceImage.width),
            height: CGFloat(sourceImage.height),
            maxWidth: width.map { CGFloat($0) },
            maxHeight: height.map { CGFloat($0) }
        )
        let processedImage = resizeCGImage(sourceImage, targetSize: targetSize)
        let maxBytes = fileSize(at: sourceURL)

        guard let imageData = encodeImage(
            processedImage,
            format: outputFormat,
            quality: CGFloat(quality),
            maxBytes: maxBytes
        ) else {
            call.reject("Failed to compress image")
            return
        }

        let tempDirectory = FileManager.default.temporaryDirectory
        let fileName = "compressed_\(UUID().uuidString).\(outputFormat.fileExtension)"
        let fileURL = tempDirectory.appendingPathComponent(fileName)

        do {
            try imageData.write(to: fileURL)
            call.resolve([
                "path": fileURL.path
            ])
        } catch {
            call.reject("Failed to save compressed image: \(error.localizedDescription)")
        }
    }

    private func resolveFileURL(from path: String) -> URL? {
        if path.hasPrefix("file://") {
            return URL(string: path)
        }

        if path.hasPrefix("/") {
            return URL(fileURLWithPath: path)
        }

        if let url = URL(string: path), url.scheme != nil {
            return url
        }

        return nil
    }

    private func fileSize(at url: URL) -> Int? {
        guard let attributes = try? FileManager.default.attributesOfItem(atPath: url.path),
              let size = attributes[.size] as? NSNumber else {
            return nil
        }

        return size.intValue
    }

    private func loadCGImage(from url: URL) -> CGImage? {
        guard let source = CGImageSourceCreateWithURL(url as CFURL, nil) else {
            return nil
        }

        return CGImageSourceCreateImageAtIndex(source, 0, nil)
    }

    private func calculateTargetSize(
        width: CGFloat,
        height: CGFloat,
        maxWidth: CGFloat?,
        maxHeight: CGFloat?
    ) -> CGSize {
        if maxWidth == nil && maxHeight == nil {
            return CGSize(width: width, height: height)
        }

        var ratio = CGFloat(1)

        if let maxWidth = maxWidth, let maxHeight = maxHeight {
            ratio = min(maxWidth / width, maxHeight / height, 1)
        } else if let maxWidth = maxWidth {
            ratio = min(maxWidth / width, 1)
        } else if let maxHeight = maxHeight {
            ratio = min(maxHeight / height, 1)
        }

        return CGSize(width: width * ratio, height: height * ratio)
    }

    private func resizeCGImage(_ image: CGImage, targetSize: CGSize) -> CGImage {
        let targetWidth = Int(targetSize.width.rounded())
        let targetHeight = Int(targetSize.height.rounded())

        if targetWidth == image.width && targetHeight == image.height {
            return image
        }

        guard let context = CGContext(
            data: nil,
            width: targetWidth,
            height: targetHeight,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else {
            return image
        }

        context.interpolationQuality = .high
        context.draw(image, in: CGRect(x: 0, y: 0, width: targetWidth, height: targetHeight))

        return context.makeImage() ?? image
    }

    private func encodeImage(
        _ image: CGImage,
        format: ImageOutputFormat,
        quality: CGFloat,
        maxBytes: Int?
    ) -> Data? {
        var currentQuality = quality

        while true {
            guard let data = encodeImage(image, format: format, quality: currentQuality) else {
                return nil
            }

            let withinSizeLimit = maxBytes.map { data.count <= $0 } ?? true
            if withinSizeLimit || currentQuality <= minimumQuality || !format.supportsQuality {
                return data
            }

            currentQuality = max(minimumQuality, currentQuality - qualityStep)
        }
    }

    private func encodeImage(_ image: CGImage, format: ImageOutputFormat, quality: CGFloat) -> Data? {
        let data = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(data, format.utType, 1, nil) else {
            return nil
        }

        var properties: [CFString: Any] = [:]
        if format.supportsQuality {
            properties[kCGImageDestinationLossyCompressionQuality] = quality
        }

        CGImageDestinationAddImage(destination, image, properties as CFDictionary)

        guard CGImageDestinationFinalize(destination) else {
            return nil
        }

        return data as Data
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }
}

private struct ImageOutputFormat {
    let mimeType: String
    let utType: CFString
    let fileExtension: String
    let supportsQuality: Bool

    init(mimeType: String, utType: CFString, fileExtension: String, supportsQuality: Bool) {
        self.mimeType = mimeType
        self.utType = utType
        self.fileExtension = fileExtension
        self.supportsQuality = supportsQuality
    }

    init?(mimeType: String) {
        switch mimeType.lowercased() {
        case "image/jpeg", "image/jpg":
            self.init(
                mimeType: "image/jpeg",
                utType: UTType.jpeg.identifier as CFString,
                fileExtension: "jpg",
                supportsQuality: true
            )
        case "image/png":
            self.init(
                mimeType: "image/png",
                utType: UTType.png.identifier as CFString,
                fileExtension: "png",
                supportsQuality: false
            )
        case "image/heif", "image/heic":
            self.init(
                mimeType: "image/heif",
                utType: UTType.heic.identifier as CFString,
                fileExtension: "heic",
                supportsQuality: true
            )
        case "image/webp":
            if #available(iOS 14.0, *) {
                self.init(
                    mimeType: "image/webp",
                    utType: UTType.webP.identifier as CFString,
                    fileExtension: "webp",
                    supportsQuality: true
                )
            } else {
                return nil
            }
        default:
            return nil
        }
    }

    static var supportedMimeTypes: [String] {
        if #available(iOS 14.0, *) {
            return ["image/jpeg", "image/png", "image/heif", "image/heic", "image/webp"]
        }

        return ["image/jpeg", "image/png", "image/heif", "image/heic"]
    }
}
