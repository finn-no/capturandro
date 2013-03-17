package no.finn.capturandro.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import no.finn.capturandro.callbacks.CameraCallback;
import no.finn.capturandro.callbacks.PicasaCallback;
import no.finn.capturandro.Capturandro;

import java.io.File;

public class CapturandroSampleActivity extends Activity implements CameraCallback, PicasaCallback {
    private Capturandro capturandro;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        capturandro = new Capturandro.Builder(this)
                                .withCameraCallback(this)
                                .withPicasaCallback(this)
                                .build();
        capturandro.handleImageIfSentFromGallery(getIntent());
    }

    public void addFromCameraClick(View v){
        capturandro.importImageFromCamera("camera_image.jpg");
    }

    public void addFromGalleryClick(View v){
        capturandro.importImageFromGallery("gallery_image.jpg");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        capturandro.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onImportSuccess(String filename) {
        showImageFile(filename);
    }

    private void showImageFile(String filename) {
        File imageFile = new File(getExternalCacheDir(), filename);

        if(imageFile.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            ((ImageView)findViewById(R.id.image)).setImageBitmap(bitmap);
        }
    }

    @Override
    public void onImportFailure(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title_error_importing_image))
            .setMessage(getString(R.string.dialog_message_error_importing_image))
            .create();
    }

    @Override
    public void onPicasaDownloadStarted(String filename) {
        progressDialog = ProgressDialog.show(this, "Downloading", "Downloading image from Picasa...", true);
    }

    @Override
    public void onPicasaDownloadComplete(String filename) {
        progressDialog.dismiss();
        showImageFile(filename);
    }
}
