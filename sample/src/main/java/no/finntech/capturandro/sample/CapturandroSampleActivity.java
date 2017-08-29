package no.finntech.capturandro.sample;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import no.finntech.capturandro.Capturandro;
import no.finntech.capturandro.CapturandroException;

public class CapturandroSampleActivity extends Activity {
    private static final int CAMERA_RESULT_CODE = 1;
    private static final int GALLERY_RESULT_CODE = 2;
    private static final int CAMERA_PERMISSION_CODE = 3;
    private static final int GALLERY_PERMISSION_CODE = 4;

    private Capturandro capturandro = null;

    private static List<Uri> uris = new ArrayList<>(); // lazy mans onSaveInstanceState... really lazy :p

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (capturandro == null) {
            capturandro = new Capturandro(this, callback, "no.finn.capturandro.provider");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            capturandro.onActivityResult(this, requestCode, resultCode, data);
        } catch (CapturandroException e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                capturandro.importImageFromCamera(this, CAMERA_RESULT_CODE, 1600);
            }
        } else if (requestCode == GALLERY_PERMISSION_CODE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                capturandro.importImageFromGallery(this, GALLERY_RESULT_CODE, 400);
            }
        }
    }

    public void addFromCameraClick(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, CAMERA_PERMISSION_CODE);
        } else {
            capturandro.importImageFromCamera(this, CAMERA_RESULT_CODE, 1600);
        }
    }

    public void addFromGalleryClick(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, GALLERY_PERMISSION_CODE);
        } else {
            capturandro.importImageFromGallery(this, GALLERY_RESULT_CODE, 400);
        }
    }

    private void showImage(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout) findViewById(R.id.image_list)).addView(imageView);
        TextView textView = new TextView(this);
        textView.setText(getString(R.string.dimensions, bitmap.getWidth(), bitmap.getHeight()));
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
            observable.subscribe(new Consumer<Uri>() {
                @Override
                public void accept(Uri uri) {
                    downloadComplete();
                    uris.add(uri);
                    showImage(resolveBitmap(uri));
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
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
