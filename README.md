[![Build Status](https://travis-ci.org/finn-no/capturandro.svg?branch=master)](https://travis-ci.org/finn-no/capturandro)
[![Download](https://api.bintray.com/packages/finnandroid/maven/capturandro/images/download.svg)](https://bintray.com/finnandroid/maven/capturandro/_latestVersion)
# Capturandro

## About
Capturandro is an easy-to-use image import library for Android.


## Features
* Import image from camera
* Import local image from Gallery

## How To Use
### Capturandro instance
Use Capturandro(Context context, CapturandoCallback callback, String fileProviderAuthority) to create an instance of Capturandro and set parameters:
```java
Capturandro capturandro = new Capturandro(activity, yourCapturandroCallback, fileProviderAuthority);
```

### CapturandroCallback
In order for Capturandro to function, you need to implement CapturandroCallback in your application. This class contains
a single method that will be called when you the import is completed (or failed):
```java
void onImport(int requestCode, Observable<Uri> observable)
```

If import was successful, the Observable's `onNext` will provide a `Uri` to the imported image. This will 
be a _content://_ URI on devices running Nougat or higher, and a _file://_ URI on older Android installations.
Make sure you can handle both types. The `requestCode` parameter contains the code you supplied to one of 
the `importImage*` methods described in the next section.

### Image import
Assuming your Capturandro instance is named capturandro, imports can be done as follows:

```java
// Import from camera, max 1024 pixels in either direction:
capturandro.importImageFromCamera(activity, MY_CAMERA_CODE, 1024); 

// Import from gallery, allow multiple images to be selected
capturandro.importImageFromGallery(activity, MY_GALLERY_CODE, 1024, true);
```

### Update your AndroidManifest.xml
Make sure to have the following permission line in your AndroidManifest.xml. If you are targeting Marshmallow or newer, make
sure you have been granted this permission before using Capturandro.
```xml
<uses-permission android:name="android.permission.CAMERA" />
```

Since Nougat, you are no longer allowed to share file:// URIs between applications, so you will need a
FileProvider to create content:// URIs for you. The `authorities` field here is what you want to provide as
the final argument to the Capturandro constructor:
```xml
<provider
        android:authorities="${applicationId}.fileprovider"
        android:name="android.support.v4.content.FileProvider"
        android:exported="false"
        android:grantUriPermissions="true">
    <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/provider_paths"/>
</provider>
```

Lastly, you'll need to specify the path where you'll be storing your captured images in
`res/xml/provider_paths.xml`. Capturandro will attempt to store images in `Context.getExternalCacheDir` or, failing that,
`Context.getCacheDir`. (Read more about 
[FileProvider](https://developer.android.com/reference/android/support/v4/content/FileProvider.html)). If you
prefer the more restrictive `files-external-path` to `external-path` you will need a recent (24.2.x) version of the support library:
```xml
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-cache-path name="external-cache-path" path="."/>
    <cache-path name="private-cache-path" path="." />
    ...
</paths>
```

### Gradle
```groovy
repositories {
    jcenter()
}

dependencies {
    compile('no.finntech:capturandro:1.0.18@aar')
}
```

## License

    Copyright (C) 2020 FINN.no.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
