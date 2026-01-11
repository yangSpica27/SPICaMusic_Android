package me.spica27.spicamusic.common.entity

/**
 * 歌曲排序方式
 */
enum class SongSortOrder {
    /** 按显示名称升序 */
    DISPLAY_NAME_ASC,
    
    /** 按显示名称降序 */
    DISPLAY_NAME_DESC,
    
    /** 按艺术家升序 */
    ARTIST_ASC,
    
    /** 按艺术家降序 */
    ARTIST_DESC,
    
    /** 按时长升序 */
    DURATION_ASC,
    
    /** 按时长降序 */
    DURATION_DESC,
    
    /** 按文件大小升序 */
    SIZE_ASC,
    
    /** 按文件大小降序 */
    SIZE_DESC,
    
    /** 按添加时间升序 */
    DATE_ADDED_ASC,
    
    /** 按添加时间降序 */
    DATE_ADDED_DESC,
    
    /** 按播放次数降序 */
    PLAY_COUNT_DESC,
    
    /** 随机排序 */
    RANDOM,
    
    /** 默认排序（数据库默认顺序） */
    DEFAULT
}
