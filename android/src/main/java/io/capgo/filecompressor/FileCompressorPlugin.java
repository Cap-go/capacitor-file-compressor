package io.capgo.filecompressor;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@CapacitorPlugin(name = "FileCompressor")
public class FileCompressorPlugin extends Plugin {

    private final String pluginVersion = "8.0.34";

    @PluginMethod
    public void compressImage(PluginCall call) {
        String path = call.getString("path");
        if (path == null) {
            call.reject("path is required");
            return;
        }

        float quality = call.getFloat("quality", 0.6f);
        Integer width = call.getInt("width");
        Integer height = call.getInt("height");
        String mimeType = call.getString("mimeType", "image/jpeg");

        // Validate mime type for Android
        if (!mimeType.equals("image/jpeg") && !mimeType.equals("image/webp")) {
            call.reject("Only image/jpeg and image/webp are supported on Android");
            return;
        }

        // Validate quality range
        if (quality < 0.0f || quality > 1.0f) {
            call.reject("quality must be between 0.0 and 1.0");
            return;
        }

        try {
            long originalSize = getOriginalFileSize(path);

            // Load bitmap from path
            Bitmap bitmap = loadBitmapFromPath(path);
            if (bitmap == null) {
                call.reject("Failed to load image from path");
                return;
            }

            // Resize bitmap if dimensions are provided
            Bitmap processedBitmap = bitmap;
            if (width != null && height != null) {
                processedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            } else if (width != null) {
                float aspectRatio = (float) bitmap.getHeight() / bitmap.getWidth();
                int targetHeight = (int) (width * aspectRatio);
                processedBitmap = Bitmap.createScaledBitmap(bitmap, width, targetHeight, true);
            } else if (height != null) {
                float aspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();
                int targetWidth = (int) (height * aspectRatio);
                processedBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, height, true);
            }

            // Compress bitmap
            File compressedFile = compressBitmap(processedBitmap, quality, mimeType);
            if (compressedFile == null) {
                call.reject("Failed to compress image");
                return;
            }

            // Clean up
            if (processedBitmap != bitmap) {
                processedBitmap.recycle();
            }
            bitmap.recycle();

            // Return result
            JSObject result = new JSObject();
            if (originalSize > 0 && compressedFile.length() > originalSize) {
                compressedFile.delete();
                result.put("path", path);
            } else {
                result.put("path", compressedFile.getAbsolutePath());
            }
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Error compressing image: " + e.getMessage(), e);
        }
    }

    private long getOriginalFileSize(String path) {
        try {
            if (path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                try (AssetFileDescriptor descriptor = getContext().getContentResolver().openAssetFileDescriptor(uri, "r")) {
                    if (descriptor != null) {
                        return descriptor.getLength();
                    }
                }
            } else {
                String filePath = path.startsWith("file://") ? path.substring(7) : path;
                File file = new File(filePath);
                if (file.exists()) {
                    return file.length();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private Bitmap loadBitmapFromPath(String path) {
        try {
            // Handle content:// URIs
            if (path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    return bitmap;
                }
            }
            // Handle file:// URIs and absolute paths
            else if (path.startsWith("file://")) {
                path = path.substring(7); // Remove "file://" prefix
            }

            // Try loading as file path
            File file = new File(path);
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File compressBitmap(Bitmap bitmap, float quality, String mimeType) {
        try {
            // Determine compression format and file extension
            Bitmap.CompressFormat format;
            String extension;

            if (mimeType.equals("image/webp")) {
                format = Bitmap.CompressFormat.WEBP;
                extension = ".webp";
            } else {
                format = Bitmap.CompressFormat.JPEG;
                extension = ".jpg";
            }

            // Create temporary file
            File tempDir = getContext().getCacheDir();
            String fileName = "compressed_" + UUID.randomUUID().toString() + extension;
            File compressedFile = new File(tempDir, fileName);

            // Compress and save
            FileOutputStream outputStream = new FileOutputStream(compressedFile);
            int qualityInt = (int) (quality * 100);
            bitmap.compress(format, qualityInt, outputStream);
            outputStream.flush();
            outputStream.close();

            return compressedFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @PluginMethod
    public void getPluginVersion(final PluginCall call) {
        try {
            final JSObject ret = new JSObject();
            ret.put("version", this.pluginVersion);
            call.resolve(ret);
        } catch (final Exception e) {
            call.reject("Could not get plugin version", e);
        }
    }
}
