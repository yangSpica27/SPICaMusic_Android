package me.spcia.lyric_core.parser

import timber.log.Timber

/**
 * YRC 格式歌词解析器
 * 每个字符都有独立的时间戳，支持卡拉OK效果
 */
object YrcParser {
    
    /**
     * 解析 YRC 格式歌词
     * 
     * YRC 格式示例:
     * [0,268](0,23,0)作(23,23,0)词(46,11,0)：(57,346,0)A(403,11,0)-(414,450,0)L(864,11,0)I
     * 
     * @param yrcContent YRC 格式字符串
     * @return 解析后的歌词行列表
     */
    fun parse(yrcContent: String): List<YrcLine> {
        if (yrcContent.isBlank()) {
            return emptyList()
        }
        
        val lines = mutableListOf<YrcLine>()
        
        try {
            yrcContent.lines().forEach { line ->
                if (line.startsWith("[") && line.contains("](")) {
                    parseLine(line)?.let { lines.add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.tag("YrcParser").e(e, "YRC解析失败")
        }
        
        return lines
    }
    
    /**
     * 解析单行 YRC 歌词
     * 
     * 格式: [行开始时间,行持续时间](字1开始,字1持续,字1空格)文1(字2...)文2...
     */
    private fun parseLine(line: String): YrcLine? {
        try {
            // 提取行时间信息 [startTime,duration]
            val lineTimeEnd = line.indexOf(']')
            if (lineTimeEnd == -1) return null
            
            val lineTimeStr = line.substring(1, lineTimeEnd)
            val lineTimes = lineTimeStr.split(",")
            if (lineTimes.size < 2) return null
            
            val lineStartTime = lineTimes[0].toLongOrNull() ?: return null
            val lineDuration = lineTimes[1].toLongOrNull() ?: return null
            
            // 提取字符信息
            val contentStart = line.indexOf("](") + 2
            if (contentStart < 2) return null
            
            val words = mutableListOf<YrcWord>()
            var currentIndex = contentStart
            var currentTime = 0L
            
            while (currentIndex < line.length) {
                // 查找下一个字符的时间标记
                if (line[currentIndex] == '(') {
                    val timeEnd = line.indexOf(')', currentIndex)
                    if (timeEnd == -1) break
                    
                    // 提取字符时间 (startOffset,duration,emptySpace)
                    val timeStr = line.substring(currentIndex + 1, timeEnd)
                    val times = timeStr.split(",")
                    if (times.size < 2) break
                    
                    val startOffset = times[0].toLongOrNull() ?: 0
                    val duration = times[1].toLongOrNull() ?: 0
                    
                    // 提取字符文本
                    val textStart = timeEnd + 1
                    var textEnd = textStart
                    while (textEnd < line.length && line[textEnd] != '(') {
                        textEnd++
                    }
                    
                    if (textStart < line.length) {
                        val text = line.substring(textStart, textEnd)
                        if (text.isNotEmpty()) {
                            words.add(
                                YrcWord(
                                    text = text,
                                    startTime = lineStartTime + startOffset,
                                    duration = duration
                                )
                            )
                        }
                    }
                    
                    currentIndex = textEnd
                } else {
                    currentIndex++
                }
            }
            
            return if (words.isNotEmpty()) {
                YrcLine(
                    startTime = lineStartTime,
                    duration = lineDuration,
                    words = words
                )
            } else {
                null
            }
            
        } catch (e: Exception) {
            Timber.tag("YrcParser").w(e, "解析行失败: $line")
            return null
        }
    }
    
    /**
     * YRC 歌词行
     */
    data class YrcLine(
        val startTime: Long,      // 行开始时间（毫秒）
        val duration: Long,       // 行持续时间（毫秒）
        val words: List<YrcWord>  // 字符列表
    ) {
        val endTime: Long get() = startTime + duration
        
        val text: String get() = words.joinToString("") { it.text }
        
        /**
         * 获取指定时间点应该高亮的字符索引
         * @param currentTime 当前播放时间（毫秒）
         * @return 高亮字符的索引，-1 表示还未开始，words.size 表示已全部唱完
         */
        fun getHighlightIndex(currentTime: Long): Int {
            if (currentTime < startTime) return -1
            if (currentTime >= endTime) return words.size
            
            words.forEachIndexed { index, word ->
                if (currentTime < word.endTime) {
                    return index
                }
            }
            
            return words.size
        }
        
        /**
         * 获取指定时间点当前字符的进度（0.0 ~ 1.0）
         */
        fun getWordProgress(currentTime: Long, wordIndex: Int): Float {
            if (wordIndex < 0 || wordIndex >= words.size) return 0f
            
            val word = words[wordIndex]
            if (currentTime < word.startTime) return 0f
            if (currentTime >= word.endTime) return 1f
            
            val elapsed = currentTime - word.startTime
            return (elapsed.toFloat() / word.duration).coerceIn(0f, 1f)
        }
    }
    
    /**
     * YRC 歌词字符
     */
    data class YrcWord(
        val text: String,         // 字符文本
        val startTime: Long,      // 字符开始时间（毫秒）
        val duration: Long        // 字符持续时间（毫秒）
    ) {
        val endTime: Long get() = startTime + duration
    }
    
    /**
     * 转换为标准 LRC 格式（仅时间戳+整行文本）
     */
    fun toLrc(yrcLines: List<YrcLine>): String {
        return buildString {
            yrcLines.forEach { line ->
                val minutes = line.startTime / 60000
                val seconds = (line.startTime % 60000) / 1000
                val milliseconds = (line.startTime % 1000) / 10
                appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, milliseconds, line.text))
            }
        }
    }
}
