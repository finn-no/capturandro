package no.finn.capturandro.callbacks;

import android.content.Intent;

public interface CameraCallback {
    public void onActivityResult(int requestCode, int resultCode, Intent data);
    public void onImportSuccess(String filename);
    public void onImportFailure(Exception e);
}
