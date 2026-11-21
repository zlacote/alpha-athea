package iatheiya.ayana.ui.main

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Larger photos are decoded at a fraction of their size: the screen never needs more. */
private const val MAX_SIDE_PX = 2048

/**
 * The user's wallpaper, filling the area behind the transcript and darkened by [dimPct] so the
 * text stays readable. Nothing is drawn while there is none ([version] 0). It is usually decoded
 * already ([WallpaperCache.prefetch], at start); when it is not, it fades in once it is, instead
 * of popping in.
 */
@Composable
fun Wallpaper(file: File, version: Long, dimPct: Int, blurDp: Int = 0, modifier: Modifier = Modifier) {
    if (version == 0L) return
    val ready = remember(file, version) { WallpaperCache.cached(file, version) }
    val image by produceState(ready, file, version) {
        if (value == null) value = withContext(Dispatchers.IO) { WallpaperCache.load(file, version) }
    }
    val bitmap = image ?: return
    val shown = remember(file, version) { Animatable(if (ready != null) 1f else 0f) }
    LaunchedEffect(bitmap) { shown.animateTo(1f, tween(WALLPAPER_FADE_MS)) }
    Box(modifier.fillMaxSize().graphicsLayer { alpha = shown.value }) {
        Image(
            bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurDp > 0) Modifier.blur(blurDp.dp) else Modifier),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimPct.coerceIn(0, 100) / 100f)),
        )
    }
}

private const val WALLPAPER_FADE_MS = 260

/**
 * The decoded wallpaper, kept for the life of the app: a screen opened again, or the app brought
 * back, shows it at once instead of decoding the photo anew.
 */
object WallpaperCache {
    private var key: Pair<String, Long>? = null
    private var image: ImageBitmap? = null

    fun cached(file: File, version: Long): ImageBitmap? = synchronized(this) {
        image.takeIf { key == file.path to version }
    }

    fun load(file: File, version: Long): ImageBitmap? = synchronized(this) {
        cached(file, version) ?: decodeWallpaper(file)?.also {
            key = file.path to version
            image = it
        }
    }

    /** Decodes it ahead, off the main thread, as the app starts. */
    suspend fun prefetch(file: File, version: Long) {
        if (version != 0L) withContext(Dispatchers.IO) { load(file, version) }
    }
}

/**
 * Rewrites a newly picked photo as the wallpaper it will be: upright, no larger than
 * [MAX_SIDE_PX], as a plain JPEG. Opening the app then decodes a small file, not a camera's
 * full-size photo.
 */
fun shrinkWallpaper(file: File) {
    runCatching {
        val bitmap = decodeUpright(file) ?: return
        val scale = MAX_SIDE_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
        val fitted = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        val temp = File(file.path + ".tmp")
        temp.outputStream().use { fitted.compress(Bitmap.CompressFormat.JPEG, WALLPAPER_QUALITY, it) }
        if (!temp.renameTo(file)) temp.delete()
    }
}

private const val WALLPAPER_QUALITY = 92

private fun decodeWallpaper(file: File): ImageBitmap? = decodeUpright(file)?.asImageBitmap()

/** The picture at no more than about [MAX_SIDE_PX] on its long side, turned upright as the camera saved it. */
private fun decodeUpright(file: File): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
    val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return@runCatching null
    upright(decoded, file)
}.getOrNull()

private fun upright(bitmap: Bitmap, file: File): Bitmap {
    val degrees = when (
        ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
