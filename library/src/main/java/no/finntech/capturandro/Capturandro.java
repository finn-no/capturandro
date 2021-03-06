package no.finntech.capturandro;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.concurrent.Executors;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/*
* This is the main class for the capturandro library.
*/
public class Capturandro {
    private static final String KEY = "CAPTURANDO_STATE";
    protected static final String TEMPFILE_PREFIX = "capturandro-";
    public static final int DEFAULT_STORED_IMAGE_COMPRESSION_PERCENT = 75;

    private final CapturandoCallback callback;
    private static Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());
    private CapturandoState state = null;
    private static boolean initialStartup = true;
    private static String fileProviderAuthority;
    private Disposable deleteTask;

    public Capturandro(Context context, CapturandoCallback callback, String fileProviderAuthority) {
        super();
        this.callback = callback;
        Capturandro.fileProviderAuthority = fileProviderAuthority;
        if (initialStartup) {
            clearAllCachedBitmaps(context);
            initialStartup = false;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            state = savedInstanceState.getParcelable(KEY);
        }
    }

    public void onDestroy() {
        if (deleteTask != null && !deleteTask.isDisposed()) {
            deleteTask.dispose();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY, state);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void importImageFromCamera(final Activity activity, final int requestCode) {
        importImageFromCamera(activity, requestCode, -1);
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }


    public static void setScheduler(Scheduler scheduler) {
        Capturandro.scheduler = scheduler;
    }

    public static String getFileProviderAuthority() {
        return fileProviderAuthority;
    }

    /**
     * onCameraImport will trigger with an observable that will return an Uri onNext once the image is captured.
     * <p>
     * When executing subscribe on the Obserable<Uri> image processing may take place (on a seperate thread).
     * Image processing is done on a single background thread to prevent oom
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public void importImageFromCamera(final Activity activity, final int requestCode, final int longestSide) {
        String filename = BitmapUtil.getUniqueFilename(activity);
        state = new CameraState(longestSide, filename);

        Uri uri;
        File file = new File(filename);
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(activity,
                    getFileProviderAuthority(),
                    file);
        } else {
            uri = Uri.fromFile(file);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        activity.startActivityForResult(intent, requestCode);

    }

    public void importImageFromGallery(Activity activity, int requestCode) {
        importImageFromGallery(activity, requestCode, -1);
    }

    public void importImageFromGallery(Activity activity, int requestCode, int longestSide) {
        importImageFromGallery(activity, requestCode, longestSide, true);
    }

    /**
     * onGalleryImport will return one or more Obserable<Uri> once the images are selected.
     * <p>
     * When executing subscribe on the Obserable<Uri> image processing may take place (on a seperate thread).
     * * Image processing is done on a single background thread to prevent oom
     */
    public void importImageFromGallery(final Activity activity, final int requestCode, final int longestSide, final boolean multiselect) {
        state = new GalleryState(longestSide, multiselect);
        Intent intent;
        try {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (multiselect) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            if (multiselect) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
            activity.startActivityForResult(intent, requestCode);
        }
    }

    @SuppressWarnings("MissingPermission")
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) throws CapturandroException {
        if (state != null && resultCode == Activity.RESULT_OK) {
            if (state instanceof CameraState) {
                CameraState state = (CameraState) this.state;
                ImportHandler importHandler = new ImportHandler(activity, state.longestSide);
                callback.onImport(requestCode, importHandler.camera(scheduler, state.cameraFilename));
            } else if (state instanceof GalleryState) {
                GalleryState state = (GalleryState) this.state;
                ImportHandler importHandler = new ImportHandler(activity, state.longestSide);
                if (intent != null) {
                    ClipData clipData = intent.getClipData();
                    if (clipData != null && clipData.getItemCount() > 0) {
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            callback.onImport(requestCode, importHandler.gallery(scheduler, uri));
                        }
                        return;
                    }
                    Uri selectedImage = intent.getData();
                    callback.onImport(requestCode, importHandler.gallery(scheduler, selectedImage));
                } else {
                    throw new CapturandroException("intent is null on gallery import");
                }
            }
            state = null;
        }
    }

    public void importImageFromUri(Context context, Uri imageUri, int longestSide, int requestCode) {
        ImportHandler importHandler = new ImportHandler(context, longestSide);
        callback.onImport(requestCode, importHandler.gallery(scheduler, imageUri));
    }

    private void clearAllCachedBitmaps(Context context) {
        final File cacheDir = BitmapUtil.getCacheDir(context);
        if (cacheDir != null) {
            deleteTask = Completable.create(emitter -> {
                        File[] files = cacheDir.listFiles((dir, filename) ->
                                filename.startsWith(TEMPFILE_PREFIX) && filename.endsWith(".jpg")
                        );
                        if (files != null) {
                            for (File file : files) {
                                if (emitter.isDisposed()) {
                                    return;
                                }
                                try {
                                    file.delete();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        emitter.onComplete();
                    }
            )
                    .subscribeOn(Schedulers.io())
                    .subscribe();
        }
    }

    private static abstract class CapturandoState implements Parcelable {

        @Override
        public int describeContents() {
            return 0;
        }
    }

    private static class CameraState extends CapturandoState {

        private final int longestSide;
        private final String cameraFilename;

        public CameraState(int longestSide, String cameraFilename) {
            this.longestSide = longestSide;
            this.cameraFilename = cameraFilename;
        }

        public CameraState(Parcel in) {
            longestSide = in.readInt();
            cameraFilename = in.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(longestSide);
            dest.writeString(cameraFilename);
        }


        public static final Parcelable.Creator<CameraState> CREATOR =
                new Parcelable.Creator<CameraState>() {
                    public CameraState createFromParcel(Parcel in) {
                        return new CameraState(in);
                    }

                    public CameraState[] newArray(int size) {
                        return new CameraState[size];
                    }
                };

    }

    private static class GalleryState extends CapturandoState {

        private final int longestSide;
        private final boolean multiselect;

        public GalleryState(Parcel in) {
            longestSide = in.readInt();
            multiselect = in.readInt() == 1;
        }

        public GalleryState(int longestSide, boolean multiselect) {
            this.longestSide = longestSide;
            this.multiselect = multiselect;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(longestSide);
            dest.writeInt(multiselect ? 1 : 0);
        }

        public static final Parcelable.Creator<GalleryState> CREATOR =
                new Parcelable.Creator<GalleryState>() {
                    public GalleryState createFromParcel(Parcel in) {
                        return new GalleryState(in);
                    }

                    public GalleryState[] newArray(int size) {
                        return new GalleryState[size];
                    }
                };
    }

    public interface CapturandoCallback {
        void onImport(int requestCode, Observable<Uri> observable);
    }
}
