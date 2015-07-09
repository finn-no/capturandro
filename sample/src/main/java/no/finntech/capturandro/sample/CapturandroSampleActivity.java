package no.finntech.capturandro.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

import no.finntech.capturandro.Capturandro;
import no.finntech.capturandro.CapturandroCallback;
import no.finntech.capturandro.CapturandroException;

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
                .withCallback(this)
                .build();
    }

    public void addFromCameraClick(View v) {
        capturandro.importImageFromCamera(this, CAMERA_RESULT_CODE, 1600);
    }

    public void addFromGalleryClick(View v) {
        capturandro.importImageFromGallery(this, GALLERY_RESULT_CODE, 400);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            capturandro.onActivityResult(requestCode, resultCode, data);
        } catch (CapturandroException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showImage(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout) findViewById(R.id.image_list)).addView(imageView);
        TextView textView = new TextView(this);
        textView.setText("width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());
        ((LinearLayout) findViewById(R.id.image_list)).addView(textView);
    }


    @Override
    public ImageHandler createHandler(int requestCode) {
        return new ImageHandler().execute(new ImageResponseCallback() {
            private int downloads = 0;

            @Override
            public void onCameraImportFailure(UUID importId, Exception e) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CapturandroSampleActivity.this);
                builder.setTitle(getString(R.string.dialog_title_error_importing_image))
                        .setMessage(getString(R.string.dialog_message_error_importing_image))
                        .create();
            }

            @Override
            public void onCameraImportSuccess(UUID importId, Uri uri) {
                Bitmap bitmap = resolveBitmap(uri);
                showImage(bitmap);
            }

            @Override
            public void onGalleryImportStarted(UUID importId) {
                downloads++;
                if (progressDialog == null) {
                    progressDialog = ProgressDialog.show(CapturandroSampleActivity.this, "Downloading", "Downloading image from Picasa...", true);
                }
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            }

            @Override
            public void onGalleryImportFailure(UUID importId, Exception e) {
                downloads--;
                if (downloads == 0) {
                    progressDialog.dismiss();
                }
                Toast.makeText(CapturandroSampleActivity.this, "Import of image(s) from Picasa failed", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onGalleryImportSuccess(UUID importId, Uri uri) {
                downloads--;
                if (progressDialog != null && progressDialog.isShowing() && downloads == 0) {
                    progressDialog.dismiss();
                }
                showImage(resolveBitmap(uri));
            }
        });
    }

    private Bitmap resolveBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

}
