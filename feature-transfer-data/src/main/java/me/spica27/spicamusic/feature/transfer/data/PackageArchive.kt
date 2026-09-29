package me.spica27.spicamusic.feature.transfer.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.encodeToString
import me.spica27.spicamusic.feature.transfer.domain.LibraryPackage
import me.spica27.spicamusic.feature.transfer.domain.PackageAsset
import me.spica27.spicamusic.feature.transfer.domain.PackageJson
import me.spica27.spicamusic.feature.transfer.domain.PackageManifest
import me.spica27.spicamusic.feature.transfer.domain.PackagePlaylist
import me.spica27.spicamusic.feature.transfer.domain.PackageTrack
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** 流式读写 ZIP，不按包内路径解压文件。 */
class PackageArchive {
    suspend fun write(
        library: LibraryPackage,
        output: OutputStream,
        openAsset: (String) -> InputStream,
        progress: (Long, Long) -> Unit = { _, _ -> },
    ) {
        library.validate()
        val manifestBytes = PackageJson.encodeToString(library.manifest).toByteArray(Charsets.UTF_8)
        require(manifestBytes.size <= LibraryPackage.MAX_METADATA_BYTES) { "完整包目录过大" }
        val total = library.manifest.assets.sumOf { it.size }
        var written = 0L
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifestBytes)
            zip.closeEntry()
            library.manifest.assets.forEach { asset ->
                zip.setLevel(if (asset.kind == "audio") 0 else 6)
                zip.putNextEntry(ZipEntry(asset.path))
                openAsset(asset.path).use { input ->
                    val actual = copyAndHash(input, zip, asset.size) { bytes -> progress(written + bytes, total) }
                    require(actual.first == asset.size && actual.second == asset.sha256) { "资源在导出过程中发生变化：${asset.path}" }
                }
                zip.closeEntry()
                written += asset.size
            }
        }
    }

    suspend fun inspect(
        file: File,
        progress: (Long, Long) -> Unit = { _, _ -> },
    ): LibraryPackage =
        ZipFile(file).use { zip ->
            val names = HashSet<String>()
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                require(!entry.isDirectory && names.add(entry.name) && names.size <= 300_003) { "完整包包含重复或无效条目" }
            }
            val manifest = PackageJson.decodeFromString<PackageManifest>(readSmall(zip, "manifest.json", LibraryPackage.MAX_METADATA_BYTES))
            require(manifest.version == 1 && manifest.format == "spica-library") { "不支持的完整包格式或版本" }
            require(
                manifest.assets.size <= 300_002 &&
                    manifest.assets
                        .map { it.path }
                        .toSet()
                        .size == manifest.assets.size,
            ) { "资源清单无效" }
            require(names == manifest.assets.map { it.path }.toSet() + "manifest.json") { "完整包资源缺失或包含未声明的文件" }
            // 先检查资源大小，再解压校验资源或读取目录。
            manifest.assets.forEach { asset ->
                val limit =
                    when (asset.kind) {
                        "audio" -> LibraryPackage.MAX_AUDIO_BYTES
                        "lyrics" -> LibraryPackage.MAX_LYRIC_BYTES
                        "artwork", "metadata" -> LibraryPackage.MAX_METADATA_BYTES
                        else -> error("不支持的资源类型")
                    }
                require(asset.size in 0..limit) { "资源大小无效" }
            }
            val total = manifest.assets.sumOf { it.size }
            var checked = 0L
            manifest.assets.forEach { asset ->
                val entry = zip.getEntry(asset.path) ?: error("缺少资源：${asset.path}")
                require(entry.size == asset.size) { "资源大小不匹配：${asset.path}" }
                zip.getInputStream(entry).use { input ->
                    val result = copyAndHash(input, null, asset.size) { bytes -> progress(checked + bytes, total) }
                    require(result.first == asset.size && result.second == asset.sha256) { "资源校验失败：${asset.path}" }
                }
                checked += asset.size
            }
            LibraryPackage(
                manifest,
                PackageJson.decodeFromString<List<PackageTrack>>(readSmall(zip, "tracks.json", LibraryPackage.MAX_METADATA_BYTES)),
                PackageJson.decodeFromString<List<PackagePlaylist>>(readSmall(zip, "playlists.json", LibraryPackage.MAX_METADATA_BYTES)),
            ).also { it.validate() }
        }

    companion object {
        fun digest(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

        fun asset(
            path: String,
            bytes: ByteArray,
            kind: String,
        ) = PackageAsset(path, digest(bytes), bytes.size.toLong(), kind)

        suspend fun copyAndHash(
            input: InputStream,
            output: OutputStream?,
            limit: Long,
            progress: (Long) -> Unit = {},
        ): Pair<Long, String> {
            val buffer = ByteArray(128 * 1024)
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                size += count
                require(size <= limit) { "文件超过声明的大小" }
                digest.update(buffer, 0, count)
                output?.write(buffer, 0, count)
                progress(size)
            }
            return size to digest.digest().toHex()
        }

        fun readSmall(
            zip: ZipFile,
            path: String,
            limit: Long,
        ): String {
            val entry = zip.getEntry(path) ?: error("完整包缺少 $path")
            require(entry.size in 0..limit) { "资源过大：$path" }
            return zip.getInputStream(entry).use { input ->
                val bytes = input.readBounded(entry.size)
                require(bytes.size.toLong() == entry.size) { "资源大小不匹配：$path" }
                bytes.decodeToString(throwOnInvalidSequence = true)
            }
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    }
}
