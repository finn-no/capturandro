package no.finntech.capturandro.sample;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
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

import no.finntech.capturandro.Capturandro;
import no.finntech.capturandro.CapturandroException;

import rx.Observable;
import rx.functions.Action1;

public class CapturandroSampleActivity extends Activity {
    private static final int CAMERA_RESULT_CODE = 1;
    private static final int GALLERY_RESULT_CODE = 2;

    private Capturandro capturandro = null;

    private static List<Uri> uris = new ArrayList<>(); // lazy mans onSaveInstanceState... really lazy :p

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (capturandro == null) {
            capturandro = new Capturandro(this, callback);
        }
        capturandro.onCreate(savedInstanceState);
        for (Uri uri : uris) {
            showImage(resolveBitmap(uri));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        capturandro.onSaveInstanceState(outState);
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
            capturandro.onActivityResult(this, requestCode, resultCode, data);
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

    private Bitmap resolveBitmap(Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Capturandro.CapturandoCallback callback = new Capturandro.CapturandoCallback() {
        int processing = 0;
        private ProgressDialog progressDialog = null;

        @Override
        public void onImport(int requestCode, Observable<Uri> observable) {
            if (progressDialog == null) {
                progressDialog = ProgressDialog.show(CapturandroSampleActivity.this, "Processing", "Processing image...", true);
            }
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }

            processing++;
            observable.subscribe(new Action1<Uri>() {
                @Override
                public void call(Uri uri) {
                    downloadComplete();
                    uris.add(uri);
                    showImage(resolveBitmap(uri));
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    downloadComplete();
                    Toast.makeText(CapturandroSampleActivity.this, "Import of image(s) from gallery failed", Toast.LENGTH_LONG).show();
                }
            });
        }

        private void downloadComplete() {
            processing--;
            if (progressDialog != null && progressDialog.isShowing() && processing == 0) {
                progressDialog.dismiss();
            }
        }
    };
}
