package no.finntech.capturandro;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;

/*
* This is the main class for the capturandro library.
*/
public class Capturandro {

    public static final int DEFAULT_STORED_IMAGE_COMPRESSION_PERCENT = 75;

    private int galleryIntentResultCode;
    private int cameraIntentResultCode;
    private final Context context;
    private CapturandroCallback capturandroCallback;
    private String cameraFilename;
    private int longestSide;

    /*
    * Builder class for specifying options to capturandro.
    *
    * @param capturandroCallback    Implementation of CapturandroCallback, whose methods are called
    *                               when an image is captured or capturing fails.
    *
    * @param filenamePrefix         Prefix for temporary files stored in the Activity's external
    *                               cache dir. This file is usually removed by capturandro after
    *                               import.
    *
    * @param context                Android context
    */
    public static class Builder {
        private CapturandroCallback capturandroCallback;
        private String filenamePrefix;
        private Context context;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder withCallback(CapturandroCallback capturandroCallback) {
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

    Capturandro(Builder builder) {
        super();
        this.context = builder.context;
        this.capturandroCallback = builder.capturandroCallback;
    }

    public void setCapturandroCallback(CapturandroCallback capturandroCallback) {
        this.capturandroCallback = capturandroCallback;
    }


    /*
    * Start the import process for getting an image from the camera.
    *
    * @param activity   Android Activity. Needed for sending Intent to get pictures from camera.
    *
    * @param resultCode The integer code which will be returned in onActivityResult. Handy
    *                   for knowing where in your app the particular import request comes from.
    */
    public void importImageFromCamera(Activity activity, int resultCode) {
        importImageFromCamera(activity, resultCode, -1);
    }

    /*
    * Start the import process for getting an image from the camera.
    *
    * @param activity   Android Activity. Needed for sending Intent to get pictures from camera.
    *
    * @param resultCode The integer code which will be returned in onActivityResult. Handy
    *                   for knowing where in your app the particular import request comes from.
    *
    * @param longestSide    The longest side of the imported image in either direction
    */
    public void importImageFromCamera(Activity activity, int resultCode, int longestSide) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        this.cameraFilename = BitmapUtil.getUniqueFilename(context);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(cameraFilename)));
        this.longestSide = longestSide;
        this.cameraIntentResultCode = resultCode;
        activity.startActivityForResult(intent, resultCode);
    }

    /*
    *
    */
    public void importImageFromGallery(Activity activity, int resultCode) {
        importImageFromGallery(activity, resultCode, -1);
    }

    /*
    *
    */
    public void importImageFromGallery(Activity activity, int resultCode, int longestSide) {
        importImageFromGallery(activity, resultCode, longestSide, true);
    }

    public void importImageFromGallery(Activity activity, int resultCode, int longestSide, boolean multiselect) {
        // it probably would have been better if this tried both these methods and presented them
        // both in a chooser instead of falling back when the main one isn't found. With this approach,
        // if you have, say, google's photos (g+) and the cyanogenmod gallery installed, the latter
        // will never be queried.
        this.galleryIntentResultCode = resultCode;
        this.longestSide = longestSide;
        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (Build.VERSION.SDK_INT >= 18 && multiselect) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            activity.startActivityForResult(intent, galleryIntentResultCode);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            if (Build.VERSION.SDK_INT >= 18 && multiselect) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            activity.startActivityForResult(intent, galleryIntentResultCode);
        }
    }

    /*
    * onActivityResult needs to be called by the same Activity which is passed to either of the
    * import methods. It calls on the methods in the CapturandroCallback implementation sent to
    * the Capturandro constructor.
    *
    * @throws CapturandroException
    */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) throws CapturandroException {
        if (capturandroCallback == null) {
            throw new IllegalStateException("Unable to import image. Have you implemented CapturandroCallback?");
        }
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == cameraIntentResultCode) {
                importFromCamera();
            } else if (requestCode == galleryIntentResultCode) {
                importFromGallery(intent);
            }
        }
    }

    private void importFromCamera() {
        ImportHandler importHandler = new ImportHandler(context, longestSide);
        capturandroCallback.cameraImport(importHandler.camera(cameraFilename));
    }

    private void importFromGallery(Intent intent) {
        ImportHandler importHandler = new ImportHandler(context, longestSide);
        if (intent != null) {
            if (Build.VERSION.SDK_INT >= 18) {
                ClipData clipData = intent.getClipData();
                if (clipData != null && clipData.getItemCount() > 0) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        Uri uri = clipData.getItemAt(i).getUri();
                        capturandroCallback.galleryImport(importHandler.gallery(uri));
                    }
                    return;
                }
            }
            Uri selectedImage = intent.getData();
            capturandroCallback.galleryImport(importHandler.gallery(selectedImage));
        }
    }

    public void clearAllCachedBitmaps() {
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null) {
            final String cachePath = externalCacheDir.getAbsolutePath();
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                    File[] files = new File(cachePath).listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            return filename.startsWith("capturando-") && filename.endsWith(".jpg");
                        }
                    });
                    for (File file : files) {
                        try {
                            file.delete();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }
            }.execute();
        }
    }
}