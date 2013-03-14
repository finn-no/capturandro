package no.finn.capturandro.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import no.finn.capturandro.Config;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;

public class BitmapUtil {

    private BitmapUtil(){}

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

            // Decode withEventHandler inSampleSize
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


    public static void resizeAndSaveBitmapFile(File fileToResizeAndSave, int reqWidth, int reqHeight) throws IllegalArgumentException {
        Bitmap bitmap = decodeBitmap(fileToResizeAndSave, reqWidth, reqHeight);
        saveBitmap(bitmap, fileToResizeAndSave);
    }

    public static void resizeAndSaveBitmapFile(File fileToResizeAndSave, int reqWidth, int reqHeight, int compressionPercentage) throws IllegalArgumentException {
        Bitmap bitmap = decodeBitmap(fileToResizeAndSave, reqWidth, reqHeight);
        saveBitmap(bitmap, fileToResizeAndSave, compressionPercentage);
    }

    public static void resizeAndSaveBitmapFile(File inFile, File outFile, int reqWidth, int reqHeight) throws IllegalArgumentException {
        Bitmap bitmap = decodeBitmap(inFile, reqWidth, reqHeight);
        saveBitmap(bitmap, outFile);
    }


    public static void saveBitmap(Bitmap bitmap, File filenameToSave) throws IllegalArgumentException {
        saveBitmap(bitmap, filenameToSave, Config.DEFAULT_STORED_IMAGE_COMPRESSION_PERCENT);
    }

    public static void saveBitmap(Bitmap bitmap, File filenameToSave, int compressionPercentage) throws IllegalArgumentException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filenameToSave);
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressionPercentage, out);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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

            // Handle images withEventHandler weird aspect ratios
            final float totalPixels = width * height;
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }

        return inSampleSize;
    }

    public static void resizeAndRotateAndSaveBitmapFile(File inFile, File outFile, ExifInterface exif, int reqWidth, int reqHeight) {

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

        switch (orientation){
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

        if (orientation != 0){
            Matrix transformationMatrix = new Matrix();
            transformationMatrix.postRotate(orientation);

            // Decode -scaled- bitmap before rotating it. Makes things more memory friendly.
            Bitmap sourceBitmap = BitmapUtil.decodeBitmap(inFile, reqWidth, reqHeight);

            // Bitmap is immutable, so we need to create a new one based on the transformation
            Bitmap rotatedBitmap =
                    Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), transformationMatrix, true);
            BitmapUtil.saveBitmap(rotatedBitmap, outFile);
        } else {
            // Orientation for image is correct, so we save it
            resizeAndSaveBitmapFile(inFile, outFile, reqWidth, reqHeight);
        }
    }

    public static ExifInterface getExifFromFile(File file) {
        ExifInterface exif;

        try {
            return new ExifInterface(file.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO: Handle better
        }
    }

    public static Bitmap fetchBitmap(Uri uri, File fileToSave, int compressionPercentage) throws IOException {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            inputStream = new URL(uri.toString()).openStream();
            outputStream = new FileOutputStream(fileToSave);
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        BitmapUtil.resizeAndSaveBitmapFile(fileToSave, Config.DEFAULT_STORED_IMAGE_WIDTH, Config.DEFAULT_STORED_IMAGE_HEIGHT, compressionPercentage);

        return BitmapUtil.decodeBitmap(fileToSave, 400, 400);
    }

}