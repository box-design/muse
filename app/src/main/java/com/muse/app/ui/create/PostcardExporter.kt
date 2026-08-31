package com.muse.app.ui.create

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean

object PostcardExporter {

    private const val EXPORT_WIDTH_PX = 1080
    private const val EXPORT_HEIGHT_PX = 1440
    private const val RENDER_TIMEOUT_MS = 5000L

    sealed class Failure : Exception() {
        object NoActivity : Failure()
        object PhotoUnavailable : Failure()
        object RenderTimeout : Failure()
        object StorageFailed : Failure()

        val messageLabel: String
            get() = when (this) {
                PhotoUnavailable -> "PHOTO UNAVAILABLE"
                RenderTimeout -> "EXPORT TIMEOUT"
                NoActivity, StorageFailed -> "EXPORT FAILED"
            }
    }

    suspend fun export(
        context: Context,
        view: DraftView,
        fileNameBase: String,
    ): Result<String> {
        return try {
            val activity = context.findActivity() ?: return Result.failure(Failure.NoActivity)
            val photo = withContext(Dispatchers.IO) { loadPhoto(context, view.uri) }
                ?: return Result.failure(Failure.PhotoUnavailable)
            val bitmap = renderToBitmap(activity, view, photo)
            withContext(Dispatchers.IO) {
                savePng(context, bitmap, sanitize(fileNameBase))
            }
        } catch (e: Failure) {
            Result.failure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.w("PostcardExporter", "export failed", e)
            Result.failure(Failure.StorageFailed)
        }
    }

    private suspend fun loadPhoto(context: Context, uri: String): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false)
            .build()
        return context.imageLoader.execute(request).drawable?.toBitmap()
    }

    private suspend fun renderToBitmap(
        activity: Activity,
        view: DraftView,
        photo: Bitmap,
        widthPx: Int = EXPORT_WIDTH_PX,
        heightPx: Int = EXPORT_HEIGHT_PX,
    ): Bitmap = withContext(Dispatchers.Main) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content)
            ?: throw Failure.NoActivity
        val firstDraw = AtomicBoolean(false)
        val composeView = ComposeView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
            translationX = -(widthPx * 2f)
            importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setViewTreeLifecycleOwner(activity as LifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
            setContent {
                val scale = with(LocalDensity.current) {
                    widthPx.toFloat() / BASE_CARD_DP.dp.toPx()
                }
                CompositionLocalProvider(
                    LocalStaticPhoto provides photo.asImageBitmap()
                ) {
                    Box(Modifier.drawBehind { firstDraw.set(true) }) {
                        ScaledPostcard(view = view, scale = scale)
                    }
                }
            }
        }
        root.addView(composeView, 0)
        try {
            withTimeout(RENDER_TIMEOUT_MS) {
                while (!firstDraw.get()) withFrameNanos {}
                repeat(2) { withFrameNanos {} }
            }
            Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { out ->
                composeView.draw(android.graphics.Canvas(out))
            }
        } catch (e: TimeoutCancellationException) {
            throw Failure.RenderTimeout
        } finally {
            root.removeView(composeView)
        }
    }

    private fun savePng(context: Context, bitmap: Bitmap, baseName: String): Result<String> {
        val resolver = context.contentResolver
        val displayName = "$baseName.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Muse")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw Failure.StorageFailed
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw Failure.StorageFailed
                }
            } ?: throw Failure.StorageFailed
            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, done, null, null)
            }
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        return Result.success(displayName)
    }

    private fun sanitize(raw: String): String {
        val cleaned = raw.uppercase()
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
            .take(40)
        val stamp = System.currentTimeMillis()
        return if (cleaned.isBlank()) "MUSE_POSTCARD_$stamp" else "MUSE_${cleaned}_$stamp"
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
