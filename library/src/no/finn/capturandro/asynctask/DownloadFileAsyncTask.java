package no.finn.capturandro.asynctask;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import no.finn.capturandro.CapturandroPicasaEventHandler;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class DownloadFileAsyncTask extends AsyncTask<FileObject, Integer, CapturandroPicasaEventHandler> {

    private final CapturandroPicasaEventHandler capturandroPicasaEventHandler;
    protected ProgressDialog dialog;
    protected Activity activity;

    private Uri uri;
    private String filename;

    public DownloadFileAsyncTask(Activity activity, Uri imageToDownloadUri, String filename, CapturandroPicasaEventHandler iFileDownloadResult) {
        this.activity = activity;
        this.uri = imageToDownloadUri;
        this.filename = filename;
        this.capturandroPicasaEventHandler = iFileDownloadResult;
    }


//    private Bitmap getPicasaBitmap(String filename, Uri uri) throws IOException {
//
//        // BitmapUtil.resizeAndSaveBitmapFile(filename, Config.STORED_AD_IMAGE_WIDTH, Config.STORED_AD_IMAGE_HEIGHT);
//
//        return BitmapUtil.decodeBitmap(filename, 400, 400);
//    }

    @Override
    protected CapturandroPicasaEventHandler doInBackground(FileObject... fileObjects) {
        File file = new File(activity.getExternalCacheDir(), filename);

        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            if (uri.toString().startsWith("content://")) {
                try {
                    inputStream = activity.getContentResolver().openInputStream(uri);
                } catch (FileNotFoundException e) {
                }

            } else {
                inputStream = new URL(uri.toString()).openStream();
            }
            outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            IOUtils.closeQuietly(outputStream);
            IOUtils.closeQuietly(inputStream);
        }

        return capturandroPicasaEventHandler;
    }

    @Override
    protected void onPostExecute(CapturandroPicasaEventHandler o){
        o.onFileDownloaded(filename);
    }
}
