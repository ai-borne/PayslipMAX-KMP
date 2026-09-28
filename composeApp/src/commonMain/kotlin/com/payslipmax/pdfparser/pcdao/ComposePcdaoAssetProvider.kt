package com.payslipmax.pdfparser.pcdao

import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import pdfparser.composeapp.generated.resources.Res

/**
 * Compose Multiplatform asset provider that reads PCDA canonical datasets
 * bundled in composeResources/files/pcdao.
 */
class ComposePcdaoAssetProvider : PcdaoAssetProvider {
    override suspend fun loadAsset(fileName: String): String {
        val resourcePath = "files/pcdao/$fileName"
        val bytes = Res.readBytes(resourcePath)
        return bytes.decodeToString()
    }
}
