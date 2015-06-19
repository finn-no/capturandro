package no.finntech.capturandro;

import android.graphics.Bitmap;

public interface CapturandroCallback {

    ImageHandler createHandler(int requestCode);

    interface ImageResponseCallback {
        void onGalleryImportStarted(String filename);
        void onGalleryImportFailure(String filename, Exception e);
        void onGalleryImportSuccess(String filename, Bitmap bitmap);

        void onCameraImportFailure(String filename, Exception e);
        void onCameraImportSuccess(String filename, Bitmap bitmap);
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
        public void onCameraImportSuccess(final String filename, final Bitmap bitmap) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onCameraImportSuccess(filename, bitmap);
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
        public void onGalleryImportSuccess(final String filename, final Bitmap bitmap) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportSuccess(filename, bitmap);
                }
            };
            attemptDeliver();
        }


        interface DeliverResult {
            void execute(ImageResponseCallback callback);
        }
    }
}
