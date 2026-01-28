import Foundation
import Capacitor
import UIKit

@objc(FileCompressorPlugin)
public class FileCompressorPlugin: CAPPlugin, CAPBridgedPlugin {
    private let pluginVersion: String = "8.0.10"
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

        // Validate mime type for iOS (only JPEG supported)
        if mimeType != "image/jpeg" {
            call.reject("Only image/jpeg is supported on iOS")
            return
        }

        // Validate quality range
        if quality < 0.0 || quality > 1.0 {
            call.reject("quality must be between 0.0 and 1.0")
            return
        }

        // Load image from path
        guard let image = loadImage(from: path) else {
            call.reject("Failed to load image from path")
            return
        }

        // Resize image if dimensions are provided
        var processedImage = image
        if let targetWidth = width, let targetHeight = height {
            processedImage = resizeImage(image: image, targetWidth: CGFloat(targetWidth), targetHeight: CGFloat(targetHeight))
        } else if let targetWidth = width {
            let aspectRatio = image.size.height / image.size.width
            let targetHeight = CGFloat(targetWidth) * aspectRatio
            processedImage = resizeImage(image: image, targetWidth: CGFloat(targetWidth), targetHeight: targetHeight)
        } else if let targetHeight = height {
            let aspectRatio = image.size.width / image.size.height
            let targetWidth = CGFloat(targetHeight) * aspectRatio
            processedImage = resizeImage(image: image, targetWidth: targetWidth, targetHeight: CGFloat(targetHeight))
        }

        // Compress image to JPEG
        guard let imageData = processedImage.jpegData(compressionQuality: CGFloat(quality)) else {
            call.reject("Failed to compress image")
            return
        }

        // Save compressed image to temporary file
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
        // Handle different path types
        if path.hasPrefix("file://") {
            let url = URL(string: path)
            if let url = url, let data = try? Data(contentsOf: url) {
                return UIImage(data: data)
            }
        } else if path.hasPrefix("/") {
            // Absolute file path
            if let data = try? Data(contentsOf: URL(fileURLWithPath: path)) {
                return UIImage(data: data)
            }
        } else if path.hasPrefix("content://") || path.hasPrefix("file:///") {
            // Try to parse as URL
            if let url = URL(string: path), let data = try? Data(contentsOf: url) {
                return UIImage(data: data)
            }
        }

        // Try loading from photo library asset identifier
        // This would require Photos framework integration

        return nil
    }

    private func resizeImage(image: UIImage, targetWidth: CGFloat, targetHeight: CGFloat) -> UIImage {
        let size = CGSize(width: targetWidth, height: targetHeight)
        let renderer = UIGraphicsImageRenderer(size: size)

        return renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: size))
        }
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }
}
