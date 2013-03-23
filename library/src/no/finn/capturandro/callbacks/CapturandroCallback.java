package no.finn.capturandro.callbacks;

public interface CapturandroCallback {
    void onCameraImportSuccess(String filename);
    void onCameraImportFailure(Exception e);

    void onPicasaImportStarted(String filename);
    void onPicasaImportSuccess(String filename);
    void onPicasaImportFailure(Exception e);
}
