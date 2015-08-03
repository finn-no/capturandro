package no.finntech.capturandro;

import android.net.Uri;

import rx.Observable;

public interface CapturandroCallback {

    void cameraImport(Observable<Uri> observable);

    void galleryImport(Observable<Uri> observable);
}
