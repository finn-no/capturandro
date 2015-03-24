package no.finntech.capturandro;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class DownloadRemoteImageAsyncTask extends AsyncTask<Void, Integer, CapturandroCallback> {

    private CapturandroCallback capturandroCallback;
    private final int resultCode;
    private final int longestSide;
    protected Context context;

    private Uri uri;
    private String filename;
    private Bitmap bitmap;

    public DownloadRemoteImageAsyncTask(Context context, Uri imageToDownloadUri, String filename, CapturandroCallback capturandroCallback, int resultCode, int longestSide) {
        this.context = context;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.capturandroCallback = capturandroCallback;
        this.resultCode = resultCode;
        this.longestSide = longestSide;
    }

    @Override
    protected void onPreExecute() {
        capturandroCallback.onGalleryImportStarted(this, filename);
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
            capturandroCallback.onGalleryImportFailure(e);
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
        callback.onImportSuccess(bitmap, resultCode);
    }

    public void setCapturandroCallback(CapturandroCallback capturandroCallback) {
        this.capturandroCallback = capturandroCallback;
    }
}
