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
import java.util.UUID;

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
    private String filename;

    GalleryHandler() {
    }

    void handle(UUID importId, Uri selectedImage, String filename, CapturandroCallback.ImageHandler imageHandler, Context context, int longestSide) throws CapturandroException {
        this.imageHandler = imageHandler;
        this.context = context;
        this.longestSide = longestSide;
        this.filename = filename;

        if (isUserAttemptingToAddVideo(selectedImage)) {
            imageHandler.onGalleryImportFailure(importId, new CapturandroException("Video files are not supported"));
            return;
        }

        if (selectedImage != null) {
            int orientation = getOrientation(context.getContentResolver(), selectedImage);

            if (selectedImage.getScheme().equals("file")) {
                imageHandler.onGalleryImportStarted(importId);
                Uri uri = fetchOldStyleGalleryImageFile(selectedImage, orientation);
                imageHandler.onGalleryImportSuccess(importId, uri);
            }

            Cursor cursor = context.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

                if (isPicasaAndroid3Image(selectedImage) || imageIsRemote(cursor)) {
                    imageHandler.onGalleryImportStarted(importId);
                    fetchPicasaAndroid3Image(importId, selectedImage, filename, cursor);
                } else if ("content".equals(selectedImage.getScheme())) {
                    imageHandler.onGalleryImportStarted(importId);
                    Uri uri = fetchOldStyleGalleryImageFile(selectedImage, orientation);
                    imageHandler.onGalleryImportSuccess(importId, uri);
                } else {
                    Uri uri = fetchLocalGalleryImageFile(cursor, columnIndex, orientation);
                    imageHandler.onGalleryImportSuccess(importId, uri);
                }
                cursor.close();
            }
        }
    }

    private Uri fetchOldStyleGalleryImageFile(Uri selectedImage, int orientation) {
        InputStream stream;
        try {
            stream = context.getContentResolver().openInputStream(selectedImage);
            Uri uri = BitmapUtil.getProcessedImage(stream, longestSide, orientation, filename, context.getExternalCacheDir().getAbsolutePath());
            stream.close();
            return uri;
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

    private Bitmap fetchPicasaAndroid3Image(UUID importId, Uri selectedImage, String filename, Cursor cursor) {
        int columnIndex;
        columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (columnIndex != -1) {
            new DownloadRemoteImageAsyncTask(importId, context, selectedImage, filename, imageHandler, longestSide).execute();
        }
        return null;
    }

    private Uri fetchLocalGalleryImageFile(Cursor cursor, int columnIndex, int orientation) {
        // Resize and save so that the image is still kept if the user deletes the original image from Gallery
        File inFile = new File(cursor.getString(columnIndex));
        Uri uri = BitmapUtil.getProcessedImage(inFile, longestSide, orientation);
        inFile.delete();
        return uri;
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }

}
