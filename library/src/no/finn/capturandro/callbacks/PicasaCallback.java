package no.finn.capturandro.callbacks;

public interface PicasaCallback {
    void onProgressUpdate(Integer... progress);
    void onDownloadComplete(String filename);

}
