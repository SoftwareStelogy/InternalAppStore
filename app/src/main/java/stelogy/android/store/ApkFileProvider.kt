package stelogy.android.store

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileFilter
import java.util.Date
import java.util.concurrent.TimeUnit

class ApkFileProvider : FileProvider() {

    companion object {

        private const val TAG = "ApkFileProvider"
        private const val MIME_TYPE_APK = "application/vnd.android.package-archive"
        private const val FILE_EXTENSION_APK = ".apk"

        fun uri(file: File, context: Context): Uri = getUriForFile(context, "${context.packageName}.apk_provider", file)

        private fun apkFilename(version: stelogy.android.store.Version, withExtension: Boolean = true) = "${version.key}_${version.apkGeneration}${if (withExtension) stelogy.android.store.ApkFileProvider.Companion.FILE_EXTENSION_APK else ""}"

        fun tempApkFile(context: Context, version: stelogy.android.store.Version): File = File(context.cacheDir, "~${stelogy.android.store.ApkFileProvider.Companion.apkFilename(version)}")

        fun apkFile(context: Context, version: stelogy.android.store.Version): File {
            return File(context.filesDir, stelogy.android.store.ApkFileProvider.Companion.apkFilename(version)).apply {
                if (exists()) {
                    setLastModified(System.currentTimeMillis())
                }
            }
        }

        fun delete(context: Context, version: stelogy.android.store.Version, action: (() -> Unit)? = null) {
            AsyncTask.execute {
                stelogy.android.store.ApkFileProvider.Companion.tempApkFile(context, version).delete()
                stelogy.android.store.ApkFileProvider.Companion.apkFile(context, version).delete()
                action?.let { Handler(Looper.getMainLooper()).post(it) }
            }
        }

        fun invalidate(context: Context) {
            val filter = FileFilter { it.endsWith(stelogy.android.store.ApkFileProvider.Companion.FILE_EXTENSION_APK) }
            AsyncTask.execute { stelogy.android.store.ApkFileProvider.Companion.delete(context, filter) }
        }

        fun cleanUp(context: Context) {
            val time = Date().time
            val month = TimeUnit.DAYS.toMillis(30)
            val filter = FileFilter { it.endsWith(stelogy.android.store.ApkFileProvider.Companion.FILE_EXTENSION_APK) && time - it.lastModified() > month }
            AsyncTask.execute { stelogy.android.store.ApkFileProvider.Companion.delete(context, filter) }
        }

        private fun delete(context: Context, fileFilter: FileFilter?) {
            stelogy.android.store.ApkFileProvider.Companion.deleteFiles(context.cacheDir, fileFilter)
            stelogy.android.store.ApkFileProvider.Companion.deleteFiles(context.filesDir, fileFilter)
        }

        private fun deleteFiles(dir: File, filter: FileFilter? = null) {
            for (file in dir.listFiles(filter) ?: return) {
                if (!file.isDirectory) {
                    if (!file.delete()) {
                        Log.w(stelogy.android.store.ApkFileProvider.Companion.TAG, "Failed to delete file: $file")
                    }
                }
            }
        }

        fun shareIntent(context: Context, application: stelogy.android.store.Application, version: stelogy.android.store.Version): Intent {
            return Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_SUBJECT, "${application.name} ${version.name}")
                putExtra(Intent.EXTRA_STREAM, stelogy.android.store.ApkFileProvider.Companion.uri(stelogy.android.store.ApkFileProvider.Companion.apkFile(context, version), context))
                type = stelogy.android.store.ApkFileProvider.Companion.MIME_TYPE_APK
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
        }
    }

}
