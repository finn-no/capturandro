package no.finntech.capturandro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.ClipData;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

class DownloadMultipleAsyncTask extends AsyncTask<Void, String, Void> {
    private final CapturandroCallback.ImageHandler imageHandler;
    private final int longestSide;
    private final ClipData clipData;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;

    public DownloadMultipleAsyncTask(Context context, CapturandroCallback.ImageHandler imageHandler, int longestSide, ClipData clipData) {
        this.clipData = clipData;
        this.context = context;
        this.imageHandler = imageHandler;
        this.longestSide = longestSide;
    }

    @Override
    protected Void doInBackground(Void... params) {
        List<UUID> importIds = new ArrayList<>();

        for (int i = 0; i < clipData.getItemCount(); i++) {
            importIds.add(UUID.randomUUID());
        }

        // Notify that we've started X gallery imports
        for (int i = 0; i < clipData.getItemCount(); i++) {
            final UUID importId = importIds.get(i);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageHandler.onGalleryImportStarted(importId);
                }
            });
        }

        for (int i = 0; i < clipData.getItemCount(); i++) {
            final UUID importId = importIds.get(i);
            Uri uri = clipData.getItemAt(i).getUri();
            int orientation = GalleryHandler.getOrientation(context.getContentResolver(), uri);
            File file = new File(getRealPathFromURI(context, uri));
            final Uri importedUri = BitmapUtil.getProcessedImage(file, longestSide, orientation);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageHandler.onGalleryImportSuccess(importId, importedUri);
                }
            });
        }
        return null;
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

}
