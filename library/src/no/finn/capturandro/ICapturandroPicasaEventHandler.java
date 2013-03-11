package no.finn.capturandro;

import android.content.Intent;

public interface ICapturandroPicasaEventHandler {
    void onProgressUpdate(Integer... progress);
    void onFileDownloaded(String filename);
}
