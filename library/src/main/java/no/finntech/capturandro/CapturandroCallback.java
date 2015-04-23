package no.finntech.capturandro;

import android.graphics.Bitmap;

public interface CapturandroCallback {

    ImageHandler createHandler(int requestCode);

    interface ImageResponseCallback {
        void onGalleryImportStarted(String filename);

        void onCameraImportFailure(String filename, Exception e);

        void onGalleryImportFailure(String filename, Exception e);

        void onImportSuccess(String filename, Bitmap bitmap);
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
        public void onGalleryImportStarted(final String filename) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportStarted(filename);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onCameraImportFailure(final String filename, final Exception e) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onCameraImportFailure(filename, e);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onGalleryImportFailure(final String filename, final Exception e) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportFailure(filename, e);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onImportSuccess(final String filename, final Bitmap bitmap) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onImportSuccess(filename, bitmap);
                }
            };
            attemptDeliver();
        }


        interface DeliverResult {
            void execute(ImageResponseCallback callback);
        }
    }
}
