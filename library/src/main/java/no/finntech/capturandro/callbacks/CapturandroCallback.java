package no.finntech.capturandro.callbacks;

import android.graphics.Bitmap;

import no.finntech.capturandro.asynctask.DownloadRemoteImageAsyncTask;

public interface CapturandroCallback {
    void onGalleryImportStarted(DownloadRemoteImageAsyncTask downloadRemoteImageAsyncTask, String filename);

    void onCameraImportFailure(Exception e);
    void onGalleryImportFailure(Exception e);

    void onImportSuccess(Bitmap bitmap, int resultCode);
}
