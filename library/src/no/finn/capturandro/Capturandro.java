package no.finn.capturandro;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import no.finn.capturandro.callbacks.CameraCallback;
import no.finn.capturandro.callbacks.PicasaCallback;
import no.finn.capturandro.asynctask.DownloadFileAsyncTask;
import no.finn.capturandro.exception.CapturandroException;
import no.finn.capturandro.util.BitmapUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static no.finn.capturandro.Config.STORED_IMAGE_HEIGHT;
import static no.finn.capturandro.Config.STORED_IMAGE_WIDTH;

public class Capturandro {

    private final static int IMAGE_FROM_CAMERA_RESULT = 1;
    private final static int IMAGE_FROM_GALLERY_RESULT = 2;

    private final static String[] PICASA_CONTENT_PROVIDERS = {
                "content://com.android.gallery3d.provider",
                "content://com.google.android.gallery3d",
                "content://com.android.sec.gallery3d",
                "content://com.sec.android.gallery3d"
    };

    private final static String[] FILE_PATH_COLUMNS = {
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME
    };

    // Added to make code more readable
    private static final String[] NO_SELECTION_ARGS = null;
    private static final String NO_SELECTION = null;
    private static final String NO_SORT_ORDER = null;

    private CameraCallback cameraCallback;
    private PicasaCallback picasaCallback;

    private String filename;
    private File storageDirectory;
    private Activity activity;


    public static class Builder {
        private CameraCallback cameraCallback;
        private PicasaCallback picasaCallback;
        private String filename;
        private File storageDirectory;
        private Activity activity;

        public Builder(Activity activity){
            this.activity = activity;
        }

        public Builder withCameraCallback(CameraCallback cameraCallback){
            this.cameraCallback = cameraCallback;

            return this;
        }

        public Builder withPicasaCallback(PicasaCallback picasaCallback){
            this.picasaCallback = picasaCallback;

            return this;
        }

        public Builder withFilename(String filename){
            this.filename = filename;

            return this;
        }

        public Builder withStorageDirectory(File path){
            this.storageDirectory = path;

            return this;
        }

        public Capturandro build(){
            return new Capturandro(this);
        }
    }

    public Capturandro(Builder builder) {
        this.activity = builder.activity;
        this.cameraCallback = builder.cameraCallback;
        this.picasaCallback = builder.picasaCallback;
        this.filename = builder.filename;
        this.storageDirectory = builder.storageDirectory;
    }

    public void importImageFromCamera(String filename) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getStorageDirectory(), filename)));
        this.filename = filename;

        activity.startActivityForResult(intent, IMAGE_FROM_CAMERA_RESULT);
    }

    public void importImageFromGallery(String filename) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        this.filename = filename;
        activity.startActivityForResult(intent, IMAGE_FROM_GALLERY_RESULT);
    }

    public void onActivityResult(int reqCode, int resultCode, Intent intent) throws IllegalArgumentException {
        if (cameraCallback == null){
            throw new IllegalStateException("Unable to import image. Have you implemented CameraCallback?");
        }

        switch (reqCode) {
            case IMAGE_FROM_CAMERA_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    if (filename != null){
                        File fileToStore = new File(getStorageDirectory(), filename);
                        try {
                            fileToStore.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            cameraCallback.onImportFailure(e);
                        }
                        saveBitmap(filename, fileToStore, fileToStore);
                        cameraCallback.onImportSuccess(filename);
                    } else {
                        // Throw exception saying image couldnt be added. Or something. showImageCouldNotBeAddedDialog();
                        cameraCallback.onImportFailure(new CapturandroException("Image could not be added"));
                    }
                }

                break;
            case IMAGE_FROM_GALLERY_RESULT:
                Uri selectedImage = null;

                if (intent != null){
                    selectedImage = intent.getData();
                }

                if (isUserAttemptingToAddVideo(selectedImage)){
                    cameraCallback.onImportFailure(new CapturandroException("Video can't be added"));
                    break;
                }

                if (selectedImage != null) {
                    handleImageFromGallery(selectedImage, filename);
                }

                break;
        }
    }

    private void saveBitmap(String imageFilename, File inFile, File outFile) {
        // Store Exif information as it is not kept when image is copied
        ExifInterface exifInterface = BitmapUtil.getExifFromFile(inFile);

        try {
            if (exifInterface != null){
                BitmapUtil.resizeAndRotateAndSaveBitmapFile(inFile, outFile, exifInterface, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
            } else {
                BitmapUtil.resizeAndSaveBitmapFile(outFile, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
            }

            cameraCallback.onImportSuccess(imageFilename);

        } catch (IllegalArgumentException e) {
            cameraCallback.onImportFailure(e);
        }
    }

    private void handleImageFromGallery(Uri selectedImage, String filename) {
        Cursor cursor = activity.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, NO_SELECTION, NO_SELECTION_ARGS, NO_SORT_ORDER);

        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

            if (isPicasaAndroid3Image(selectedImage)){
                fetchPicasaAndroid3Image(selectedImage, filename, cursor);
            } else {
                fetchLocalGalleryImageFile(filename, cursor, columnIndex);
            }

            cursor.close();
        } else if (isPicasaAndroid2Image(selectedImage)) {
            fetchPicasaImage(selectedImage, filename);
        }
    }




    private boolean isPicasaAndroid2Image(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().length() > 0;
    }

    private boolean isPicasaAndroid3Image(Uri selectedImage) {
        for (int i = 0; i < PICASA_CONTENT_PROVIDERS.length; i++){
            if (selectedImage.toString().startsWith(PICASA_CONTENT_PROVIDERS[i])){
                return true;
            }
        }

        return false;
    }

    private void fetchPicasaAndroid3Image(Uri selectedImage, String filename, Cursor cursor) {
        int columnIndex;
        columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (columnIndex != -1) {
            fetchPicasaImage(selectedImage, filename);
        }
    }

    private void fetchPicasaImage(Uri selectedImage, String filename) {
        if (picasaCallback == null){
            throw new IllegalStateException("Unable to import image. Have you implemented PicasaCallback?");
        }

        new DownloadFileAsyncTask(activity, selectedImage, filename, picasaCallback).execute();
    }


    private void fetchLocalGalleryImageFile(String filename, Cursor cursor, int columnIndex) {
        // Resize and save so that the image is still kept if the user deletes it from the Gallery
        File inFile = new File(cursor.getString(columnIndex));
        File outFile = new File(activity.getExternalCacheDir(), filename);

        saveBitmap(filename, inFile, outFile);
        cameraCallback.onImportSuccess(filename);
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }

    public ArrayList<Uri> getImagesFromIntent(Intent intent) {
        ArrayList<Uri> imageUris = new ArrayList<Uri>();
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                imageUris.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        }
        else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
        }
        return imageUris;
    }

    public void handleSendImages(Uri imageUris, String filename) {
        handleImageFromGallery(imageUris,  filename);
    }

    private boolean isReceivingImage(Intent intent) {
        String action = intent.getAction();
        String mimeType = intent.getType();
        return (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
                && (mimeType != null && mimeType.startsWith("image"));
    }

    public File getStorageDirectory() {
        if (storageDirectory == null){
            return activity.getExternalCacheDir();
        } else {
            return storageDirectory;
        }
    }

}