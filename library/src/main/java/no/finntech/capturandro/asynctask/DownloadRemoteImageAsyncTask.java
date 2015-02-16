package no.finntech.capturandro.asynctask;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;

import no.finntech.capturandro.callbacks.CapturandroCallback;
import no.finntech.capturandro.util.BitmapUtil;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import static no.finntech.capturandro.Config.STORED_IMAGE_HEIGHT;
import static no.finntech.capturandro.Config.STORED_IMAGE_WIDTH;

public class DownloadRemoteImageAsyncTask extends AsyncTask<Void, Integer, CapturandroCallback> {

    private CapturandroCallback capturandroCallback;
    protected Context context;

    private Uri uri;
    private String filename;

    public DownloadRemoteImageAsyncTask(Context context, Uri imageToDownloadUri, String filename, CapturandroCallback capturandroCallback) {
        this.context = context;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.capturandroCallback = capturandroCallback;
    }

    @Override
    protected void onPreExecute(){
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

        ExifInterface exifInterface = BitmapUtil.getExifFromFile(file);
        if (exifInterface != null) {
            BitmapUtil.resizeAndRotateAndSaveBitmapFile(file, file, exifInterface, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
        } else {
            BitmapUtil.resizeAndSaveBitmapFile(file, file, STORED_IMAGE_WIDTH, STORED_IMAGE_HEIGHT);
        }

        return capturandroCallback;
    }

    @Override
    protected void onPostExecute(CapturandroCallback callback){
        callback.onGalleryImportSuccess(filename);
    }

    public void setCapturandroCallback(CapturandroCallback capturandroCallback){
        this.capturandroCallback = capturandroCallback;
    }
}
