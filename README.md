# @capgo/capacitor-file-compressor
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin_file-compressor"> ➡️ Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin_file-compressor"> Missing a feature? We'll build the plugin for you 💪</a></h2>
</div>

Capacitor plugin for efficient image compression supporting PNG, JPEG, and WebP formats across iOS, Android, and Web platforms.

## Why File Compressor?

A free, open-source alternative for **client-side image compression**:

- **Multiple formats** - JPEG and WebP compression support
- **Quality control** - Adjustable compression quality (0.0 - 1.0)
- **Smart resizing** - Automatic aspect ratio preservation
- **Cross-platform** - Consistent API across iOS, Android, and Web
- **Zero backend** - All compression happens on the device

Essential for apps that need to optimize image uploads, reduce storage, or improve performance without server-side processing.

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/file-compressor/

## Install

```bash
npm install @capgo/capacitor-file-compressor
npx cap sync
```

## Platform Support

| Platform | Supported Formats | Notes |
|----------|-------------------|-------|
| iOS | JPEG | Only JPEG compression supported |
| Android | JPEG, WebP | Both formats fully supported |
| Web | JPEG, WebP | Canvas API-based compression |

Note: EXIF metadata is removed during compression on all platforms.

## API

<docgen-index>

* [`compressImage(...)`](#compressimage)
* [`getPluginVersion()`](#getpluginversion)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Capacitor File Compressor Plugin interface for image compression.

### compressImage(...)

```typescript
compressImage(options: CompressImageOptions) => Promise<CompressImageResult>
```

Compresses an image file with specified dimensions and quality settings.

This method compresses images to reduce file size while maintaining acceptable quality.
It supports resizing and format conversion (JPEG/WebP depending on platform).

**Important Notes:**
- EXIF metadata is removed during compression on all platforms
- Aspect ratio is automatically maintained if only one dimension is provided
- Compressed files are saved to temporary directories on native platforms

| Param         | Type                                                                  | Description                                   |
| ------------- | --------------------------------------------------------------------- | --------------------------------------------- |
| **`options`** | <code><a href="#compressimageoptions">CompressImageOptions</a></code> | - Configuration options for image compression |

**Returns:** <code>Promise&lt;<a href="#compressimageresult">CompressImageResult</a>&gt;</code>

**Since:** 7.0.0

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<{ version: string; }>
```

Get the native Capacitor plugin version.

Returns the version of the native plugin implementation.
Useful for debugging and ensuring compatibility.

**Returns:** <code>Promise&lt;{ version: string; }&gt;</code>

**Since:** 7.0.0

--------------------


### Interfaces


#### CompressImageResult

The result of compressing an image.

Contains either a file path (native platforms) or a Blob (web platform)
depending on where the compression was performed.

| Prop       | Type                | Description                                                                                                                                                                                                                                                                                                                                                       | Since |
| ---------- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| **`path`** | <code>string</code> | The file path of the compressed image. **Platform:** Android, iOS only (undefined on Web) Points to a temporary file containing the compressed image. On iOS, typically in `NSTemporaryDirectory()`. On Android, typically in app cache directory. **Important:** These files may be cleaned up by the OS. Copy to permanent storage if needed for long-term use. | 7.0.0 |
| **`blob`** | <code>Blob</code>   | The blob of the compressed image. **Platform:** Web only (undefined on iOS/Android) A Blob object containing the compressed image data. Can be used to: - Create object URLs for preview: `URL.createObjectURL(blob)` - Upload to server via FormData - Save to IndexedDB or other storage - Convert to base64 with FileReader                                    | 7.0.0 |


#### CompressImageOptions

Options for compressing an image.

Configure the compression behavior including quality, dimensions, and output format.
Platform-specific options are available for path (native) and blob (web).

| Prop           | Type                | Description                                                                                                                                                                                                                                                                                                                                                                                             | Default                   | Since |
| -------------- | ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------- | ----- |
| **`path`**     | <code>string</code> | The file path of the image to compress. **Platform:** Android, iOS only (not supported on Web) Accepts various path formats: - iOS: `file://` URLs or absolute paths - Android: `content://` URIs, `file://` URLs, or absolute paths                                                                                                                                                                    |                           | 7.0.0 |
| **`blob`**     | <code>Blob</code>   | The file blob of the image to compress. **Platform:** Web only (not supported on iOS/Android) Use this when compressing images from file inputs, fetch responses, or any other Blob source in web applications.                                                                                                                                                                                         |                           | 7.0.0 |
| **`quality`**  | <code>number</code> | The quality of the compressed image. **Range:** 0.0 to 1.0 - `0.0` = Maximum compression (lowest quality, smallest file) - `1.0` = Minimum compression (highest quality, largest file) - `0.6` = Default balanced compression **Platform:** All platforms Higher quality values result in larger files but better visual quality. The actual compression ratio depends on the image content and format. | <code>0.6</code>          | 7.0.0 |
| **`width`**    | <code>number</code> | The width of the compressed image in pixels. **Platform:** All platforms If only width is specified, height is calculated automatically to maintain the original aspect ratio. If both width and height are specified, the image is resized to exact dimensions (may distort if ratio differs).                                                                                                         |                           | 7.0.0 |
| **`height`**   | <code>number</code> | The height of the compressed image in pixels. **Platform:** All platforms If only height is specified, width is calculated automatically to maintain the original aspect ratio. If both width and height are specified, the image is resized to exact dimensions (may distort if ratio differs).                                                                                                        |                           | 7.0.0 |
| **`mimeType`** | <code>string</code> | The MIME type of the compressed output image. **Platform Support:** - **iOS:** `image/jpeg` only - **Android:** `image/jpeg`, `image/webp` - **Web:** `image/jpeg`, `image/webp` **Format Characteristics:** - **JPEG:** Universal support, good for photos, no transparency - **WebP:** Better compression, supports transparency, not on iOS                                                          | <code>"image/jpeg"</code> | 7.0.0 |

</docgen-api>
