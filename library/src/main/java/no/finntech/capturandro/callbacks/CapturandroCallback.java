package no.finntech.capturandro.callbacks;

import no.finntech.capturandro.asynctask.DownloadRemoteImageAsyncTask;

public interface CapturandroCallback {
    void onCameraImportSuccess(String filename);
    void onCameraImportFailure(Exception e);

    void onGalleryImportStarted(DownloadRemoteImageAsyncTask downloadRemoteImageAsyncTask, String filename);
    void onGalleryImportSuccess(String filename);
    void onGalleryImportFailure(Exception e);
}
