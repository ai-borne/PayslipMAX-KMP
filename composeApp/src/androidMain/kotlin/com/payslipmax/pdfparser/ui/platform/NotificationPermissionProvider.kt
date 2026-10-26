package com.payslipmax.pdfparser.ui.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import com.payslipmax.pdfparser.reminders.AndroidNotificationPermission

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    val holder = remember { arrayOfNulls<AndroidNotificationPermission>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { holder[0]?.onResult(it) }
    return remember(context, launcher) {
        AndroidNotificationPermission(
            sdk = Build.VERSION.SDK_INT,
            isGranted = { ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED },
            areEnabled = { NotificationManagerCompat.from(context).areNotificationsEnabled() },
            launch = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        ).also { holder[0] = it }
    }
}
