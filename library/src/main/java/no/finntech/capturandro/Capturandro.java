package no.finntech.capturandro;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;

import no.finntech.capturandro.callbacks.CapturandroCallback;
import no.finntech.capturandro.exception.CapturandroException;
import no.finntech.capturandro.util.BitmapUtil;
import no.finntech.capturandro.util.GalleryHandler;

public class Capturandro {

    private final GalleryHandler galleryHandler = new GalleryHandler();
    private final String filenamePrefix;
    private int galleryIntentResultCode;
    private int cameraIntentResultCode;
    private final Context context;
    private CapturandroCallback capturandroCallback;
    private String filename;

    public static class Builder {
        private CapturandroCallback capturandroCallback;
        private String filenamePrefix;
        private Context context;

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

        public Capturandro build() {
            return new Capturandro(this);
        }
    }

    public Capturandro(Builder builder) {
        this.context = builder.context;
        this.capturandroCallback = builder.capturandroCallback;
        this.filenamePrefix = builder.filenamePrefix;
    }

    public void setCapturandroCallback(CapturandroCallback capturandroCallback) {
        this.capturandroCallback = capturandroCallback;
    }

    public void importImageFromCamera(Activity activity, int resultCode) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        this.filename = getUniqueFilename();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(context.getExternalCacheDir(), filename)));
        this.cameraIntentResultCode = resultCode;
        activity.startActivityForResult(intent, resultCode);
    }

    public void importImageFromGallery(Activity activity, int resultCode) {
        // it probably would have been better if this tried both these methods and presented them
        // both in a chooser instead of falling back when the main one isn't found. With this approach,
        // if you have, say, google's photos (g+) and the cyanogenmod gallery installed, the latter
        // will never be queried.
        this.galleryIntentResultCode = resultCode;
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

    public void onActivityResult(int reqCode, int resultCode, Intent intent) throws CapturandroException {
        if (capturandroCallback == null) {
            throw new IllegalStateException("Unable to import image. Have you implemented CapturandroCallback?");
        }

        if (reqCode == cameraIntentResultCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (filename != null) {
                    File file = new File(context.getExternalCacheDir(), filename);
                    final Bitmap bitmap = BitmapUtil.getProcessedBitmap(file);
                    file.delete();
                    capturandroCallback.onImportSuccess(bitmap, reqCode);
                } else {
                    capturandroCallback.onCameraImportFailure(new CapturandroException("Could not get image from camera"));
                }
            }
        } else if (reqCode == galleryIntentResultCode) {
            if (resultCode == Activity.RESULT_OK && intent != null) { //sometimes intent is null when gallery app is opened
                Uri selectedImage = intent.getData();
                if (filename == null) {
                    filename = getUniqueFilename();
                }
                galleryHandler.handle(selectedImage, filename, capturandroCallback, context, reqCode);
            }
        }
    }

    private String getUniqueFilename() {
        if (filenamePrefix != null) {
            return filenamePrefix + System.currentTimeMillis() + ".jpg";
        } else {
            return "capturandro-" + System.currentTimeMillis() + ".jpg";
        }
    }

    public void handleSharedImageIntent(Intent intent) throws CapturandroException {
        if (isReceivingImage(intent)) {
            handleSendImages(getImagesFromIntent(intent));
        }
    }

    private boolean isReceivingImage(Intent intent) {
        String action = intent.getAction();
        String mimeType = intent.getType();
        return (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
                && (mimeType != null && mimeType.startsWith("image"));
    }

    private void handleSendImages(ArrayList<Uri> imagesFromIntent) throws CapturandroException {
        for (Uri imageUri : imagesFromIntent) {
            galleryHandler.handle(imageUri, getUniqueFilename(), capturandroCallback, context, galleryIntentResultCode);
        }
    }

    private ArrayList<Uri> getImagesFromIntent(Intent intent) {
        ArrayList<Uri> imageUris = new ArrayList<>();
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

}