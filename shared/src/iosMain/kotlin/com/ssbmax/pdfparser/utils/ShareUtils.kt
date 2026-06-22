package com.ssbmax.pdfparser.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
actual fun shareText(text: String, title: String) {
    val keyWindow = UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = keyWindow?.rootViewController ?: return

    val activityController = UIActivityViewController(listOf(text), null)

    val popover = activityController.popoverPresentationController
    if (popover != null) {
        popover.sourceView = rootViewController.view
        popover.sourceRect = rootViewController.view.bounds
    }

    rootViewController.presentViewController(activityController, animated = true, completion = null)
}
