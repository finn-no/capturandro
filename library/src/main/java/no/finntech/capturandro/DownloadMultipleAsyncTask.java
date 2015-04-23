package no.finntech.capturandro;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.content.ClipData;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

class DownloadMultipleAsyncTask extends AsyncTask<Void, String, Void> {
    private static final Random random = new Random(1);
    private final ContentResolver contentResolver;
    private final CapturandroCallback.ImageHandler imageHandler;
    private final int longestSide;
    private final ClipData clipData;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    public DownloadMultipleAsyncTask(ContentResolver contentResolver, CapturandroCallback.ImageHandler imageHandler, int longestSide, ClipData clipData) {
        this.clipData = clipData;
        this.contentResolver = contentResolver;
        this.imageHandler = imageHandler;
        this.longestSide = longestSide;
    }

    @Override
    protected Void doInBackground(Void... params) {
        List<String> filenames = new ArrayList<>();
        for (int i = 0; i < clipData.getItemCount(); i++) {
            filenames.add(getUniqueFilename());
        }

        // Notify that we've started X gallery imports
        for (int i = 0; i < clipData.getItemCount(); i++) {
            final String filename = filenames.get(i);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageHandler.onGalleryImportStarted(filename);
                }
            });
        }

        for (int i = 0; i < clipData.getItemCount(); i++) {
            final String filename = filenames.get(i);
            try {
                Bitmap mediaStoreBitmap = MediaStore.Images.Media.getBitmap(contentResolver, clipData.getItemAt(i).getUri());
                final Bitmap bitmap = BitmapUtil.getProcessedBitmap(mediaStoreBitmap, longestSide);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        imageHandler.onImportSuccess(filename, bitmap);
                    }
                });
            } catch (final IOException e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        imageHandler.onGalleryImportFailure(filename, e);
                    }
                });
            }
        }

        return null;
    }

    private String getUniqueFilename() {
        return "capturandro-" + System.currentTimeMillis() + random.nextInt() + ".jpg";
    }
}
