package com.muse.app.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muse.app.R
import com.muse.app.ui.components.MetaText
import com.muse.app.ui.components.Pressable
import com.muse.app.ui.components.findActivity
import com.muse.app.ui.theme.MuseColors

data class PhotoPerms(
    val granted: Boolean,
    val permanentlyDenied: Boolean,
    val request: () -> Unit
)

private fun requiredPermissions(): List<String> {
    val list = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= 33) list += Manifest.permission.READ_MEDIA_IMAGES
    else list += Manifest.permission.READ_EXTERNAL_STORAGE
    if (Build.VERSION.SDK_INT >= 29) list += Manifest.permission.ACCESS_MEDIA_LOCATION
    return list
}

private fun allGranted(context: Context): Boolean =
    requiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

@Composable
fun rememberPhotoPermissions(): PhotoPerms {
    val context = LocalContext.current
    val activity = context.findActivity()
    var granted by remember { mutableStateOf(allGranted(context)) }
    var deniedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        granted = allGranted(context)
        if (!granted) deniedOnce = true
    }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = allGranted(context)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val primary = requiredPermissions().first()
    val permanentlyDenied = !granted && deniedOnce &&
        activity != null &&
        !ActivityCompat.shouldShowRequestPermissionRationale(activity, primary)

    return PhotoPerms(
        granted = granted,
        permanentlyDenied = permanentlyDenied,
        request = { launcher.launch(requiredPermissions().toTypedArray()) }
    )
}

@Composable
fun PermissionScreen(perms: PhotoPerms) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_vector_photo),
            contentDescription = null,
            tint = MuseColors.White.copy(alpha = 0.9f),
            modifier = Modifier.height(46.dp).width(46.dp)
        )
        Spacer(Modifier.height(30.dp))
        Text(
            text = "私人数字博物馆",
            style = TextStyle(
                fontSize = 16.sp,
                letterSpacing = 7.sp,
                fontFamily = FontFamily.SansSerif,
                color = MuseColors.White
            ),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.permission_body),
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 23.sp,
                fontFamily = FontFamily.SansSerif,
                color = MuseColors.Gray1
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(270.dp)
        )
        Spacer(Modifier.height(48.dp))
        Pressable(onClick = {
            if (perms.permanentlyDenied) openAppSettings(context) else perms.request()
        }) {
            Box(
                Modifier
                    .border(BorderStroke(1.dp, MuseColors.White.copy(alpha = 0.55f)))
                    .padding(horizontal = 38.dp, vertical = 15.dp)
            ) {
                MetaText(
                    text = if (perms.permanentlyDenied) {
                        stringResource(R.string.permission_open_settings)
                    } else {
                        stringResource(R.string.permission_action)
                    },
                    color = MuseColors.White,
                    size = 11.sp,
                    letterSpacing = 3.sp
                )
            }
        }
        if (perms.permanentlyDenied) {
            Spacer(Modifier.height(26.dp))
            MetaText(
                text = stringResource(R.string.permission_denied_hint),
                color = MuseColors.Gray3,
                size = 10.sp,
                align = TextAlign.Center,
                modifier = Modifier.width(250.dp)
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
