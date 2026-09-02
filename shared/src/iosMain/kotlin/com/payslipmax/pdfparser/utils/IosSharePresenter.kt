package com.payslipmax.pdfparser.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
internal fun presentIosShare(items: List<Any>) {
    val keyWindow =
        UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val rootViewController = keyWindow?.rootViewController ?: return

    val activityController = UIActivityViewController(items, null)
    activityController.popoverPresentationController?.let { popover ->
        popover.sourceView = rootViewController.view
        val (width, height) = rootViewController.view.bounds.useContents { size.width to size.height }
        popover.sourceRect = CGRectMake(width / 2.0, height / 2.0, 0.0, 0.0)
        popover.permittedArrowDirections = 0u
    }
    rootViewController.presentViewController(activityController, animated = true, completion = null)
}
