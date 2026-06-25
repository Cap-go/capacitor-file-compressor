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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@CapacitorPlugin(name = "FileCompressor")
public class FileCompressorPlugin extends Plugin {

    private final String pluginVersion = "8.1.0";
    private static final float MIN_QUALITY = 0.1f;
    private static final float QUALITY_STEP = 0.05f;
    private static final List<String> SUPPORTED_OUTPUT_MIME_TYPES = Arrays.asList("image/jpeg", "image/png", "image/webp");

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
        String mimeType = call.getString("mimeType", "image/jpeg").toLowerCase();

        if (!SUPPORTED_OUTPUT_MIME_TYPES.contains(mimeType)) {
            call.reject("Unsupported output mimeType: " + mimeType + ". Supported: " + String.join(", ", SUPPORTED_OUTPUT_MIME_TYPES));
            return;
        }

        if (quality < 0.0f || quality > 1.0f) {
            call.reject("quality must be between 0.0 and 1.0");
            return;
        }

        try {
            long maxBytes = getOriginalFileSize(path);
            OutputFormat outputFormat = OutputFormat.fromMimeType(mimeType);

            Bitmap bitmap = loadBitmapFromPath(path);
            if (bitmap == null) {
                call.reject("Failed to load image from path");
                return;
            }

            int[] targetDimensions = calculateTargetDimensions(bitmap, width, height);
            Bitmap processedBitmap = bitmap;
            if (targetDimensions[0] != bitmap.getWidth() || targetDimensions[1] != bitmap.getHeight()) {
                processedBitmap = Bitmap.createScaledBitmap(bitmap, targetDimensions[0], targetDimensions[1], true);
            }

            File compressedFile = compressBitmap(processedBitmap, quality, outputFormat, maxBytes);
            if (compressedFile == null) {
                call.reject("Failed to compress image");
                return;
            }

            if (processedBitmap != bitmap) {
                processedBitmap.recycle();
            }
            bitmap.recycle();

            JSObject result = new JSObject();
            result.put("path", compressedFile.getAbsolutePath());
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Error compressing image: " + e.getMessage(), e);
        }
    }

    private int[] calculateTargetDimensions(Bitmap bitmap, Integer maxWidth, Integer maxHeight) {
        int sourceWidth = bitmap.getWidth();
        int sourceHeight = bitmap.getHeight();

        if (maxWidth == null && maxHeight == null) {
            return new int[] { sourceWidth, sourceHeight };
        }

        float ratio = 1f;

        if (maxWidth != null && maxHeight != null) {
            ratio = Math.min((float) maxWidth / sourceWidth, (float) maxHeight / sourceHeight);
        } else if (maxWidth != null) {
            ratio = (float) maxWidth / sourceWidth;
        } else if (maxHeight != null) {
            ratio = (float) maxHeight / sourceHeight;
        }

        ratio = Math.min(ratio, 1f);

        return new int[] { Math.round(sourceWidth * ratio), Math.round(sourceHeight * ratio) };
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
            if (path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    inputStream.close();
                    return bitmap;
                }
            } else if (path.startsWith("file://")) {
                path = path.substring(7);
            }

            File file = new File(path);
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File compressBitmap(Bitmap bitmap, float quality, OutputFormat outputFormat, long maxBytes) {
        float currentQuality = quality;
        File lastFile = null;

        while (true) {
            File compressedFile = writeCompressedBitmap(bitmap, outputFormat, currentQuality);
            if (compressedFile == null) {
                return lastFile;
            }

            if (lastFile != null && lastFile != compressedFile) {
                lastFile.delete();
            }

            if (maxBytes <= 0 || compressedFile.length() <= maxBytes || currentQuality <= MIN_QUALITY || !outputFormat.supportsQuality) {
                return compressedFile;
            }

            lastFile = compressedFile;
            currentQuality = Math.max(MIN_QUALITY, currentQuality - QUALITY_STEP);
        }
    }

    private File writeCompressedBitmap(Bitmap bitmap, OutputFormat outputFormat, float quality) {
        try {
            File tempDir = getContext().getCacheDir();
            String fileName = "compressed_" + UUID.randomUUID().toString() + outputFormat.extension;
            File compressedFile = new File(tempDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(compressedFile);
            int qualityInt = Math.round(quality * 100);
            bitmap.compress(outputFormat.format, qualityInt, outputStream);
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

    private static final class OutputFormat {

        private final Bitmap.CompressFormat format;
        private final String extension;
        private final boolean supportsQuality;

        private OutputFormat(Bitmap.CompressFormat format, String extension, boolean supportsQuality) {
            this.format = format;
            this.extension = extension;
            this.supportsQuality = supportsQuality;
        }

        private static OutputFormat fromMimeType(String mimeType) {
            switch (mimeType) {
                case "image/webp":
                    return new OutputFormat(Bitmap.CompressFormat.WEBP, ".webp", true);
                case "image/png":
                    return new OutputFormat(Bitmap.CompressFormat.PNG, ".png", false);
                case "image/jpeg":
                default:
                    return new OutputFormat(Bitmap.CompressFormat.JPEG, ".jpg", true);
            }
        }
    }
}
