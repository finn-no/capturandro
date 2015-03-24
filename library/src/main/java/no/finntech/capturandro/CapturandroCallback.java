package no.finntech.capturandro;

import android.graphics.Bitmap;

public interface CapturandroCallback {
    void onGalleryImportStarted(DownloadRemoteImageAsyncTask downloadRemoteImageAsyncTask, String filename);

    void onCameraImportFailure(Exception e);
    void onGalleryImportFailure(Exception e);

    void onImportSuccess(Bitmap bitmap, int resultCode);
}
