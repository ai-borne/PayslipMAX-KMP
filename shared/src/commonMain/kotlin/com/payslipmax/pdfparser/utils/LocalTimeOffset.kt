package com.payslipmax.pdfparser.utils

/**
 * The device's offset from UTC at the instant [atMillis] (epoch milliseconds), in milliseconds, east positive: +19,800,000 for India. It is
 * asked for the instant, not "now", so a daylight-saving change is right for a date in the past. Used to show the day an instant fell on
 * as the officer lived it (the day a slip was printed or a draft was edited), never to store anything.
 */
expect fun localUtcOffsetMillis(atMillis: Long): Long
