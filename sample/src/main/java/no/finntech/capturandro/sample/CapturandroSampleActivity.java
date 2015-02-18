package no.finntech.capturandro.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import no.finntech.capturandro.Capturandro;
import no.finntech.capturandro.asynctask.DownloadRemoteImageAsyncTask;
import no.finntech.capturandro.callbacks.CapturandroCallback;
import no.finntech.capturandro.exception.CapturandroException;

public class CapturandroSampleActivity extends Activity implements CapturandroCallback {
    private static final int CAMERA_RESULT_CODE = 1;
    private static final int GALLERY_RESULT_CODE = 2;

    private Capturandro capturandro;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

            capturandro = new Capturandro.Builder(this)
                .withCameraCallback(this)
                .withCameraIntentResultCode(CAMERA_RESULT_CODE)
                .withGalleryIntentResultCode(GALLERY_RESULT_CODE)
                .build();
        try {
            capturandro.handleSharedImageIntent(getIntent());
        } catch (CapturandroException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void addFromCameraClick(View v) {
        capturandro.importImageFromCamera(this);
    }

    public void addFromGalleryClick(View v) {
        capturandro.importImageFromGallery(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            capturandro.onActivityResult(requestCode, resultCode, data);
        } catch (CapturandroException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImportSuccess(Bitmap bitmap) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        showImageFile(bitmap);
    }

    private void showImageFile(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout) findViewById(R.id.image_list)).addView(imageView);
    }

    @Override
    public void onCameraImportFailure(Exception e) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title_error_importing_image))
                .setMessage(getString(R.string.dialog_message_error_importing_image))
                .create();
    }

    @Override
    public void onGalleryImportStarted(DownloadRemoteImageAsyncTask downloadRemoteImageAsyncTask, String filename) {
        progressDialog = ProgressDialog.show(this, "Downloading", "Downloading image from Picasa...", true);
    }

    @Override
    public void onGalleryImportFailure(Exception e) {
        progressDialog.dismiss();
        Toast.makeText(this, "Import of image(s) from Picasa failed", Toast.LENGTH_LONG).show();
    }

}
