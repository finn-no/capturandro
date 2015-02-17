package no.finntech.capturandro;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import no.finntech.capturandro.asynctask.DownloadRemoteImageAsyncTask;
import no.finntech.capturandro.callbacks.CapturandroCallback;
import no.finntech.capturandro.util.BitmapUtil;

import static no.finntech.capturandro.Config.STORED_IMAGE_HEIGHT;
import static no.finntech.capturandro.Config.STORED_IMAGE_WIDTH;

public class Capturandro {

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

    private final String filenamePrefix;
    private final File storageDirectoryPath;
    private final int galleryIntentResultCode;
    private final int cameraIntentResultCode;
    private final Context context;
    private CapturandroCallback capturandroCallback;
    private String filename;

    public static class Builder {
        private CapturandroCallback capturandroCallback;
        private String filenamePrefix;
        private File storageDirectoryPath;
        private Context context;
        private int galleryIntentResultCode;
        private int cameraIntentResultCode;

        public Builder(Context context) {
            this.context = context;
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
        this.context = builder.context;
        this.capturandroCallback = builder.capturandroCallback;
        this.filenamePrefix = builder.filenamePrefix;
        this.storageDirectoryPath = builder.storageDirectoryPath;
        this.galleryIntentResultCode = builder.galleryIntentResultCode;
        this.cameraIntentResultCode = builder.cameraIntentResultCode;
    }

    public void importImageFromCamera(Activity activity) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        this.filename = getUniqueFilename();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(getStorageDirectoryPath(), filename)));
        activity.startActivityForResult(intent, cameraIntentResultCode);
    }

    public void importImageFromGallery(Activity activity) {
        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activity.startActivityForResult(intent, galleryIntentResultCode);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivityForResult(intent, galleryIntentResultCode);
        }
    }

    public void onActivityResult(int reqCode, int resultCode, Intent intent) throws IllegalArgumentException {
        if (capturandroCallback == null) {
            throw new IllegalStateException("Unable to import image. Have you implemented CapturandroCallback?");
        }
        if (filename == null) {
            filename = getUniqueFilename();
        }

        if (reqCode == cameraIntentResultCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (filename != null) {
                    File fileToStore = new File(getStorageDirectoryPath(), filename);
                    capturandroCallback.onImportSuccess(resizeAndRotateFile(fileToStore));
                } else {
                    capturandroCallback.onCameraImportFailure(new RuntimeException("Could not get image from camera"));
                }
            }
        } else if (reqCode == galleryIntentResultCode) {
            if (resultCode == Activity.RESULT_OK && intent != null) { //sometimes intent is null when gallery app is opened
                Uri selectedImage = intent.getData();

                if (isUserAttemptingToAddVideo(selectedImage)) {
                    capturandroCallback.onCameraImportFailure(new IllegalArgumentException("Videos can't be added"));
                    return;
                }

                if (selectedImage != null) {
                    final Bitmap bitmap = handleImageFromGallery(selectedImage, filename);
                    capturandroCallback.onImportSuccess(bitmap);
                }
            }
        }
    }

    private Bitmap resizeAndRotateFile(File inFile) {
        // Store Exif information as it is not kept when image is copied
        ExifInterface exifInterface = BitmapUtil.getExifFromFile(inFile);

        if (exifInterface != null) {
            return BitmapUtil.resizeAndRotateBitmapFromFile(inFile, exifInterface, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
        } else {
            return BitmapUtil.decodeBitmap(inFile, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
        }
    }

    private Bitmap handleImageFromGallery(Uri selectedImage, String filename) {
        if (selectedImage.getScheme().equals("file")) {
            return fetchOldStyleGalleryImageFile(selectedImage);
        }

        Cursor cursor = context.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

            if (isPicasaAndroid3Image(selectedImage) || imageIsRemote(cursor)) {
                fetchPicasaAndroid3Image(selectedImage, filename, cursor);
            } else if ("content".equals(selectedImage.getScheme())) {
                return fetchOldStyleGalleryImageFile(selectedImage);
            } else {
                return fetchLocalGalleryImageFile(cursor, columnIndex);
            }
            cursor.close();
        }
        return null;
    }

    private Bitmap fetchOldStyleGalleryImageFile(Uri selectedImage) {
        InputStream stream = null;
        try {
            stream = context.getContentResolver().openInputStream(selectedImage);
            Bitmap bitmap = BitmapFactory.decodeStream(stream);
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
            fetchPicasaImage(selectedImage, filename);
        }
        return null;
    }

    private void fetchPicasaImage(Uri selectedImage, String filename) {
        new DownloadRemoteImageAsyncTask(context, selectedImage, filename, capturandroCallback).execute();
    }


    private Bitmap fetchLocalGalleryImageFile(Cursor cursor, int columnIndex) {
        // Resize and save so that the image is still kept if the user deletes the original image from Gallery
        File inFile = new File(cursor.getString(columnIndex));
        return resizeAndRotateFile(inFile);
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
        if (filenamePrefix != null) {
            return filenamePrefix + System.currentTimeMillis() + ".jpg";
        } else {
            return "capturandro-" + System.currentTimeMillis() + ".jpg";
        }
    }

    public File getStorageDirectoryPath() {
        if (storageDirectoryPath == null || !storageDirectoryPath.getAbsolutePath().equals("")) {
            return context.getExternalCacheDir();
        } else {
            return storageDirectoryPath;
        }
    }

}