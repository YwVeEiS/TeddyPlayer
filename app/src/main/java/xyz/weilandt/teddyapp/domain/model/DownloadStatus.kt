package xyz.weilandt.teddyapp.domain.model

sealed interface DownloadStatus {
    data object None : DownloadStatus
    data object Queued : DownloadStatus

    /** @param progress 0..1 oder `null`, wenn die Gesamtgröße noch unbekannt ist */
    data class Downloading(val progress: Float?) : DownloadStatus
    data object Completed : DownloadStatus
    data object Failed : DownloadStatus
}

data class DownloadInfo(
    val tonieId: String,
    val status: DownloadStatus,
    val bytesDownloaded: Long,
)
