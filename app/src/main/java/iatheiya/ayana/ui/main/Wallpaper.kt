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
 * text stays readable. Nothing is drawn while there is none ([version] 0) or while it decodes.
 */
@Composable
fun Wallpaper(file: File, version: Long, dimPct: Int, blurDp: Int = 0, modifier: Modifier = Modifier) {
    if (version == 0L) return
    val image by produceState<ImageBitmap?>(null, file, version) {
        value = withContext(Dispatchers.IO) { decodeWallpaper(file) }
    }
    val bitmap = image ?: return
    Box(modifier.fillMaxSize()) {
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

/** The picture at no more than [MAX_SIDE_PX] on its long side, turned upright as the camera saved it. */
private fun decodeWallpaper(file: File): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE_PX) sample *= 2
    val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        ?: return@runCatching null
    upright(decoded, file).asImageBitmap()
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
