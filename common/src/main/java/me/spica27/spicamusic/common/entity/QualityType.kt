package me.spica27.spicamusic.common.entity

enum class QualityType {
    LOSSY_COMPRESSED,      // MP3, AAC, OGG (128-320 kbps)
    CD_QUALITY_LOSSLESS,   // FLAC, ALAC 16/44.1
    HI_RES_LOSSLESS,       // FLAC, ALAC 24/48+ kHz
    HI_RES_STUDIO_MASTER,  // FLAC, ALAC 24/192 kHz
    LOSSLESS_SURROUND,     // e.g., FLAC 5.1
    DOLBY_LOSSY_SURROUND,  // AC-3, E-AC-3
    DOLBY_LOSSLESS,        // TrueHD, Atmos
    DTS_SURROUND,          // DTS variants
    UNKNOWN
}

fun Song.getQualityType(): QualityType {
    val format = this.codec.lowercase()
    val bitrate = this.bitRate
    val sampleRate = this.sampleRate
    val channels = this.channels

    return when (format) {
        "mp3", "aac", "ogg" -> if (bitrate in 128 * 1_000..320 * 1_000) QualityType.LOSSY_COMPRESSED else QualityType.UNKNOWN
        "flac", "alac" -> when {
            sampleRate >= 192000 && channels >= 2 -> QualityType.HI_RES_STUDIO_MASTER
            sampleRate >= 48000 && channels >= 2 -> QualityType.HI_RES_LOSSLESS
            else -> QualityType.CD_QUALITY_LOSSLESS
        }

        "ac3", "eac3" -> QualityType.DOLBY_LOSSY_SURROUND
        "truehd", "atmos" -> QualityType.DOLBY_LOSSLESS
        "dts" -> QualityType.DTS_SURROUND
        else -> QualityType.UNKNOWN
    }
}
