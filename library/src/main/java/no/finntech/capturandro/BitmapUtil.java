package no.finntech.capturandro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.io.IOUtils;

class BitmapUtil {
    private static final Random random = new Random();

    private BitmapUtil() {
    }

    static Uri getProcessedImage(File inFile, int longestSide) {
        return getProcessedImage(inFile, longestSide, getOrientation(inFile));
    }

    static Uri getProcessedImage(File inFile, int longestSide, int orientation) {
        Bitmap bitmap;
        bitmap = decodeBitmapFile(inFile, longestSide);
        Bitmap rotatedBitmap = rotateBitmap(bitmap, orientation);
        return saveBitmap(rotatedBitmap);
    }

    static Uri getProcessedImage(InputStream inputStream, int longestSide, int orientation) {
        try {
            File file = new File(getUniqueFilename());
            FileOutputStream fos = new FileOutputStream(file);
            IOUtils.copy(inputStream, fos);

            Bitmap bitmap = decodeBitmapFile(file, longestSide);
            Bitmap rotatedBitmap = rotateBitmap(bitmap, orientation);
            file.delete();
            return saveBitmap(rotatedBitmap);
        } catch (IOException e) {
            return Uri.EMPTY;
        }
    }

    private static Bitmap rotateBitmap(Bitmap sourceBitmap, int orientation) {
        Matrix transformationMatrix = new Matrix();
        transformationMatrix.postRotate(orientation);
        // Bitmap is immutable, so we need to create a new one based on the transformation
        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), transformationMatrix, true);
    }

    public static Uri saveBitmap(Bitmap bitmap) throws IllegalArgumentException {
        FileOutputStream out = null;
        String filename = getUniqueFilename();
        int compressionPercentage = Capturandro.DEFAULT_STORED_IMAGE_COMPRESSION_PERCENT;
        try {
            out = new FileOutputStream(filename);
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionPercentage, out);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
        return Uri.fromFile(new File(filename));
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
            IOUtils.closeQuietly(inJustDecodeBoundsImageStream);
            IOUtils.closeQuietly(inSampleSizeImageStream);
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


    public static String getUniqueFilename() {
        return new File(Capturandro.applicationContext.getExternalCacheDir().getAbsolutePath(), "capturandro-" + System.currentTimeMillis() + "." + random.nextInt() + ".jpg").toString();
    }

    private static int getOrientation(File file) {
        ExifInterface exif = getExifFromFile(file);

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        switch (orientation) {
            case 0:
                break;
            case 3:
                orientation = 180;
                break;
            case 6:
                orientation = 90;
                break;
            case 8:
                orientation = 270;
                break;
            default:
                orientation = 0;
                break;
        }
        return orientation;
    }

    private static ExifInterface getExifFromFile(File file) {
        try {
            return new ExifInterface(file.getPath());
        } catch (IOException e) {
            Log.i("Capturandro", "Could not read Exif data from file: " + file.getPath());
        }
        return null;
    }

}