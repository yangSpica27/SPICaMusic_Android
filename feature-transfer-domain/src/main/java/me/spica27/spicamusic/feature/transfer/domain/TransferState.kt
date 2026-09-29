package me.spica27.spicamusic.feature.transfer.domain

data class ImportPreview(
    val songs: Int,
    val playlists: Int,
    val newSongs: Int,
    val reusedSongs: Int,
    val newPlaylists: Int,
    val lyricConflicts: Int,
    val bytesToCopy: Long,
)

sealed interface TransferState {
    data object Idle : TransferState

    data class Running(
        val message: String,
        val completed: Long = 0,
        val total: Long = 0,
    ) : TransferState

    data class Preview(
        val summary: ImportPreview,
    ) : TransferState

    data class Success(
        val message: String,
    ) : TransferState

    data class Interrupted(
        val message: String,
    ) : TransferState

    data class Failure(
        val message: String,
        val canRetry: Boolean,
    ) : TransferState
}
