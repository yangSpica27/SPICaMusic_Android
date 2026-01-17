package me.spica27.spicamusic.common.entity

/**
 * 歌曲分组数据类 - 用于按首字母分组展示
 * @param groupKey 分组键（A-Z, #）
 * @param songs 该分组下的歌曲列表
 */
data class SongGroup(
    val groupKey: String,
    val songs: List<Song>
)
