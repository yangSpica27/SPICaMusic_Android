@file:Suppress("unused")

package me.spica27.spicamusic.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.util.*


val Context.contentResolverSafe: ContentResolver
  get() = applicationContext.contentResolver

/**
 * 查询 [ContentResolver] 数据库封装
 */
fun ContentResolver.safeQuery(
  uri: Uri,
  projection: Array<out String>,
  selector: String? = null,
  args: Array<String>? = null
) = requireNotNull(query(uri, projection, selector, args, null)) { "ContentResolver query failed" }


/**
 * 查询 [ContentResolver] 数据库封装
 * 预留回调清理游标
 */
inline fun <reified R> ContentResolver.useQuery(
  uri: Uri,
  projection: Array<out String>,
  selector: String? = null,
  args: Array<String>? = null,
  block: (Cursor) -> R
) = safeQuery(uri, projection, selector, args).use(block)

/** 自定义的 [MediaStore] 数据库 */
private val EXTERNAL_COVERS_URI = Uri.parse("content://media/external/audio/albumart")

/**
 * 将[MediaStore]歌曲ID转换为其音频文件的[Uri]
 * 返回可能为空
 */
fun Long.toAudioUri() =
  ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this)

/**
 * 根据从[MediaStore]乐曲的id获取封面
 * 返回可能为空
 */
fun Long.toCoverUri() = ContentUris.withAppendedId(EXTERNAL_COVERS_URI, this)

//
///**
// * 效果等同 StorageManager.getStorageVolumes
// * API 21 到 API 23 改方法被私有化，所以需要反射
// * @see StorageManager.getStorageVolumes
// */
//@Suppress("NewApi")
//private val SM_API21_GET_VOLUME_LIST_METHOD: Method by
//lazyReflectedMethod(StorageManager::class, "getVolumeList")
//
///**
// * 等同[StorageVolume.getDirectory]
// * API 23后被私有了 所以使用反射调用
// */
//@Suppress("NewApi")
//private val SV_API21_GET_PATH_METHOD: Method by lazyReflectedMethod(StorageVolume::class, "getPath")


/**
 * 将可用的主要共享/外部存储卷返回给当前用户。
 */
val StorageManager.primaryStorageVolumeCompat: StorageVolume
  @Suppress("NewApi") get() = primaryStorageVolume

/**
 * 将可用的主要共享/外部存储卷返回给当前用户。
 */
val StorageManager.storageVolumesCompat: List<StorageVolume>
  get() =
    storageVolumes.toList()

///**
// * 系统[StorageVolume]的路径
// */
//val StorageVolume.directoryCompat: String?
//    @SuppressLint("NewApi")
//    get() =
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            directory?.absolutePath
//        } else {
//            // Replicate API: Analogous method if mounted, null if not
//            when (stateCompat) {
//                Environment.MEDIA_MOUNTED,
//                Environment.MEDIA_MOUNTED_READ_ONLY ->
//                    SV_API21_GET_PATH_METHOD.invoke(this) as String
//                else -> null
//            }
//        }

/**
 * 获取卷的描述 如 【内部共享储存】
 */
@SuppressLint("NewApi")
fun StorageVolume.getDescriptionCompat(context: Context): String = getDescription(context)

/**
 * 检查 [StorageVolume] 是否是主要存储
 */
val StorageVolume.isPrimaryCompat: Boolean
  @SuppressLint("NewApi") get() = isPrimary

/**
 * 是否是模拟存储
 */
val StorageVolume.isEmulatedCompat: Boolean
  @SuppressLint("NewApi") get() = isEmulated

/**
 * 是否主要卷 是的化用兼容方式获取uri
 */
val StorageVolume.isInternalCompat: Boolean
  get() = isPrimaryCompat && isEmulatedCompat


val StorageVolume.uuidCompat: String?
  @SuppressLint("NewApi") get() = uuid

/**
 * 获取 [StorageVolume]状态
 */
val StorageVolume.stateCompat: String
  @SuppressLint("NewApi") get() = state


fun Context.uriToFile(uri: Uri) = with(contentResolverSafe) {
  val extension = getUriExtension(uri)
  val file = File(
    cacheDir.path,
    "${UUID.randomUUID()}.$extension"
  )

  try {
    val inputStream = openInputStream(uri) ?: return@with file
    file.writeBytes(inputStream.buffered().readBytes())
    inputStream.close()
  } catch (e: java.lang.Exception) {
    e.printStackTrace()
  }

  return@with file
}


fun ContentResolver.getUriExtension(uri: Uri) = MimeTypeMap.getSingleton()
  .getMimeTypeFromExtension(getType(uri))

val StorageVolume.mediaStoreVolumeNameCompat: String?
  get() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      mediaStoreVolumeName
    } else {
      if (isPrimaryCompat) {
        @Suppress("NewApi") MediaStore.VOLUME_EXTERNAL_PRIMARY
      } else {
        uuidCompat?.lowercase()
      }
    }
