package me.spica27.spicamusic.common.entity

/**
 * 音质类型分级
 * 按音质从低到高 + 环绕声类型分类
 */
enum class QualityType {
    // 有损压缩 - 按比特率分级
    LOSSY_LOW,              // <128 kbps (MP3, AAC, OGG)
    LOSSY_STANDARD,         // 128-192 kbps
    LOSSY_HIGH,             // 192-320 kbps
    
    // 无损 - CD 品质
    CD_QUALITY_LOSSLESS,    // FLAC/ALAC 16bit/44.1kHz
    
    // 高解析度无损
    HI_RES_LOSSLESS,        // FLAC/ALAC 24bit/48-96kHz
    HI_RES_STUDIO_MASTER,   // FLAC/ALAC 24bit/96kHz+
    
    // 环绕声 - 有损（至少3声道）
    DOLBY_DIGITAL,          // AC-3, E-AC-3 (Dolby Digital/Plus)
    DTS_SURROUND,           // DTS, DTS-ES (有损环绕声)
    
    // 环绕声 - 无损
    DOLBY_LOSSLESS,         // TrueHD, Atmos
    DTS_HD_MA,              // DTS-HD Master Audio (无损)
    FLAC_SURROUND,          // FLAC 多声道 (>2 channels)
    
    UNKNOWN                 // 未知或不支持
}

/**
 * 获取歌曲的音质类型
 * 优化后的识别逻辑：
 * 1. 环绕声必须 channels > 2
 * 2. Hi-Res 必须考虑位深度
 * 3. 有损压缩按比特率分级
 * 4. 区分 DTS 有损/无损变体
 */
fun Song.getQualityType(): QualityType {
    val format = this.codec.lowercase()
    val bitrate = this.bitRate
    val sampleRate = this.sampleRate
    val channels = this.channels
    val bitDepth = this.digit
    
    // 环绕声格式（channels > 2）
    if (channels > 2) {
        return when (format) {
            "ac3", "eac3" -> QualityType.DOLBY_DIGITAL
            "truehd" -> QualityType.DOLBY_LOSSLESS
            "dts" -> {
                // DTS 系列识别
                when {
                    // DTS-HD MA 通常比特率很高或有特定标记
                    bitrate > 4_000_000 -> QualityType.DTS_HD_MA
                    else -> QualityType.DTS_SURROUND
                }
            }
            "flac", "alac" -> QualityType.FLAC_SURROUND
            else -> QualityType.UNKNOWN
        }
    }
    
    // 立体声或单声道格式
    return when (format) {
        // 有损压缩格式 - 按比特率分级
        "mp3", "aac", "ogg", "opus", "wma" -> {
            when {
                bitrate < 128_000 -> QualityType.LOSSY_LOW
                bitrate < 192_000 -> QualityType.LOSSY_STANDARD
                bitrate <= 320_000 -> QualityType.LOSSY_HIGH
                else -> QualityType.UNKNOWN
            }
        }
        
        // 无损格式 - 按位深度和采样率分级
        "flac", "alac", "ape", "wav", "aiff" -> {
            when {
                // CD 品质：16bit/44.1kHz
                bitDepth == 16 && sampleRate == 44100 -> QualityType.CD_QUALITY_LOSSLESS
                
                // Hi-Res：24bit 且采样率 >= 96kHz
                bitDepth >= 24 && sampleRate >= 96000 -> QualityType.HI_RES_STUDIO_MASTER
                
                // Hi-Res：24bit 且采样率 >= 48kHz
                bitDepth >= 24 && sampleRate >= 48000 -> QualityType.HI_RES_LOSSLESS
                
                // 其他无损（可能是 16bit 但非标准采样率）
                else -> QualityType.CD_QUALITY_LOSSLESS
            }
        }
        
        // 环绕声格式但只有2声道（降级处理）
        "ac3", "eac3", "dts", "truehd" -> {
            // 理论上不应该出现2声道的环绕声编码，标记为 UNKNOWN
            QualityType.UNKNOWN
        }
        
        else -> QualityType.UNKNOWN
    }
}

/**
 * 获取音质描述文本
 */
fun QualityType.getDescription(): String {
    return when (this) {
        QualityType.LOSSY_LOW -> "标准音质"
        QualityType.LOSSY_STANDARD -> "高品质"
        QualityType.LOSSY_HIGH -> "超高品质"
        QualityType.CD_QUALITY_LOSSLESS -> "CD 无损"
        QualityType.HI_RES_LOSSLESS -> "Hi-Res 无损"
        QualityType.HI_RES_STUDIO_MASTER -> "母带品质"
        QualityType.DOLBY_DIGITAL -> "杜比环绕声"
        QualityType.DTS_SURROUND -> "DTS 环绕声"
        QualityType.DOLBY_LOSSLESS -> "杜比全景声"
        QualityType.DTS_HD_MA -> "DTS-HD MA"
        QualityType.FLAC_SURROUND -> "多声道无损"
        QualityType.UNKNOWN -> "未知"
    }
}

/**
 * 判断是否为无损音质
 */
fun QualityType.isLossless(): Boolean {
    return when (this) {
        QualityType.CD_QUALITY_LOSSLESS,
        QualityType.HI_RES_LOSSLESS,
        QualityType.HI_RES_STUDIO_MASTER,
        QualityType.DOLBY_LOSSLESS,
        QualityType.DTS_HD_MA,
        QualityType.FLAC_SURROUND -> true
        else -> false
    }
}

/**
 * 判断是否为环绕声
 */
fun QualityType.isSurround(): Boolean {
    return when (this) {
        QualityType.DOLBY_DIGITAL,
        QualityType.DTS_SURROUND,
        QualityType.DOLBY_LOSSLESS,
        QualityType.DTS_HD_MA,
        QualityType.FLAC_SURROUND -> true
        else -> false
    }
}

/**
 * 获取音质级别（用于 UI 显示）
 */
fun QualityType.getQualityLevel(): Int {
    return when (this) {
        // 顶级音质（4）
        QualityType.HI_RES_STUDIO_MASTER,
        QualityType.DOLBY_LOSSLESS,
        QualityType.DTS_HD_MA -> 4
        
        // 优秀音质（3）
        QualityType.HI_RES_LOSSLESS,
        QualityType.FLAC_SURROUND -> 3
        
        // 良好音质（2）
        QualityType.CD_QUALITY_LOSSLESS,
        QualityType.LOSSY_HIGH -> 2
        
        // 标准音质（1）
        QualityType.LOSSY_STANDARD,
        QualityType.DOLBY_DIGITAL,
        QualityType.DTS_SURROUND -> 1
        
        // 低音质（0）
        QualityType.LOSSY_LOW,
        QualityType.UNKNOWN -> 0
    }
}
