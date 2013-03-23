package no.finn.capturandro.asynctask;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import no.finn.capturandro.callbacks.CapturandroCallback;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadPicasaImageAsyncTask extends AsyncTask<Void, Integer, CapturandroCallback> {

    private final CapturandroCallback picasaCallback;
    protected Activity activity;

    private Uri uri;
    private String filename;

    public DownloadPicasaImageAsyncTask(Activity activity, Uri imageToDownloadUri, String filename, CapturandroCallback picasaCallback) {
        this.activity = activity;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.picasaCallback = picasaCallback;
    }

    @Override
    protected void onPreExecute(){
        picasaCallback.onPicasaImportStarted(filename);
    }

    @Override
    protected CapturandroCallback doInBackground(Void... voids) {
        File file = new File(activity.getExternalCacheDir(), filename);
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            if (uri.toString().startsWith("content://")) {
                try {
                    ContentResolver cr = activity.getContentResolver();
                    //Cursor cursor = cr.query(uri)
                    inputStream = activity.getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                }

            } else {
                inputStream = new URL(uri.toString()).openStream();
            }
            outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
        } catch (MalformedURLException e) {
            e.printStackTrace();  // TODO
        } catch (FileNotFoundException e) {
            e.printStackTrace();  // TODO
        } catch (IOException e) {
            e.printStackTrace();  // TODO
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        return picasaCallback;
    }

    @Override
    protected void onPostExecute(CapturandroCallback callback){
        callback.onPicasaImportSuccess(filename);
    }
}
