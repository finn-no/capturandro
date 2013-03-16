package no.finn.capturandro.callbacks;

public interface PicasaCallback {
    void onPicasaDownloadStarted(String filename);
    void onPicasaDownloadComplete(String filename);

}
