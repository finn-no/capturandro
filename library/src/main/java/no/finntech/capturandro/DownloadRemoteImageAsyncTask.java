package no.finntech.capturandro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;

public class DownloadRemoteImageAsyncTask extends AsyncTask<Void, Integer, CapturandroCallback> {

    private final CapturandroCallback capturandroCallback;
    private final int requestCode;
    private final int longestSide;
    protected Context context;

    private Uri uri;
    private String filename;
    private Bitmap bitmap;

    public DownloadRemoteImageAsyncTask(Context context, Uri imageToDownloadUri, String filename, CapturandroCallback capturandroCallback, int requestCode, int longestSide) {
        this.context = context;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.capturandroCallback = capturandroCallback;
        this.requestCode = requestCode;
        this.longestSide = longestSide;
    }

    @Override
    protected void onPreExecute() {
        capturandroCallback.onGalleryImportStarted(this, filename, requestCode);
    }

    @Override
    protected CapturandroCallback doInBackground(Void... voids) {
        File file = new File(context.getExternalCacheDir(), filename);
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            if (uri.toString().startsWith("content://")) {
                inputStream = context.getContentResolver().openInputStream(uri);
            } else {
                inputStream = new URL(uri.toString()).openStream();
            }
            outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            capturandroCallback.onGalleryImportFailure(e, requestCode);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        bitmap = BitmapUtil.getProcessedBitmap(file, longestSide);
        file.delete();

        return capturandroCallback;
    }

    @Override
    protected void onPostExecute(CapturandroCallback callback) {
        callback.onImportSuccess(bitmap, requestCode);
    }
}
