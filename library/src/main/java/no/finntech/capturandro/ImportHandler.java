package no.finntech.capturandro;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;

import static no.finntech.capturandro.OrientationUtil.getOrientation;
import static no.finntech.capturandro.OrientationUtil.readExifFromFile;

class ImportHandler {
    private final static String[] FILE_PATH_COLUMNS = {
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME
    };

    private Context context;
    private int longestSide;

    ImportHandler(Context context, int longestSide) {
        this.context = context;
        this.longestSide = longestSide;
    }

    Observable<Uri> camera(Scheduler scheduler, final String cameraFilename) {
        return Observable.create((ObservableOnSubscribe<Uri>) emitter -> {
                    if (cameraFilename != null) {
                        File file = new File(cameraFilename);
                        Uri bitmapUri = BitmapUtil.getProcessedImage(
                                context, file, longestSide, getOrientation(readExifFromFile(file))
                        );
                        emitter.onNext(bitmapUri);
                        emitter.onComplete();
                    } else {
                        emitter.onError(new CapturandroException("Could not get image from camera"));
                    }
                }
        ).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    Observable<Uri> gallery(Scheduler scheduler, final Uri selectedImage) {
        return Observable.create((ObservableOnSubscribe<Uri>) emitter -> {
            if (selectedImage == null) {
                emitter.onError(new CapturandroException("Could not get image - it's null"));
                return;
            }
            if (isUserAttemptingToAddVideo(selectedImage)) {
                emitter.onError(new CapturandroException("User selected video"));
                return;
            }
            try {
                int orientation = getOrientation(selectedImage, context.getContentResolver());
                InputStream inputStream = context.getContentResolver().openInputStream(selectedImage);
                if (inputStream == null) {
                    inputStream = openRemoteImage(selectedImage);
                }
                if (inputStream == null) {
                    emitter.onError(new CapturandroException("Could not resolve url " + selectedImage));
                    return;
                }
                try {
                    Uri uri = BitmapUtil.getProcessedImage(context, inputStream, longestSide, orientation);
                    emitter.onNext(uri);
                    emitter.onComplete();
                } finally {
                    inputStream.close();
                }
            } catch (IOException e) {
                emitter.onError(new CapturandroException(e));
            }
        }
        ).subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    private InputStream openRemoteImage(Uri selectedImage) throws IOException {
        Cursor cursor = context.getContentResolver().query(selectedImage, FILE_PATH_COLUMNS, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (columnIndex != -1) {
                    String url = cursor.getString(columnIndex);
                    if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                        return new URL(url).openStream();
                    }
                }
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }
}
