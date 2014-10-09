package no.finntech.capturandro;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import no.finntech.capturandro.asynctask.DownloadPicasaImageAsyncTask;
import no.finntech.capturandro.callbacks.CapturandroCallback;
import no.finntech.capturandro.util.BitmapUtil;

import static no.finntech.capturandro.Config.STORED_IMAGE_HEIGHT;
import static no.finntech.capturandro.Config.STORED_IMAGE_WIDTH;

public class Capturandro {

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

    private Activity activity;

    private String filename;
    private String filenamePrefix;
    private File storageDirectoryPath;
    private CapturandroCallback capturandroCallback;
    private final int galleryIntentResultCode;
    private final int cameraIntentResultCode;

    public static class Builder {
        private CapturandroCallback capturandroCallback;
        private String filenamePrefix;
        private File storageDirectoryPath;
        private Activity activity;
        private int galleryIntentResultCode;
        private int cameraIntentResultCode;

        public Builder(Activity activity) {
            this.activity = activity;
        }

        public Builder withCameraCallback(CapturandroCallback capturandroCallback) {
            this.capturandroCallback = capturandroCallback;
            return this;
        }

        public Builder withFilenamePrefix(String filenamePrefix) {
            this.filenamePrefix = filenamePrefix + "_";
            return this;
        }

        public Builder withStorageDirectoryPath(File storageDirectoryPath) {
            this.storageDirectoryPath = storageDirectoryPath;
            return this;
        }

        public Builder withGalleryIntentResultCode(int galleryIntentResultCode) {
            this.galleryIntentResultCode = galleryIntentResultCode;
            return this;
        }

        public Builder withCameraIntentResultCode(int cameraIntentResultCode) {
            this.cameraIntentResultCode = cameraIntentResultCode;
            return this;
        }

        public Capturandro build() {
            return new Capturandro(this);
        }
    }

    public Capturandro(Builder builder) {
        this.activity = builder.activity;
        this.capturandroCallback = builder.capturandroCallback;
        this.filenamePrefix = builder.filenamePrefix;
        this.storageDirectoryPath = builder.storageDirectoryPath;
        this.galleryIntentResultCode = builder.galleryIntentResultCode;
        this.cameraIntentResultCode = builder.cameraIntentResultCode;
    }

    public void importImageFromCamera() {
        importImageFromCamera(getUniqueFilename());
    }

    public void importImageFromCamera(String filename) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getStorageDirectoryPath(), filename)));
        this.filename = filename;

        activity.startActivityForResult(intent, cameraIntentResultCode);
    }

    public void importImageFromGallery() {
        importImageFromGallery(getUniqueFilename());
    }

    public void importImageFromGallery(String filename) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        this.filename = filename;
        activity.startActivityForResult(intent, galleryIntentResultCode);
    }

    public void onActivityResult(int reqCode, int resultCode, Intent intent) throws IllegalArgumentException {
        if (capturandroCallback == null) {
            throw new IllegalStateException("Unable to import image. Have you implemented CapturandroCallback?");
        }

        if (reqCode == cameraIntentResultCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (filename != null) {
                    File fileToStore = new File(getStorageDirectoryPath(), filename);
                    try {
                        fileToStore.createNewFile();
                    } catch (IOException e) {
                        capturandroCallback.onCameraImportFailure(e);
                    }
                    saveBitmap(filename, fileToStore, fileToStore);
                } else {
                    capturandroCallback.onCameraImportFailure(new IllegalArgumentException("Image could not be added"));
                }
            }
        } else if (reqCode == galleryIntentResultCode) {
            Uri selectedImage = null;

            if (intent != null) {
                selectedImage = intent.getData();
            }

            if (isUserAttemptingToAddVideo(selectedImage)) {
                capturandroCallback.onCameraImportFailure(new IllegalArgumentException("Videos can't be added"));
//                    break;
            }

            if (selectedImage != null) {
                handleImageFromGallery(selectedImage, filename);
            }
        }
    }

    private void saveBitmap(String imageFilename, File inFile, File outFile) {
        try {
            // Store Exif information as it is not kept when image is copied
            ExifInterface exifInterface = BitmapUtil.getExifFromFile(inFile);

            if (exifInterface != null) {
                BitmapUtil.resizeAndRotateAndSaveBitmapFile(inFile, outFile, exifInterface, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
            } else {
                BitmapUtil.resizeAndSaveBitmapFile(outFile, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
            }

            capturandroCallback.onCameraImportSuccess(imageFilename);

        } catch (IllegalArgumentException e) {
            capturandroCallback.onCameraImportFailure(e);
        }
    }

    private void handleImageFromGallery(Uri selectedImage, String filename) {
        Cursor cursor = activity.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

            if (isPicasaAndroid3Image(selectedImage)) {
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
        for (int i = 0; i < PICASA_CONTENT_PROVIDERS.length; i++) {
            if (selectedImage.toString().startsWith(PICASA_CONTENT_PROVIDERS[i])) {
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
        if (capturandroCallback == null) {
            throw new IllegalStateException("Unable to import image. Have you implemented CapturandroCallback?");
        }

        new DownloadPicasaImageAsyncTask(activity, selectedImage, filename, capturandroCallback).execute();
    }


    private void fetchLocalGalleryImageFile(String filename, Cursor cursor, int columnIndex) {
        // Resize and save so that the image is still kept if the user deletes the original image from Gallery
        File inFile = new File(cursor.getString(columnIndex));
        File outFile = new File(activity.getExternalCacheDir(), filename);

        saveBitmap(filename, inFile, outFile);
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }

    public void handleImageIfSentFromGallery(Intent intent) {
        if (isReceivingImage(intent)) {
            handleSendImages(getImagesFromIntent(intent));
        }
    }

    public ArrayList<Uri> getImagesFromIntent(Intent intent) {
        ArrayList<Uri> imageUris = new ArrayList<Uri>();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                imageUris.add((Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM));
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            }
        }

        return imageUris;
    }

    private void handleSendImages(ArrayList<Uri> imagesFromIntent) {
        for (Uri imageUri : imagesFromIntent) {
            handleImageFromGallery(imageUri, getUniqueFilename());
        }
    }

    private boolean isReceivingImage(Intent intent) {
        String action = intent.getAction();
        String mimeType = intent.getType();
        return (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
                && (mimeType != null && mimeType.startsWith("image"));
    }

    public void setCapturandroCallback(CapturandroCallback capturandroCallback) {
        this.capturandroCallback = capturandroCallback;
    }

    private String getUniqueFilename() {
        return filenamePrefix + System.currentTimeMillis() + ".jpg";
    }

    public File getStorageDirectoryPath() {
        if (storageDirectoryPath == null || !storageDirectoryPath.equals("")) {
            return activity.getExternalCacheDir();
        } else {
            return storageDirectoryPath;
        }
    }

}