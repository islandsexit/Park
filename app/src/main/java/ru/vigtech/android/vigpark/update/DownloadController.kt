package ru.vigtech.android.vigpark.update

import android.R.attr.bitmap
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import ru.vigtech.android.vigpark.BuildConfig
import java.io.File


class DownloadController(private val context: Context, private val url: String) {

    companion object {
        private const val FILE_NAME = "VigPark.apk"
        private const val FILE_BASE_PATH = "file://"
        private const val MIME_TYPE = "application/vnd.android.package-archive"
        private const val PROVIDER_PATH = ".provider"
        private const val APP_INSTALL_PATH = "\"application/vnd.android.package-archive\""
        private var downloadStarted = false
    }

    @SuppressLint("Range", "HandlerLeak")
    fun enqueueDownload() {
        if(!downloadStarted){
            downloadStarted = true
            var destination =
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
            destination += FILE_NAME

            val uri = Uri.parse("$FILE_BASE_PATH$destination")

            val file = File(destination)
            if (file.exists()) file.delete()

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadUri = Uri.parse(url)
            val request = DownloadManager.Request(downloadUri)
//        request.setMimeType(MIME_TYPE)
            request.setTitle("Загрузка")
            request.setDescription("Загрузка обновления VigPark")
            request.setVisibleInDownloadsUi(true)

            request.setDestinationUri(uri)

            val handler: Handler = object : Handler() {

                override fun handleMessage(msg: Message) {
                    if (msg.what == 1) {
                        dialogInstallSuccess(destination, uri)
                    }
                    else{
                        dialogInstallError("Ошибка сети")
                    }
                    super.handleMessage(msg)
                }
            }
//        request.setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS, FILE_NAME)

            val downloadId = downloadManager.enqueue(request)


            Thread {
                var downloading = true
                while (downloading) {

                    val q = DownloadManager.Query()
                    q.setFilterById(downloadId)
                    val cursor: Cursor = downloadManager.query(q)
                    cursor.moveToFirst()
                    val bytes_downloaded: Int = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytes_total: Int =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                    when(status){
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val msg = handler.obtainMessage()
                            msg.what = 1
                            handler.sendMessage(msg)
                            showInstallOption(destination, uri)
                            downloading = false

                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            downloadStarted = false
                            val msg = handler.obtainMessage()
                            msg.what = 2
                            handler.sendMessage(msg)
}

                        DownloadManager.STATUS_PAUSED -> {
                            downloading = false
                            downloadStarted=false
                            val msg = handler.obtainMessage()
                            msg.what = 2
                            handler.sendMessage(msg)
                                }
                    }

                    val dl_progress =
                        if (bytes_total > 0) (bytes_downloaded * 100L / bytes_total).toInt() else 0
                    Log.e("Downloading", dl_progress.toString())
                    val summary = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)
                    )
                    val mimeType = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)
                    )



                    statusMessage(cursor)?.let { Log.d("APP_TAG", "$it, $summary, $mimeType, $bytes_total, $bytes_downloaded") }
                    cursor.close()

                }
                downloadStarted = false
            }.start()
        }


    }

    @SuppressLint("Range")
    private fun statusMessage(c: Cursor): String? {
        var msg = "???"
        msg =
            when (c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_FAILED -> "Download failed!"
                DownloadManager.STATUS_PAUSED -> "Download paused!"
                DownloadManager.STATUS_PENDING -> "Download pending!"
                DownloadManager.STATUS_RUNNING -> "Download in progress!"
                DownloadManager.STATUS_SUCCESSFUL -> "Download complete!"
                else -> "Download is nowhere in sight"
            }
        return msg
    }

    private fun showInstallOption(
        destination: String,
        uri: Uri
    ) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                            File(destination)
                        )
                        val install = Intent(Intent.ACTION_VIEW)
                        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                        install.data = contentUri
                        context.startActivity(install)


                    } else {
                        val install = Intent(Intent.ACTION_VIEW)
                        install.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        install.setDataAndType(
                            uri,
                            APP_INSTALL_PATH
                        )
                        context.startActivity(install)

                        // finish()
                    }

    }

    private fun dialogInstallError(msg: String){
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Доступно обновление")
            .setMessage("Произошла ошибка во время загрузки.\nНе выключайте устройство\n$msg")
            .setCancelable(false)
            .setPositiveButton("Скачать заново") { dialog, whichButton ->
                this.enqueueDownload()
            }
        dialog.show()
    }

    private fun dialogInstallSuccess(destination: String, uri: Uri){

        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Доступно обновление")
            .setMessage("Установите обновление\nНе выключайте устройство\n")
            .setCancelable(false)
            .setPositiveButton("Установить") { dialog, whichButton ->
                dialogInstallSuccess(destination, uri)
                showInstallOption(destination, uri)
            }
        dialog.show()
    }


    }
