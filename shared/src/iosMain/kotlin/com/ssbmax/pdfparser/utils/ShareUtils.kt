package com.ssbmax.pdfparser.utils

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

actual fun shareText(text: String, title: String) {
    val keyWindow = UIApplication.sharedApplication.keyWindow
        ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = keyWindow?.rootViewController ?: return

    val activityController = UIActivityViewController(listOf(text), null)
    
    activityController.popoverPresentationController?.let { popover ->
        popover.sourceView = rootViewController.view
        popover.sourceRect = rootViewController.view.bounds
    }

    rootViewController.presentViewController(activityController, animated = true, completion = null)
}
