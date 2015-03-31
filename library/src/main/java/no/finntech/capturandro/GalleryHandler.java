package no.finntech.capturandro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

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
            imageHandler.onCameraImportFailure(new CapturandroException("Video files are not supported"));
            return;
        }

        if (selectedImage != null) {
            if (selectedImage.getScheme().equals("file")) {
                Bitmap bitmap = fetchOldStyleGalleryImageFile(selectedImage);
                imageHandler.onImportSuccess(bitmap);
            }

            Cursor cursor = context.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

                if (isPicasaAndroid3Image(selectedImage) || imageIsRemote(cursor)) {
                    fetchPicasaAndroid3Image(selectedImage, filename, cursor);
                } else if ("content".equals(selectedImage.getScheme())) {
                    Bitmap bitmap = fetchOldStyleGalleryImageFile(selectedImage);
                    imageHandler.onImportSuccess(bitmap);
                } else {
                    Bitmap bitmap = fetchLocalGalleryImageFile(cursor, columnIndex);
                    imageHandler.onImportSuccess(bitmap);
                }
                cursor.close();
            }
        }
    }

    private Bitmap fetchOldStyleGalleryImageFile(Uri selectedImage) {
        InputStream stream;
        try {
            stream = context.getContentResolver().openInputStream(selectedImage);
            Bitmap bitmap = BitmapUtil.getProcessedBitmap(stream, longestSide);
            stream.close();
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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

    private Bitmap fetchLocalGalleryImageFile(Cursor cursor, int columnIndex) {
        // Resize and save so that the image is still kept if the user deletes the original image from Gallery
        File inFile = new File(cursor.getString(columnIndex));
        Bitmap bitmap = BitmapUtil.getProcessedBitmap(inFile, longestSide);
        inFile.delete();
        return bitmap;
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }

}
