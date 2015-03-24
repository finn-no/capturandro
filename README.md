# Capturandro

## About
Capturandro is an easy-to-use image import library for Android.


## Features
* Import image from camera
* Import local image from Gallery
* Import Picasa image from Gallery app
* Handle send intents (both single- and multiple images) from different applications.

## How To Use
### Capturandro.Builder()
Use Capturandro.Builder() to create an instance of Capturandro and set parameters:
```java
Capturandro capturandro = new Capturandro.Builder()
        .withCapturandroCallback(yourCapturandroCallback) // See documentation below
        .withFilenamePrefix("capturandro") // Prefixed filenames with "capturandro_", e.g. "capturandro_001.jpg"
        .build();
```

### CapturandroCallback
In order for Capturandro to function, you need to implement CapturandroCallback in your application. This class describes
methods that will be called when you the import is completed (or failed):
```java
    void onCameraImportSuccess(String filename);
    void onCameraImportFailure(Exception e);

    // Called when user has selected a Picasa image from gallery. If you want to show a progress indicator, 
    // this is the place to show it to the user.
    void onPicasaImportStarted(String filename);  
    
    // ...and this is the where you should hide it
    void onPicasaImportSuccess(String filename);    
    void onPicasaImportFailure(Exception e);
```

### Image import
Assuming your Capturandro instance is named capturandro, imports can be done as follows:

```java
// Import from camera with given output filename:
capturandro.importImageFromCamera(activity, 1024);  // current android activity and max 1024
                                                    // pixels in either direction

// Import from gallery, storing image with semi-random filename: [filenamePrefix] + System.currentTimeMillis() + ".jpg"
capturandro.importImageFromGallery()
```

### Update your AndroidManifest.xml
Make sure to have the following permission lines in your AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### Send/share intents
Capturandro supports send/share intents, meaning that apps (e.g. Gallery) can share images with your app.
But, you will have to do some job in your own app to make it work.

In your AndroidManifest.xml, add the following to the activity that you want to handle the send intent:
```xml
<activity android:name=".SomeActivity">
    <intent-filter>
        <action android:name="android.intent.action.SEND"/>
        <action android:name="android.intent.action.SEND_MULTIPLE"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="image/*"/>
    </intent-filter>
</activity>
```
In addition you need to insert the following code in the onCreate(..) method of the activity, 
passing the intent passed in to the activity:
```java
capturandro.handleImageIfSentFromGallery(getIntent());
```

## License

    Copyright (C) 2015 FINN.no.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
