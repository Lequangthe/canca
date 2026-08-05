package com.quangthe.canca.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.findViewTreeCompositionContext
import androidx.compose.ui.unit.Density
import androidx.core.content.FileProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.quangthe.canca.ui.theme.CANCATheme
import java.io.File
import java.io.FileOutputStream

object ScreenshotUtils {

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String = "result.png") {
        try {
            val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Gửi kết quả qua"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Renders a Composable into a Bitmap with forced standard font scale.
     */
    fun generateBitmapFromComposable(
        context: Context,
        content: @Composable () -> Unit
    ): Bitmap {
        val activity = context as? ComponentActivity ?: throw IllegalStateException("Context must be a ComponentActivity")
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            val parentComposition = rootView.findViewTreeCompositionContext()
            if (parentComposition != null) {
                setParentCompositionContext(parentComposition)
            }
            
            val lifecycleOwner = rootView.findViewTreeLifecycleOwner() ?: activity
            val viewModelStoreOwner = rootView.findViewTreeViewModelStoreOwner() ?: activity
            val savedStateRegistryOwner = rootView.findViewTreeSavedStateRegistryOwner() ?: activity
            
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)

            setContent {
                val currentDensity = LocalDensity.current
                // Force fontScale to 1.0 to prevent layout breakage on devices with large font settings
                CompositionLocalProvider(
                    LocalDensity provides Density(density = currentDensity.density, fontScale = 1.0f)
                ) {
                    CANCATheme(dynamicColor = false) {
                        Surface(color = Color.White) {
                            content()
                        }
                    }
                }
            }
        }

        composeView.visibility = View.INVISIBLE
        rootView.addView(composeView, FrameLayout.LayoutParams(1080, ViewGroup.LayoutParams.WRAP_CONTENT))

        try {
            val widthSpecFixed = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            
            composeView.measure(widthSpecFixed, heightSpec)
            
            val measuredWidth = composeView.measuredWidth
            val measuredHeight = composeView.measuredHeight
            
            val finalWidth = if (measuredWidth > 0) measuredWidth else 1080
            val finalHeight = if (measuredHeight > 0) measuredHeight else 2000
            
            composeView.layout(0, 0, finalWidth, finalHeight)

            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)
            
            return bitmap
        } finally {
            rootView.removeView(composeView)
        }
    }
}
