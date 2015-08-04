package no.finntech.capturandro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

class ImportHandler {
    private static final Scheduler scheduler = Schedulers.from(Executors.newSingleThreadExecutor());

    private Context context;
    private int longestSide;

    ImportHandler(Context context, int longestSide) {
        this.context = context;
        this.longestSide = longestSide;
    }


    Observable<Uri> camera(final String cameraFilename) {
        return Observable.create(new Observable.OnSubscribe<Uri>() {
                                     @Override
                                     public void call(Subscriber<? super Uri> subscriber) {
                                         subscriber.onStart();
                                         if (cameraFilename != null) {
                                             File file = new File(cameraFilename);
                                             subscriber.onNext(BitmapUtil.getProcessedImage(context, file, longestSide, getOrientation(file)));
                                             subscriber.onCompleted();
                                         } else {
                                             subscriber.onError(new CapturandroException("Could not get image from camera"));
                                         }
                                     }
                                 }
        ).onBackpressureBuffer().subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    Observable<Uri> gallery(final Uri selectedImage) {
        return Observable.create(new Observable.OnSubscribe<Uri>() {
                                     @Override
                                     public void call(Subscriber<? super Uri> subscriber) {
                                         subscriber.onStart();
                                         if (selectedImage == null) {
                                             subscriber.onError(new CapturandroException("Could not get image - it's null"));
                                             return;
                                         }
                                         if (isUserAttemptingToAddVideo(selectedImage)) {
                                             subscriber.onError(new CapturandroException("User selected video"));
                                             return;
                                         }
                                         try {
                                             int orientation = getOrientation(context.getContentResolver(), selectedImage);
                                             InputStream inputStream = context.getContentResolver().openInputStream(selectedImage);
                                             try {
                                                 Uri uri = BitmapUtil.getProcessedImage(context, inputStream, longestSide, orientation);
                                                 returnAndComplete(subscriber, uri);
                                             } finally {
                                                 inputStream.close();
                                             }
                                         } catch (IOException e) {
                                             subscriber.onError(new CapturandroException(e));
                                         }
                                     }

                                     private void returnAndComplete(Subscriber<? super Uri> subscriber, Uri uri) {
                                         subscriber.onNext(uri);
                                         subscriber.onCompleted();
                                     }
                                 }
        ).onBackpressureBuffer().subscribeOn(scheduler).observeOn(AndroidSchedulers.mainThread());
    }

    private static int getOrientation(ContentResolver contentResolver, Uri photoUri) {
        Cursor cursor = contentResolver.query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                return -1;
            }
        } finally {
            cursor.close();
        }
    }

    private static int getOrientation(File file) {
        ExifInterface exif = getExifFromFile(file);

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        switch (orientation) {
            case 0:
                break;
            case 3:
                orientation = 180;
                break;
            case 6:
                orientation = 90;
                break;
            case 8:
                orientation = 270;
                break;
            default:
                orientation = 0;
                break;
        }
        return orientation;
    }

    private static ExifInterface getExifFromFile(File file) {
        try {
            return new ExifInterface(file.getPath());
        } catch (IOException e) {
            Log.i("Capturandro", "Could not read Exif data from file: " + file.getPath());
        }
        return null;
    }

    private boolean isUserAttemptingToAddVideo(Uri selectedImage) {
        return selectedImage != null && selectedImage.toString().startsWith("content://media/external/video/");
    }

}