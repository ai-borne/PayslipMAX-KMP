package com.payslipmax.pdfparser.debugseed

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.payslipmax.pdfparser.ui.screens.DeveloperToolsRegistry

/**
 * Debug-only entry point. Android creates providers before `Application.onCreate`, so this only hands the
 * registry a composable; Koin is read later, at composition. It is declared in the debug manifest only, so a
 * release build has neither this class nor the manifest entry.
 */
class DebugSeedProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        DeveloperToolsRegistry.register(REGISTRY_KEY) { DebugSeedSection() }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int = 0

    private companion object {
        const val REGISTRY_KEY = "debug-seed"
    }
}
