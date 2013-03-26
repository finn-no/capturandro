package no.finn.capturandro.asynctask;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import no.finn.capturandro.callbacks.CapturandroCallback;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;

public class DownloadPicasaImageAsyncTask extends AsyncTask<Void, Integer, CapturandroCallback> {

    private CapturandroCallback capturandroCallback;
    protected Context context;

    private Uri uri;
    private String filename;

    public DownloadPicasaImageAsyncTask(Context context, Uri imageToDownloadUri, String filename, CapturandroCallback capturandroCallback) {
        this.context = context;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.capturandroCallback = capturandroCallback;
    }

    @Override
    protected void onPreExecute(){
        capturandroCallback.onPicasaImportStarted(this, filename);
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
            capturandroCallback.onPicasaImportFailure(e);
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        return capturandroCallback;
    }

    @Override
    protected void onPostExecute(CapturandroCallback callback){
        callback.onPicasaImportSuccess(filename);
    }

    public void setCapturandroCallback(CapturandroCallback capturandroCallback){
        this.capturandroCallback = capturandroCallback;
    }
}
