package com.alpaca.app.ui.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alpaca.app.BuildConfig
import com.alpaca.app.data.update.UpdateClient
import com.alpaca.app.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UpdateViewModel(private val container: AppContainer) : ViewModel() {

    sealed interface UpdateState {
        data object Idle : UpdateState
        data object Checking : UpdateState
        data class Available(val release: UpdateClient.ReleaseInfo) : UpdateState
        data object UpToDate : UpdateState
        data class Downloading(val release: UpdateClient.ReleaseInfo, val percent: Int) : UpdateState
        data class ReadyToInstall(val release: UpdateClient.ReleaseInfo) : UpdateState
        data class Failed(val message: String, val release: UpdateClient.ReleaseInfo?) : UpdateState
    }

    private val client = container.updateClient

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    init {
        registerDownloadReceiver()
        check(manual = false)
    }

    /** Auto-checks at most every 6 hours; [manual] bypasses the throttle. */
    fun check(manual: Boolean) {
        if (_state.value is UpdateState.Downloading) return
        viewModelScope.launch {
            val prefs = container.prefs.prefs.first()
            val stale = System.currentTimeMillis() - prefs.lastUpdateCheck > CHECK_INTERVAL_MS
            if (!manual && !stale) return@launch

            _state.value = UpdateState.Checking
            container.prefs.setLastUpdateCheck(System.currentTimeMillis())
            val result = client.fetchLatestRelease()
            _state.value = result.fold(
                onSuccess = { release ->
                    if (release.apkAsset == null ||
                        !UpdateClient.isNewerVersion(release.tagName, BuildConfig.VERSION_NAME)
                    ) {
                        UpdateState.UpToDate
                    } else {
                        UpdateState.Available(release)
                    }
                },
                onFailure = { UpdateState.Failed(it.message ?: "Update check failed", null) }
            )
        }
    }

    fun download(context: Context) {
        val release = when (val current = _state.value) {
            is UpdateState.Available -> current.release
            is UpdateState.Failed -> current.release ?: return
            else -> return
        }
        val asset = release.apkAsset ?: return
        val appContext = context.applicationContext

        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(asset.browserDownloadUrl))
            .setTitle("Alpaca ${release.tagName.removePrefix("v")}")
            .setDescription("Downloading the new version")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                appContext, Environment.DIRECTORY_DOWNLOADS, "alpaca-${release.tagName}.apk"
            )
        downloadId = manager.enqueue(request)
        _state.value = UpdateState.Downloading(release, 0)
        trackProgress(appContext)
    }

    private fun trackProgress(appContext: Context) {
        viewModelScope.launch {
            val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (_state.value is UpdateState.Downloading) {
                val release = (_state.value as UpdateState.Downloading).release
                val percent = queryRunningPercent(manager, downloadId)
                if (percent >= 0) {
                    _state.value = UpdateState.Downloading(release, percent)
                }
                delay(800)
            }
        }
    }

    private fun queryRunningPercent(manager: DownloadManager, id: Long): Int {
        if (id < 0) return -1
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return -1
            if (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) !=
                DownloadManager.STATUS_RUNNING
            ) return -1
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            if (total <= 0) return -1
            return ((done * 100) / total).toInt().coerceIn(0, 100)
        }
    }

    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                val current = _state.value
                val release = (current as? UpdateState.Downloading)?.release ?: return
                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val status = finalStatus(manager, id)
                _state.value = if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    UpdateState.ReadyToInstall(release)
                } else {
                    UpdateState.Failed("Download failed — check your connection and try again.", release)
                }
            }
        }
        downloadReceiver = receiver
        ContextCompat.registerReceiver(
            container.appContext,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun finalStatus(manager: DownloadManager, id: Long): Int {
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return -1
            return cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }
    }

    /** Opens the downloaded APK; Android handles the "install unknown apps" consent. */
    fun install(context: Context) {
        if (downloadId < 0) return
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = manager.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            (_state.value as? UpdateState.ReadyToInstall)?.let {
                _state.value = UpdateState.Failed(
                    "The downloaded file is gone — try the download again.", it.release
                )
            }
            return
        }
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(installIntent)
    }

    override fun onCleared() {
        downloadReceiver?.let {
            try {
                container.appContext.unregisterReceiver(it)
            } catch (_: IllegalArgumentException) {
                // already unregistered
            }
        }
        downloadReceiver = null
        super.onCleared()
    }

    companion object {
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }
}
