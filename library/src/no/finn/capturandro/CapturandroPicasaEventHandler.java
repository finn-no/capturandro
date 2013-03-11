package no.finn.capturandro;

import android.content.Intent;

public interface CapturandroPicasaEventHandler {
    void onProgressUpdate(Integer... progress);
    void onFileDownloaded(String filename);
}
