package com.payslipmax.pdfparser.utils

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.defaultTimeZone

actual fun localUtcOffsetMillis(atMillis: Long): Long =
    NSTimeZone.defaultTimeZone.secondsFromGMTForDate(NSDate.dateWithTimeIntervalSince1970(atMillis / MILLIS_PER_SECOND)) * MILLIS_PER_SECOND.toLong()

private const val MILLIS_PER_SECOND = 1000.0
