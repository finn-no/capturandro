package no.finntech.capturandro;

import android.net.Uri;

import java.util.UUID;

public interface CapturandroCallback {

    ImageHandler createHandler(int requestCode);

    interface ImageResponseCallback {
        void onGalleryImportStarted(UUID importId);
        void onGalleryImportFailure(UUID importId, Exception e);
        void onGalleryImportSuccess(UUID importId, Uri uri);

        void onCameraImportFailure(UUID importId, Exception e);
        void onCameraImportSuccess(UUID importId, Uri uri);
    }

    class ImageHandler implements ImageResponseCallback {
        private ImageResponseCallback callback;
        private DeliverResult deliverResult;

        public ImageHandler() {
        }

        public ImageHandler execute(ImageResponseCallback callback) {
            this.callback = callback;
            attemptDeliver();
            return this;
        }

        private void attemptDeliver() {
            if (deliverResult != null && callback != null) {
                deliverResult.execute(callback);
                deliverResult = null;
            }
        }

        @Override
        public void onGalleryImportStarted(final UUID importId) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportStarted(importId);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onGalleryImportFailure(final UUID importId, final Exception e) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportFailure(importId, e);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onGalleryImportSuccess(final UUID importId, final Uri uri) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportSuccess(importId, uri);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onCameraImportFailure(final UUID importId,final Exception e) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onCameraImportFailure(importId, e);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onCameraImportSuccess(final UUID importId, final Uri uri) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onCameraImportSuccess(importId, uri);
                }
            };
            attemptDeliver();
        }

        interface DeliverResult {
            void execute(ImageResponseCallback callback);
        }

    }

}
