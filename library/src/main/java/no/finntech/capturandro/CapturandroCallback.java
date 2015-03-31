package no.finntech.capturandro;

import android.graphics.Bitmap;

public interface CapturandroCallback {

    ImageHandler createHandler(int requestCode);

    interface ImageResponseCallback {
        void onGalleryImportStarted(String filename);

        void onCameraImportFailure(Exception e);

        void onGalleryImportFailure(Exception e);

        void onImportSuccess(Bitmap bitmap);
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
        public void onCameraImportFailure(final Exception e) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onCameraImportFailure(e);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onGalleryImportFailure(final Exception e) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onGalleryImportFailure(e);
                }
            };
            attemptDeliver();
        }

        @Override
        public void onImportSuccess(final Bitmap bitmap) {
            deliverResult = new DeliverResult() {
                @Override
                public void execute(ImageResponseCallback callback) {
                    callback.onImportSuccess(bitmap);
                }
            };
            attemptDeliver();
        }


        interface DeliverResult {
            void execute(ImageResponseCallback callback);
        }
    }
}
