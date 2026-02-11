// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorFileCompressor",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapgoCapacitorFileCompressor",
            targets: ["FileCompressorPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.1.0")
    ],
    targets: [
        .target(
            name: "FileCompressorPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/FileCompressorPlugin"),
        .testTarget(
            name: "FileCompressorPluginTests",
            dependencies: ["FileCompressorPlugin"],
            path: "ios/Tests/FileCompressorPluginTests")
    ]
)
