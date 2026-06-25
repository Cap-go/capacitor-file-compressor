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
    private static final float MIN_QUALITY = 0.1f;
    private static final float QUALITY_STEP = 0.05f;

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

        if (!mimeType.equals("image/jpeg") && !mimeType.equals("image/webp")) {
            call.reject("Only image/jpeg and image/webp are supported on Android");
            return;
        }

        if (quality < 0.0f || quality > 1.0f) {
            call.reject("quality must be between 0.0 and 1.0");
            return;
        }

        try {
            long maxBytes = getOriginalFileSize(path);

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

            File compressedFile = compressBitmap(processedBitmap, quality, mimeType, maxBytes);
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

    private File compressBitmap(Bitmap bitmap, float quality, String mimeType, long maxBytes) {
        Bitmap.CompressFormat format;
        String extension;

        if (mimeType.equals("image/webp")) {
            format = Bitmap.CompressFormat.WEBP;
            extension = ".webp";
        } else {
            format = Bitmap.CompressFormat.JPEG;
            extension = ".jpg";
        }

        float currentQuality = quality;
        File lastFile = null;

        while (true) {
            File compressedFile = writeCompressedBitmap(bitmap, format, extension, currentQuality);
            if (compressedFile == null) {
                if (lastFile != null) {
                    return lastFile;
                }
                return null;
            }

            if (lastFile != null && lastFile != compressedFile) {
                lastFile.delete();
            }

            if (maxBytes <= 0 || compressedFile.length() <= maxBytes || currentQuality <= MIN_QUALITY) {
                return compressedFile;
            }

            lastFile = compressedFile;
            currentQuality = Math.max(MIN_QUALITY, currentQuality - QUALITY_STEP);
        }
    }

    private File writeCompressedBitmap(Bitmap bitmap, Bitmap.CompressFormat format, String extension, float quality) {
        try {
            File tempDir = getContext().getCacheDir();
            String fileName = "compressed_" + UUID.randomUUID().toString() + extension;
            File compressedFile = new File(tempDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(compressedFile);
            int qualityInt = Math.round(quality * 100);
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
