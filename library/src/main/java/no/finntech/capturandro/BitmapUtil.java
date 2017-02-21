package no.finntech.capturandro;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.FileProvider;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

class BitmapUtil {
    private static final Random random = new Random();

    private BitmapUtil() {
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    static Uri getProcessedImage(Context context, File inFile, int longestSide, int orientation) {
        Bitmap bitmap;
        bitmap = decodeBitmapFile(inFile, longestSide);
        Bitmap rotatedBitmap = rotateBitmap(bitmap, orientation);
        return saveBitmap(context, rotatedBitmap);
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    static Uri getProcessedImage(Context context, InputStream inputStream, int longestSide, int orientation) {
        try {
            File file = new File(getUniqueFilename(context));
            FileOutputStream fos = new FileOutputStream(file);
            copy(inputStream, fos);

            Bitmap bitmap = decodeBitmapFile(file, longestSide);
            Bitmap rotatedBitmap = rotateBitmap(bitmap, orientation);
            file.delete();
            return saveBitmap(context, rotatedBitmap);
        } catch (IOException e) {
            return Uri.EMPTY;
        }
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    static String getUniqueFilename(Context context) {
        return new File(context.getExternalCacheDir().getAbsolutePath(), "capturandro-" + System.currentTimeMillis() + "." + random.nextInt() + ".jpg").toString();
    }

    @RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static Uri saveBitmap(Context context, Bitmap bitmap) throws IllegalArgumentException {
        FileOutputStream out = null;
        String filename = getUniqueFilename(context);
        int compressionPercentage = Capturandro.DEFAULT_STORED_IMAGE_COMPRESSION_PERCENT;
        try {
            out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionPercentage, out);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } finally {
            closeQuietly(out);
        }
        File file = new File(filename);
        if (Build.VERSION.SDK_INT >= 24) {
            return FileProvider.getUriForFile(context,
                    Capturandro.getFileProviderAuthority(),
                    file);
        } else {
            return Uri.fromFile(file);
        }
    }

    private static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        long count = 0;
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    private static Bitmap rotateBitmap(Bitmap sourceBitmap, int orientation) {
        Matrix transformationMatrix = new Matrix();
        transformationMatrix.postRotate(orientation);
        // Bitmap is immutable, so we need to create a new one based on the transformation
        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), transformationMatrix, true);
    }

    // Loosely based on code found in
    // http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object/823966#823966
    private static Bitmap decodeBitmapFile(File file, int longestSide) throws IllegalArgumentException {
        FileInputStream inJustDecodeBoundsImageStream = null;
        FileInputStream inSampleSizeImageStream = null;
        try {

            // Decode image size
            inJustDecodeBoundsImageStream = new FileInputStream(file);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inJustDecodeBoundsImageStream, null, o);

            // Decode actual image
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = calculateInSampleSize(o, longestSide);
            inSampleSizeImageStream = new FileInputStream(file);

            return BitmapFactory.decodeStream(inSampleSizeImageStream, null, o2);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } finally {
            closeQuietly(inJustDecodeBoundsImageStream);
            closeQuietly(inSampleSizeImageStream);
        }
    }


    private static int calculateInSampleSize(BitmapFactory.Options options, int longestSide) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;

        int sampleSize = Math.max(height, width) / longestSide;
        if (sampleSize > 1) {
            return sampleSize;
        } else {
            return 1;
        }
    }
}