/**
 * Capacitor File Compressor Plugin interface for image compression.
 *
 * @since 7.0.0
 */
export interface FileCompressorPlugin {
  /**
   * Compresses an image file with specified dimensions and quality settings.
   *
   * This method compresses images to reduce file size while maintaining acceptable quality.
   * It supports resizing and format conversion across common image formats.
   *
   * **Supported input formats (platform decoder dependent):**
   * JPEG, PNG, WebP, GIF, BMP, TIFF, HEIF/HEIC
   *
   * **Supported output formats:**
   * - **iOS:** `image/jpeg`, `image/png`, `image/heif`, `image/heic`, `image/webp` (iOS 14+)
   * - **Android:** `image/jpeg`, `image/png`, `image/webp`
   * - **Web:** `image/jpeg`, `image/png`, `image/webp`
   *
   * **Important Notes:**
   * - EXIF metadata is removed during compression on all platforms
   * - Aspect ratio is automatically maintained if only one dimension is provided
   * - When both width and height are provided, the image fits inside that box without upscaling
   * - Compressed files are saved to temporary directories on native platforms
   * - If the encoded output would be larger than the source file, quality is reduced automatically while keeping the requested output format
   *
   * @param options - Configuration options for image compression
   * @returns Promise that resolves with the compressed image path or blob
   * @throws Error if compression fails, invalid parameters, or unsupported format
   * @since 7.0.0
   * @example
   * ```typescript
   * // Web - Compress from file input
   * const fileInput = document.getElementById('file') as HTMLInputElement;
   * const file = fileInput.files[0];
   * const result = await FileCompressor.compressImage({
   *   blob: file,
   *   quality: 0.8,
   *   width: 1200,
   *   mimeType: 'image/jpeg'
   * });
   * const url = URL.createObjectURL(result.blob);
   * ```
   * @example
   * ```typescript
   * // iOS - Convert HEIF iPhone photo to JPEG
   * const result = await FileCompressor.compressImage({
   *   path: heifFileUri,
   *   quality: 0.7,
   *   width: 1920,
   *   height: 1080,
   *   mimeType: 'image/jpeg'
   * });
   * console.log('Converted to JPEG:', result.path);
   * ```
   * @example
   * ```typescript
   * // Android - Convert PNG to WebP
   * const result = await FileCompressor.compressImage({
   *   path: 'content://downloads/document/123',
   *   quality: 0.6,
   *   width: 800,
   *   height: 600,
   *   mimeType: 'image/webp'
   * });
   * console.log('Compressed to:', result.path);
   * ```
   * @example
   * ```typescript
   * // Maintain aspect ratio - only specify width
   * const result = await FileCompressor.compressImage({
   *   path: imagePath,
   *   quality: 0.75,
   *   width: 1920,
   *   mimeType: 'image/jpeg'
   * });
   * ```
   */
  compressImage(options: CompressImageOptions): Promise<CompressImageResult>;

  /**
   * Get the native Capacitor plugin version.
   *
   * Returns the version of the native plugin implementation.
   * Useful for debugging and ensuring compatibility.
   *
   * @returns Promise that resolves with the plugin version string
   * @throws Error if getting the version fails
   * @since 7.0.0
   * @example
   * ```typescript
   * const { version } = await FileCompressor.getPluginVersion();
   * console.log('Plugin version:', version); // "7.0.0"
   * ```
   */
  getPluginVersion(): Promise<{ version: string }>;
}

/**
 * Options for compressing an image.
 *
 * Configure the compression behavior including quality, dimensions, and output format.
 * Platform-specific options are available for path (native) and blob (web).
 *
 * @since 7.0.0
 */
export interface CompressImageOptions {
  /**
   * The file path of the image to compress.
   *
   * **Platform:** Android, iOS only (not supported on Web)
   *
   * Accepts various path formats:
   * - iOS: `file://` URLs or absolute paths
   * - Android: `content://` URIs, `file://` URLs, or absolute paths
   *
   * Common input formats include JPEG, PNG, WebP, GIF, BMP, TIFF, and HEIF/HEIC.
   *
   * @since 7.0.0
   * @example "file:///var/mobile/Containers/Data/Application/photo.heic" // iOS HEIF
   * @example "content://com.android.providers.downloads.documents/document/msf%3A1000000485" // Android
   * @example "/storage/emulated/0/Download/photo.png" // Android absolute path
   */
  path?: string;

