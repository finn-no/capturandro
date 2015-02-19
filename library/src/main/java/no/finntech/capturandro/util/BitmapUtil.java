package no.finntech.capturandro.util;

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

public class BitmapUtil {

    public static int STORED_IMAGE_WIDTH = 1280;
    public static int STORED_IMAGE_HEIGHT = 720;

    private BitmapUtil() {
    }

    public static Bitmap getProcessedBitmap(File inFile) {
        // Store Exif information as it is not kept when image is copied
        ExifInterface exifInterface = BitmapUtil.getExifFromFile(inFile);

        if (exifInterface != null) {
            return BitmapUtil.resizeAndRotateBitmapFromFile(inFile, exifInterface, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
        } else {
            return BitmapUtil.decodeBitmap(inFile, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
        }
    }

    // Courtesy of Fedor / Thomas Vervest
    // (http://stackoverflow.com/questions/477572/android-strange-out-of-memory-issue-while-loading-an-image-to-a-bitmap-object/823966#823966)
    public static Bitmap decodeBitmap(File file, int width, int height) throws IllegalArgumentException {
        FileInputStream inJustDecodeBoundsImageStream = null;
        FileInputStream inSampleSizeImageStream = null;
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            inJustDecodeBoundsImageStream = new FileInputStream(file);
            BitmapFactory.decodeStream(inJustDecodeBoundsImageStream, null, o);

            // Decode withCameraCallback inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();

            // Parameters are width and height, but these are equal in our case
            o2.inSampleSize = calculateInSampleSize(o, width, height);

            inSampleSizeImageStream = new FileInputStream(file);

            return BitmapFactory.decodeStream(inSampleSizeImageStream, null, o2);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(inJustDecodeBoundsImageStream);
            IOUtils.closeQuietly(inSampleSizeImageStream);
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }

            // Handle images withCameraCallback weird aspect ratios
            final float totalPixels = width * height;
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }

        return inSampleSize;
    }

    public static Bitmap resizeAndRotateBitmapFromFile(File inFile, ExifInterface exif, int reqWidth, int reqHeight) {

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

        if (orientation != 0) {
            Matrix transformationMatrix = new Matrix();
            transformationMatrix.postRotate(orientation);

            // Decode -scaled- bitmap before rotating it. Makes things more memory friendly.
            Bitmap sourceBitmap = BitmapUtil.decodeBitmap(inFile, reqWidth, reqHeight);

            // Bitmap is immutable, so we need to create a new one based on the transformation
            return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), transformationMatrix, true);
        } else {
            // Orientation for image is correct, so we just return it
            return decodeBitmap(inFile, reqWidth, reqHeight);
        }
    }

    public static ExifInterface getExifFromFile(File file) {
        try {
            return new ExifInterface(file.getPath());
        } catch (IOException e) {
            Log.i("Capturandro", "Could not read Exif data from file: " + file.getPath());
        }
        return null;
    }

}