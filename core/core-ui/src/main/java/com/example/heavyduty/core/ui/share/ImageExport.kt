package com.example.heavyduty.core.ui.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Saves [bitmap] in the cache and opens the system's share sheet with it. */
suspend fun shareImage(context: Context, bitmap: Bitmap, chooserTitle: String) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, SHARE_DIR).apply { mkdirs() }.resolve(SHARE_FILE).also { file ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Also as ClipData: it's what the share sheet reads to show the image's thumbnail.
        .apply { clipData = ClipData.newRawUri(null, uri) }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}

/** Whether [saveImageToGallery] needs the storage permission first: only before Android 10. */
val savingToGalleryNeedsPermission: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

/**
 * Saves [bitmap] as a PNG in Pictures/HeavyDuty, where the gallery shows it. Returns false if it
 * couldn't be written (storage full, permission refused).
 */
suspend fun saveImageToGallery(context: Context, bitmap: Bitmap, displayName: String): Boolean = withContext(Dispatchers.IO) {
    try {
        if (Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            saveWithMediaStore(context, bitmap, displayName)
        } else {
            saveLegacy(context, bitmap, displayName)
        }
        true
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun saveWithMediaStore(context: Context, bitmap: Bitmap, displayName: String) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$GALLERY_ALBUM")
        // Hidden from the gallery until it's completely written.
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: throw IOException("MediaStore insert failed")
    val written = try {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) } == true
    } catch (_: IOException) {
        false
    }
    if (!written) {
        // No half-written image left behind in the gallery.
        resolver.delete(uri, null, null)
        throw IOException("Couldn't write $uri")
    }
    resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
}

@Suppress("DEPRECATION") // The public Pictures folder is the only gallery path before Android 10.
private fun saveLegacy(context: Context, bitmap: Bitmap, displayName: String) {
    val album = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), GALLERY_ALBUM)
    if (!album.isDirectory && !album.mkdirs()) throw IOException("Can't create $album")
    val file = File(album, "$displayName.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
}

/** Must match res/xml/file_paths.xml in the app module. */
private const val SHARE_DIR = "shared"
private const val SHARE_FILE = "workout.png"
private const val GALLERY_ALBUM = "HeavyDuty"
private const val PNG_QUALITY = 100
