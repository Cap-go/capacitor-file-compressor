import Foundation
import Capacitor
import UIKit

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

        if mimeType != "image/jpeg" {
            call.reject("Only image/jpeg is supported on iOS")
            return
        }

        if quality < 0.0 || quality > 1.0 {
            call.reject("quality must be between 0.0 and 1.0")
            return
        }

        guard let image = loadImage(from: path) else {
            call.reject("Failed to load image from path")
            return
        }

        let targetSize = calculateTargetSize(
            image: image,
            maxWidth: width.map { CGFloat($0) },
            maxHeight: height.map { CGFloat($0) }
        )
        let processedImage = resizeImage(image: image, targetSize: targetSize)
        let maxBytes = fileSize(at: path)

        guard let imageData = jpegData(for: processedImage, quality: CGFloat(quality), maxBytes: maxBytes) else {
            call.reject("Failed to compress image")
            return
        }

        let tempDirectory = FileManager.default.temporaryDirectory
        let fileName = "compressed_\(UUID().uuidString).jpg"
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

    private func loadImage(from path: String) -> UIImage? {
        if path.hasPrefix("file://") {
            let url = URL(string: path)
            if let url = url, let data = try? Data(contentsOf: url) {
                return UIImage(data: data)
            }
        } else if path.hasPrefix("/") {
            if let data = try? Data(contentsOf: URL(fileURLWithPath: path)) {
                return UIImage(data: data)
            }
        } else if path.hasPrefix("content://") || path.hasPrefix("file:///") {
            if let url = URL(string: path), let data = try? Data(contentsOf: url) {
                return UIImage(data: data)
            }
        }

        return nil
    }

    private func fileURL(from path: String) -> URL? {
        if path.hasPrefix("file://") {
            return URL(string: path)
        }

        if path.hasPrefix("/") {
            return URL(fileURLWithPath: path)
        }

        return nil
    }

    private func fileSize(at path: String) -> Int? {
        guard let url = fileURL(from: path) else {
            return nil
        }

        guard let attributes = try? FileManager.default.attributesOfItem(atPath: url.path),
              let size = attributes[.size] as? NSNumber else {
            return nil
        }

        return size.intValue
    }

    private func calculateTargetSize(image: UIImage, maxWidth: CGFloat?, maxHeight: CGFloat?) -> CGSize {
        let sourceWidth = image.size.width * image.scale
        let sourceHeight = image.size.height * image.scale

        if maxWidth == nil && maxHeight == nil {
            return image.size
        }

        var ratio = CGFloat(1)

        if let maxWidth = maxWidth, let maxHeight = maxHeight {
            ratio = min(maxWidth / sourceWidth, maxHeight / sourceHeight, 1)
        } else if let maxWidth = maxWidth {
            ratio = min(maxWidth / sourceWidth, 1)
        } else if let maxHeight = maxHeight {
            ratio = min(maxHeight / sourceHeight, 1)
        }

        let targetWidth = sourceWidth * ratio
        let targetHeight = sourceHeight * ratio

        return CGSize(width: targetWidth / image.scale, height: targetHeight / image.scale)
    }

    private func resizeImage(image: UIImage, targetSize: CGSize) -> UIImage {
        if targetSize == image.size {
            return image
        }

        let renderer = UIGraphicsImageRenderer(size: targetSize)
        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: targetSize))
        }
    }

    private func jpegData(for image: UIImage, quality: CGFloat, maxBytes: Int?) -> Data? {
        var currentQuality = quality

        while true {
            guard let data = image.jpegData(compressionQuality: currentQuality) else {
                return nil
            }

            let withinSizeLimit = maxBytes.map { data.count <= $0 } ?? true
            if withinSizeLimit || currentQuality <= minimumQuality {
                return data
            }

            currentQuality = max(minimumQuality, currentQuality - qualityStep)
        }
    }
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }
}
