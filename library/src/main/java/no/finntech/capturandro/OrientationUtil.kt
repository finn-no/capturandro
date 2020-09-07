package no.finntech.capturandro

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException

object OrientationUtil {

    @JvmStatic
    fun getOrientation(photoUri: Uri, contentResolver: ContentResolver): Int {
        val exif = readExif(photoUri, contentResolver)
        if (exif != null) {
            return getOrientation(exif)
        } else {
            val projection = arrayOf(MediaStore.Images.ImageColumns.ORIENTATION)
            val cursor = contentResolver.query(photoUri, projection, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0)
                }
                cursor.close()
            }
        }
        return ExifInterface.ORIENTATION_UNDEFINED
    }

    @JvmStatic
    fun getOrientation(exif: ExifInterface?): Int {
        return exif?.let {
            val orientation = it.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED
            )
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: ExifInterface.ORIENTATION_UNDEFINED
    }

    private fun readExif(photoUri: Uri, contentResolver: ContentResolver): ExifInterface? {
        return if (photoUri.scheme == "file") {
            readExifFromFile(File(photoUri.toString()))
        } else {
            readExifFromContent(contentResolver, photoUri)
        }
    }

    @JvmStatic
    fun readExifFromFile(file: File): ExifInterface? {
        try {
            return ExifInterface(file)
        } catch (e: IOException) {
        }
        Log.i("Capturandro", "Unable to read exif data from file: ${file.path}")
        return null
    }

    private fun readExifFromContent(contentResolver: ContentResolver, photoUri: Uri): ExifInterface? {
        try {
            contentResolver.openInputStream(photoUri).use { inputStream ->
                if (inputStream != null) {
                    return ExifInterface(inputStream)
                }
            }
        } catch (e: IOException) {
        }
        Log.i("Capturandro", "Unable to read exif data from uri: $photoUri")
        return null
    }
}