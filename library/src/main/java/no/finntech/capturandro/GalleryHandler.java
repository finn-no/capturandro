package no.finntech.capturandro;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class GalleryHandler {

    private final static String[] PICASA_CONTENT_PROVIDERS = {
            "content://com.android.gallery3d.provider",
            "content://com.google.android.gallery3d",
            "content://com.android.sec.gallery3d",
            "content://com.sec.android.gallery3d",
            "content://com.google.android.apps.photos"
    };

    private final static String[] FILE_PATH_COLUMNS = {
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME
    };

    private CapturandroCallback.ImageHandler imageHandler;
    private Context context;
    private int longestSide;

    GalleryHandler() {
    }

    void handle(Uri selectedImage, String filename, CapturandroCallback.ImageHandler imageHandler, Context context, int longestSide) throws CapturandroException {
        this.imageHandler = imageHandler;
        this.context = context;
        this.longestSide = longestSide;

        if (isUserAttemptingToAddVideo(selectedImage)) {
            imageHandler.onCameraImportFailure(filename, new CapturandroException("Video files are not supported"));
            return;
        }

        if (selectedImage != null) {
            int orientation = getOrientation(context.getContentResolver(), selectedImage);

            if (selectedImage.getScheme().equals("file")) {
                Bitmap bitmap = fetchOldStyleGalleryImageFile(selectedImage, orientation);
                imageHandler.onImportSuccess(filename, bitmap);
            }

            Cursor cursor = context.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

                if (isPicasaAndroid3Image(selectedImage) || imageIsRemote(cursor)) {
                    fetchPicasaAndroid3Image(selectedImage, filename, cursor);
                } else if ("content".equals(selectedImage.getScheme())) {
                    Bitmap bitmap = fetchOldStyleGalleryImageFile(selectedImage, orientation);
                    imageHandler.onImportSuccess(filename, bitmap);
                } else {
                    Bitmap bitmap = fetchLocalGalleryImageFile(cursor, columnIndex, orientation);
                    imageHandler.onImportSuccess(filename, bitmap);
                }
                cursor.close();
            }
        }
    }

    private Bitmap fetchOldStyleGalleryImageFile(Uri selectedImage, int orientation) {
        InputStream stream;
        try {
            stream = context.getContentResolver().openInputStream(selectedImage);
            Bitmap bitmap = BitmapUtil.getProcessedBitmap(stream, longestSide, orientation);
            stream.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getOrientation(ContentResolver contentResolver, Uri photoUri) {
        Cursor cursor = contentResolver.query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    private boolean isPicasaAndroid3Image(Uri selectedImage) {
        for (String picasaContentProvider : PICASA_CONTENT_PROVIDERS) {
            if (selectedImage.toString().startsWith(picasaContentProvider)) {
                return true;
            }
        }
        return false;
    }

    private boolean imageIsRemote(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (columnIndex != -1 && (cursor.getString(columnIndex).startsWith("http://"))) {
            return true;
        }
        return false;
    }

    private Bitmap fetchPicasaAndroid3Image(Uri selectedImage, String filename, Cursor cursor) {
        int columnIndex;
        columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (columnIndex != -1) {
            new DownloadRemoteImageAsyncTask(context, selectedImage, filename, imageHandler, longestSide).execute();
        }
        return null;
    }

    private Bitmap fetchLocalGalleryImageFile(Cursor cursor, int columnIndex, int orientation) {
        // Resize and save so that the image is still kept if the user deletes the original image from Gallery
        File inFile = new File(cursor.getString(columnIndex));
        Bitmap bitmap = BitmapUtil.getProcessedBitmap(inFile, longestSide, orientation);
        inFile.delete();
        return bitmap;
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }

}
