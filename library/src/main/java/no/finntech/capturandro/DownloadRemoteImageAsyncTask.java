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

public class DownloadRemoteImageAsyncTask extends AsyncTask<Void, Integer, Void> {
    private final CapturandroCallback.ImageHandler imageHandler;
    private final int longestSide;
    protected Context context;

    private Uri uri;
    private String filename;
    private Bitmap bitmap;
    private IOException exception;

    public DownloadRemoteImageAsyncTask(Context context, Uri imageToDownloadUri, String filename, CapturandroCallback.ImageHandler imageHandler, int longestSide) {
        this.context = context;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.imageHandler = imageHandler;
        this.longestSide = longestSide;
    }

    @Override
    protected void onPreExecute() {
        imageHandler.onGalleryImportStarted(filename);
    }

    @Override
    protected Void doInBackground(Void... voids) {
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
            exception = e;
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        int orientation = GalleryHandler.getOrientation(context.getContentResolver(), uri);
        bitmap = BitmapUtil.getProcessedBitmap(file, longestSide, orientation);
        file.delete();
        return null;
    }

    @Override
    protected void onPostExecute(Void o) {
        if (exception != null) {
            imageHandler.onGalleryImportFailure(filename, exception);
        } else {
            imageHandler.onGalleryImportSuccess(filename, bitmap);
        }
    }
}
