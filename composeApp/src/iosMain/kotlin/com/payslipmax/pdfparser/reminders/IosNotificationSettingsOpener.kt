package com.payslipmax.pdfparser.reminders

import com.payslipmax.pdfparser.part2.reminders.NotificationSettingsOpener

/** Opens this app's page in the Settings app through the Swift gateway (Kotlin/Native does not reach `UIApplication` itself). With no gateway registered it does nothing. */
class IosNotificationSettingsOpener(
    private val gateway: () -> IosNotificationGateway? = { IosReminderBridge.gateway },
) : NotificationSettingsOpener {
    override fun open() {
        gateway()?.openSettings()
    }
}
