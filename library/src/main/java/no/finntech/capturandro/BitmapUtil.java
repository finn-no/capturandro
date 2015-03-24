package no.finntech.capturandro;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

class BitmapUtil {

    private BitmapUtil() {
    }

    static Bitmap getProcessedBitmap(File inFile, int longestSide) {
        // Decode -scaled- bitmap before rotating it. Makes things more memory friendly.
        Bitmap bitmap = decodeBitmapFile(inFile, longestSide);
        return resizeAndRotateBitmap(bitmap, longestSide, getOrientation(inFile));
    }

    static Bitmap getProcessedBitmap(InputStream inputStream, int longestSide) {
        // We can't decode from a stream twice, meaning we can't decode just the metadata
        // then do the downsampling, so we might as well just decode directly, then resize.
        // This uses more memory, so it'd be nice if we found a better way.
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        return resizeAndRotateBitmap(bitmap, longestSide, 0);
    }

    private static Bitmap resizeAndRotateBitmap(Bitmap sourceBitmap, int specifiedLongestSide, int orientation) {
        Matrix transformationMatrix = new Matrix();

        if (specifiedLongestSide > 0) {
            int longestSide = Math.max(sourceBitmap.getWidth(), sourceBitmap.getHeight());
            int shortestSide = Math.min(sourceBitmap.getWidth(), sourceBitmap.getHeight());
            float ratio = (float) longestSide / shortestSide;
            longestSide = specifiedLongestSide;
            shortestSide = (int) (longestSide / ratio);

            int newWidth = sourceBitmap.getWidth();
            int newHeight = sourceBitmap.getHeight();

            if (isPortrait(newWidth, newHeight)) {
                newHeight = longestSide;
                newWidth = shortestSide;
            } else {
                newHeight = shortestSide;
                newWidth = longestSide;
            }

            float scaleWidth = ((float) newWidth) / sourceBitmap.getWidth();
            float scaleHeight = ((float) newHeight) / sourceBitmap.getHeight();

            transformationMatrix.preScale(scaleWidth, scaleHeight);
        }

        transformationMatrix.postRotate(orientation);
        // Bitmap is immutable, so we need to create a new one based on the transformation
        Bitmap dstBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), transformationMatrix, true);

        sourceBitmap.recycle();
        return dstBitmap;
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

    private static boolean isPortrait(int width, int height) {
        return height > width;
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