package me.spica27.spicamusic.crash

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.App
import me.spica27.spicamusic.db.entity.Crash
import me.spica27.spicamusic.ui.crash.CrashActivity
import me.spica27.spicamusic.utils.ToastUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CrashHandler : Thread.UncaughtExceptionHandler {
    const val EXTRA_KEY = "CRASH_MODEL"
    private lateinit var mContext: Application
    private lateinit var packageManager: PackageManager
    private val dateFormatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.CHINA)

    fun init(context: Application) {
        mContext = context
        packageManager = context.packageManager
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(
        t: Thread,
        e: Throwable,
    ) {
        if (!this::mContext.isInitialized) return

        e.printStackTrace()

        try {
            handleException(e)
        } catch (exception: Exception) {
            handleException(exception)
        }

        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun handleException(e: Throwable) {
        Intent(App.getInstance(), CrashActivity::class.java).run {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(CrashActivity.ARG_CLASH_MODEL, e.toCrashEntity())
            App.getInstance().startActivity(this)
        }
    }

    fun Throwable.toCrashEntity(): Crash {
        val causeClass = this.cause ?: this

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        causeClass.printStackTrace(pw)
        pw.flush()

        val stackTrackElement =
            stackTrace.firstOrNull { it.className.contains(mContext.packageName) }
                ?: stackTrace.getOrNull(0)

        val buildVersion =
            kotlin
                .runCatching {
                    packageManager
                        .getPackageInfo(mContext.packageName, 0)
                        .let { "[${it.versionCode}:${it.versionName}]" }
                }.getOrNull()

        val deviceInfo =
            "[${Build.MODEL} : ${Build.BRAND} : ${Build.DEVICE}]\n" +
                "[API${Build.VERSION.SDK_INT} : ${Build.VERSION.RELEASE}]\n" +
                "[${Build.CPU_ABI}]"

        return Crash(
            title = causeClass.javaClass.name,
            message = causeClass.message ?: "No Message.",
            causeClass = stackTrackElement?.className ?: "",
            causeFile = stackTrackElement?.fileName ?: "",
            causeMethod = stackTrackElement?.methodName ?: "",
            causeLine = stackTrackElement?.lineNumber.toString(),
            stackTrace = sw.toString().take(4000),
            deviceInfo = deviceInfo,
            buildVersion = buildVersion ?: "",
        )
    }

    private fun getFileUri(
        context: Context,
        file: File,
    ): Uri? =
        FileProvider
            .getUriForFile(context, context.applicationContext.packageName + ".fileprovider", file)

    private fun createZipFile(
        files: List<File>,
        outputZipFile: File,
    ): File {
        ZipOutputStream(FileOutputStream(outputZipFile)).use { zipOut ->
            files.forEach { file ->
                FileInputStream(file).use { fis ->
                    val zipEntry = ZipEntry(file.name)
                    zipOut.putNextEntry(zipEntry)
                    fis.copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
        return outputZipFile
    }

    suspend fun shareLog(
        activity: Activity,
        crash: Crash? = null,
    ) = withContext(Dispatchers.IO) {
        val cacheDir = activity.cacheDir
        val tempDirectory = File("$cacheDir/crash_temp")
        if (tempDirectory.exists()) tempDirectory.deleteRecursively()
        if (!tempDirectory.exists()) tempDirectory.mkdirs()

        val timeStr = dateFormatter.format(Date(System.currentTimeMillis()))
        val logDirectory = File("$cacheDir/log")
        logDirectory.mkdirs()
        val infoFile = File(tempDirectory, "crash_info.json")
        val zipFile = File(tempDirectory, "share_log_$timeStr.zip")
        val zipFileUri = getFileUri(activity, zipFile)

        if (!logDirectory.exists() || !logDirectory.isDirectory || !logDirectory.canRead() || zipFileUri == null) {
            withContext(Dispatchers.Main) { ToastUtils.showToast("日志文件不存在或无法读取") }
            return@withContext
        }

        if (!infoFile.exists()) infoFile.createNewFile()

        val moshi = Moshi.Builder().build()

        val adapter = moshi.adapter(Crash::class.java)

        infoFile.writeText(adapter.toJson(crash))

        if (!zipFile.exists()) zipFile.createNewFile()

        createZipFile(
            listOf(infoFile),
            zipFile,
        )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, zipFileUri)
                putExtra(Intent.EXTRA_TITLE, "崩溃日志分享")
            }
        withContext(Dispatchers.Main) {
            activity.startActivity(Intent.createChooser(intent, "分享日志文件"))
        }
    }
}
