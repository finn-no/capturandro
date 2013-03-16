Capturandro
===========

Warning: While this is a working project, it is still work in progress, and we don't recommend using it in production.

About
-----
Capturandro is an easy-to-use image import library for Android.


Features
------
* Import image from camera
* Import local image from Gallery
* Import Picasa image from Gallery app
* Handle send intents (both single- and multiple images) from different applications.

How-To
------
Use Capturandro.Builder() to instansiate Capturandro and set required and optional parameters:
```java
Capturandro capturandro = Capturandro.Builder()
        .withCameraCallback(someCameraCallback) // See documentation below
        .withPicasaCallback(somePicasaCallback) // See documentation below
        .withStorageDirectory("/path/to/some/storage/dir"); // Discouraged! App uses getExternalCacheDir() by default
        .withFileName("someFilename.jpg") // Can be used if all imported images shall have the same filename
        .build();
```

Given the resulting instance is named capturandro, imports can be done as follows:

```java
// Import from camera:
capturandro.importImageFromCamera("savedCameraImage.jpg");

// Import image from gallery
capturandro.importImageFromGallery("savedGalleryImage.jpg")
```

License
-------

    Copyright 2013 FINN.no.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