  /**
   * The file blob of the image to compress.
   *
   * **Platform:** Web only (not supported on iOS/Android)
   *
   * Use this when compressing images from file inputs, fetch responses,
   * or any other Blob source in web applications.
   *
   * @since 7.0.0
   * @example
   * ```typescript
   * // From file input
   * const fileInput = document.getElementById('file') as HTMLInputElement;
   * const blob = fileInput.files[0];
   * ```
   * @example
   * ```typescript
   * // From fetch
   * const response = await fetch('https://example.com/image.jpg');
   * const blob = await response.blob();
   * ```
   */
  blob?: Blob;

  /**
   * The quality of the compressed image.
   *
   * **Range:** 0.0 to 1.0
   * - `0.0` = Maximum compression (lowest quality, smallest file)
   * - `1.0` = Minimum compression (highest quality, largest file)
   * - `0.6` = Default balanced compression
   *
   * **Platform:** All platforms
   *
   * Higher quality values result in larger files but better visual quality.
   * The actual compression ratio depends on the image content and format.
   * PNG output ignores quality because it is lossless.
   *
   * @since 7.0.0
   * @default 0.6
   * @example 0.8 // High quality
   * @example 0.5 // Medium quality, smaller file
   * @example 0.3 // Low quality, very small file
   */
  quality?: number;

  /**
   * The width of the compressed image in pixels.
   *
   * **Platform:** All platforms
   *
   * If only width is specified, height is calculated automatically
   * to maintain the original aspect ratio.
   *
   * If both width and height are specified, the image is scaled to fit inside
   * that box while preserving aspect ratio. Images are never upscaled.
   *
   * @since 7.0.0
   * @example 1920 // Full HD width
   * @example 1200 // Common web image width
   * @example 800  // Mobile-optimized width
   */
  width?: number;

  /**
   * The height of the compressed image in pixels.
   *
   * **Platform:** All platforms
   *
   * If only height is specified, width is calculated automatically
   * to maintain the original aspect ratio.
   *
   * If both width and height are specified, the image is scaled to fit inside
   * that box while preserving aspect ratio. Images are never upscaled.
   *
   * @since 7.0.0
   * @example 1080 // Full HD height
   * @example 800  // Common web image height
   * @example 600  // Mobile-optimized height
   */
  height?: number;

  /**
   * The MIME type of the compressed output image.
   *
   * **Platform Support:**
   * - **iOS:** `image/jpeg`, `image/png`, `image/heif`, `image/heic`, `image/webp` (iOS 14+)
   * - **Android:** `image/jpeg`, `image/png`, `image/webp`
   * - **Web:** `image/jpeg`, `image/png`, `image/webp`
   *
   * Use this option to convert the source image to another format while compressing.
   *
   * **Format Characteristics:**
   * - **JPEG:** Universal support, good for photos, no transparency
   * - **PNG:** Lossless, supports transparency
   * - **WebP:** Better compression, supports transparency
   * - **HEIF/HEIC:** Efficient photo format on iOS
   *
   * @since 7.0.0
   * @default "image/jpeg"
   * @example "image/jpeg" // JPEG format
   * @example "image/png" // PNG format
   * @example "image/webp" // WebP format
   * @example "image/heif" // HEIF format (iOS output)
   */
  mimeType?: string;
}

/**
 * The result of compressing an image.
 *
 * Contains either a file path (native platforms) or a Blob (web platform)
 * depending on where the compression was performed.
 *
 * @since 7.0.0
 */
export interface CompressImageResult {
  /**
   * The file path of the compressed image.
   *
   * **Platform:** Android, iOS only (undefined on Web)
   *
   * Points to a temporary file containing the compressed image.
   * On iOS, typically in `NSTemporaryDirectory()`.
   * On Android, typically in app cache directory.
   *
   * **Important:** These files may be cleaned up by the OS.
   * Copy to permanent storage if needed for long-term use.
   *
   * @since 7.0.0
   * @example "/var/mobile/Containers/Data/tmp/compressed_abc123.jpg" // iOS
   * @example "/data/user/0/com.app/cache/compressed_xyz789.webp" // Android
   */
  path?: string;

  /**
   * The blob of the compressed image.
   *
   * **Platform:** Web only (undefined on iOS/Android)
   *
   * A Blob object containing the compressed image data.
   * Can be used to:
   * - Create object URLs for preview: `URL.createObjectURL(blob)`
   * - Upload to server via FormData
   * - Save to IndexedDB or other storage
   * - Convert to base64 with FileReader
   *
   * @since 7.0.0
   * @example
   * ```typescript
   * // Create preview URL
   * const url = URL.createObjectURL(result.blob);
   * imageElement.src = url;
   * ```
   * @example
   * ```typescript
   * // Upload to server
   * const formData = new FormData();
   * formData.append('image', result.blob, 'compressed.jpg');
   * await fetch('/upload', { method: 'POST', body: formData });
   * ```
   */
  blob?: Blob;
}
