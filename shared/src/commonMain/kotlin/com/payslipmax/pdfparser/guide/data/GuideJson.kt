package com.payslipmax.pdfparser.guide.data

import kotlinx.serialization.json.Json

/**
 * The one Json configuration for the Guide bundle. Unknown keys are ignored so a newer bundle that only
 * adds fields still loads; a breaking change bumps the bundle's major `version` instead.
 */
val GuideJson: Json = Json { ignoreUnknownKeys = true }
