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

import rx.Observable;
import rx.Subscriber;

/*
* This is the main class for the capturandro library.
*/
public class Capturandro {

    public static final int DEFAULT_STORED_IMAGE_COMPRESSION_PERCENT = 75;

    private OnActivityResultCallback currentCallback = null;

    private interface OnActivityResultCallback {
        void OnActivityResult(int requestCode, int resultCode, Intent intent);
    }

    public Capturandro() {
        super();
    }

    public Observable<Observable<Uri>> importImageFromCamera(final Activity activity, final int resultCode) {
        return importImageFromCamera(activity, resultCode, -1);
    }

    /**
     * Returns a observable that will return a single Obserable<Uri> in onNext once the image is captured.
     *
     * When executing subscribe on the Obserable<Uri> image processing may take place (on a seperate thread).
     */
    public Observable<Observable<Uri>> importImageFromCamera(final Activity activity, final int resultCode, final int longestSide) {
        return Observable.create(new Observable.OnSubscribe<Observable<Uri>>() {
            @Override
            public void call(final Subscriber<? super Observable<Uri>> subscriber) {
                subscriber.onStart();

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                final String cameraFilename = BitmapUtil.getUniqueFilename(activity);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(cameraFilename)));
                currentCallback = new OnActivityResultCallback() {
                    @Override
                    public void OnActivityResult(int requestCode, int resultCode, Intent intent) {
                        if (resultCode == Activity.RESULT_OK) {
                            if (!subscriber.isUnsubscribed()) {
                                ImportHandler importHandler = new ImportHandler(activity, longestSide);
                                subscriber.onNext(importHandler.camera(cameraFilename));
                                subscriber.onCompleted();
                            }
                        }
                    }
                };
                activity.startActivityForResult(intent, resultCode);
            }
        });
    }

    public Observable<Observable<Uri>> importImageFromGallery(Activity activity, int resultCode) {
        return importImageFromGallery(activity, resultCode, -1);
    }

    public Observable<Observable<Uri>> importImageFromGallery(Activity activity, int resultCode, int longestSide) {
        return importImageFromGallery(activity, resultCode, longestSide, true);
    }

    /**
     * Returns a observable that will return a one or more Obserable<Uri> in onNext once the images are selected.
     *
     * When executing subscribe on the Obserable<Uri> image processing may take place (on a seperate thread).
     */
    public Observable<Observable<Uri>> importImageFromGallery(final Activity activity, final int resultCode, final int longestSide, final boolean multiselect) {
        return Observable.create(new Observable.OnSubscribe<Observable<Uri>>() {
            @Override
            public void call(final Subscriber<? super Observable<Uri>> subscriber) {
                subscriber.onStart();

                currentCallback = new OnActivityResultCallback() {
                    @Override
                    public void OnActivityResult(int requestCode, int resultCode, Intent intent) {
                        if (resultCode == Activity.RESULT_OK) {
                            if (!subscriber.isUnsubscribed()) {
                                ImportHandler importHandler = new ImportHandler(activity, longestSide);
                                if (intent != null) {
                                    if (Build.VERSION.SDK_INT >= 18) {
                                        ClipData clipData = intent.getClipData();
                                        if (clipData != null && clipData.getItemCount() > 0) {
                                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                                Uri uri = clipData.getItemAt(i).getUri();
                                                subscriber.onNext(importHandler.gallery(uri));
                                            }
                                            return;
                                        }
                                    }
                                    Uri selectedImage = intent.getData();
                                    subscriber.onNext(importHandler.gallery(selectedImage));
                                    subscriber.onCompleted();
                                } else {
                                    subscriber.onError(new CapturandroException("intent is null"));
                                }
                            }
                        }
                    }
                };

                // it probably would have been better if this tried both these methods and presented them
                // both in a chooser instead of falling back when the main one isn't found. With this approach,
                // if you have, say, google's photos (g+) and the cyanogenmod gallery installed, the latter
                // will never be queried.
                Intent intent;
                try {
                    intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    if (Build.VERSION.SDK_INT >= 18 && multiselect) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    activity.startActivityForResult(intent, resultCode);
                } catch (ActivityNotFoundException e) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    if (Build.VERSION.SDK_INT >= 18 && multiselect) {
                        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }
                    activity.startActivityForResult(intent, resultCode);
                }

            }
        });

    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) throws CapturandroException {
        if (currentCallback != null) {
            currentCallback.OnActivityResult(requestCode, resultCode, intent);
            currentCallback = null;
        }
    }

    public void clearAllCachedBitmaps(Context context) {
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