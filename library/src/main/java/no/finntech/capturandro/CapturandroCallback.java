package no.finntech.capturandro;

import android.graphics.Bitmap;

public interface CapturandroCallback {
    void onGalleryImportStarted(DownloadRemoteImageAsyncTask downloadRemoteImageAsyncTask, String filename, int requestCode);

    void onCameraImportFailure(Exception e, int requestCode);

    void onGalleryImportFailure(Exception e, int requestCode);

    void onImportSuccess(Bitmap bitmap, int requestCode);
}
