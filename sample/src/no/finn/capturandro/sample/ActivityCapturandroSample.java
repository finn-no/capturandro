package no.finn.capturandro.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import no.finn.capturandro.CapturandroEventHandler;
import no.finn.capturandro.CapturandroPicasaEventHandler;
import no.finn.capturandro.util.Capturandro;

import java.io.File;

public class ActivityCapturandroSample extends Activity implements CapturandroEventHandler, CapturandroPicasaEventHandler {
    private Capturandro capturandro;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        capturandro = new Capturandro.Builder(this)
                                .withEventHandler(this)
                                .withPicasaEventHandler(this)
                                .build();
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
        builder.setTitle("Error importing image")
            .setMessage("An error occured while importing image")
            .create();
    }

    @Override
    public void onProgressUpdate(Integer... progress) {
        Log.d("Capturandro", "Progress is now " + progress);
    }

    @Override
    public void onFileDownloaded(String filename) {
        showImageFile(filename);
    }
}
