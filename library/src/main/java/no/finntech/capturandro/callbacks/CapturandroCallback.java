package no.finntech.capturandro.callbacks;

import no.finntech.capturandro.asynctask.DownloadPicasaImageAsyncTask;

public interface CapturandroCallback {
    void onCameraImportSuccess(String filename);
    void onCameraImportFailure(Exception e);

    void onPicasaImportStarted(DownloadPicasaImageAsyncTask downloadPicasaImageAsyncTask, String filename);
    void onPicasaImportSuccess(String filename);
    void onPicasaImportFailure(Exception e);
}
