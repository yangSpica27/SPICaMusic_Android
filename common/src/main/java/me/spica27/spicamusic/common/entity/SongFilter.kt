package me.spica27.spicamusic.common.entity

/**
 * 歌曲筛选条件
 * @param keyword 搜索关键词（匹配歌名或艺术家）
 * @param minDuration 最小时长（毫秒）
 * @param maxDuration 最大时长（毫秒）
 * @param minSize 最小文件大小（字节）
 * @param maxSize 最大文件大小（字节）
 * @param artists 指定的艺术家列表（为空则不筛选）
 * @param albums 指定的专辑列表（为空则不筛选）
 * @param mimeTypes 指定的MIME类型列表（为空则不筛选）
 * @param onlyLiked 仅显示喜欢的歌曲
 * @param excludeIgnored 排除已忽略的歌曲（默认true）
 */
data class SongFilter(
    val keyword: String? = null,
    val minDuration: Long? = null,
    val maxDuration: Long? = null,
    val minSize: Long? = null,
    val maxSize: Long? = null,
    val artists: List<String>? = null,
    val albums: List<Long>? = null,
    val mimeTypes: List<String>? = null,
    val onlyLiked: Boolean = false,
    val excludeIgnored: Boolean = true
) {
    companion object {
        /** 空筛选条件 */
        val EMPTY = SongFilter()
    }
    
    /** 是否为空筛选条件 */
    fun isEmpty(): Boolean {
        return keyword.isNullOrEmpty() &&
               minDuration == null &&
               maxDuration == null &&
               minSize == null &&
               maxSize == null &&
               artists.isNullOrEmpty() &&
               albums.isNullOrEmpty() &&
               mimeTypes.isNullOrEmpty() &&
               !onlyLiked &&
               excludeIgnored
    }
}
