package no.finn.capturandro;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import no.finn.capturand.R;

import java.util.ArrayList;

public class DemoActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    private void handleSendImages(ArrayList<Uri> imagesFromIntent) {
//        for (Uri imageUri : imagesFromIntent){
//            adImageFilename = getFilename(System.currentTimeMillis() + ".jpg", adBuilder.getExternalAdId());
//
//            imageImportUtil.handleSendImage(imageUri, adImageFilename);
//        }
    }

    private boolean isReceivingImage() {
        String action = getIntent().getAction();
        String mimeType = getIntent().getType();
        return (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action))
                && (mimeType != null && mimeType.startsWith("image"));
    }
}
