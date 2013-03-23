package no.finn.capturandro.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import no.finn.capturandro.callbacks.CapturandroCallback;
import no.finn.capturandro.Capturandro;

import java.io.File;

public class CapturandroSampleActivity extends Activity implements CapturandroCallback {
    private Capturandro capturandro;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        capturandro = new Capturandro.Builder(this)
                                .withCameraCallback(this)
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
    public void onCameraImportSuccess(String filename) {
        showImageFile(filename);
    }

    private void showImageFile(String filename) {

        File imageFile = new File(getExternalCacheDir(), filename);

        if (imageFile.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setLayoutParams(new ViewPager.LayoutParams());
            ((LinearLayout)findViewById(R.id.view_pager_images)).addView(imageView);
        }
    }

    @Override
    public void onCameraImportFailure(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title_error_importing_image))
            .setMessage(getString(R.string.dialog_message_error_importing_image))
            .create();
    }

    @Override
    public void onPicasaImportStarted(String filename) {
        progressDialog = ProgressDialog.show(this, "Downloading", "Downloading image from Picasa...", true);
    }

    @Override
    public void onPicasaImportSuccess(String filename) {
        progressDialog.dismiss();
        showImageFile(filename);
    }

    @Override
    public void onPicasaImportFailure(Exception e) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
