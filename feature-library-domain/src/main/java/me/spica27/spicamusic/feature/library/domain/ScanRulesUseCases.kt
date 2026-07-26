package me.spica27.spicamusic.feature.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.storage.api.IScanRulesRepository

/**
 * 扫描规则用例：读取 / 修改扫描规则，供扫描配置 UI 使用。
 * 规则变更会在下次扫描时生效（MusicScanService 每次扫描前都会重新加载）。
 */
class ScanRulesUseCases(
    private val repository: IScanRulesRepository,
) {
    fun getRulesFlow(): Flow<ScanRules> = repository.getRulesFlow().map { it.toDomain() }

    suspend fun setMinDurationSec(seconds: Int) {
        repository.setMinDurationSec(seconds)
    }

    suspend fun setMinFileSizeKb(kb: Int) {
        repository.setMinFileSizeKb(kb)
    }

    suspend fun setEnabledFormats(keys: Set<String>) {
        repository.setEnabledFormats(keys)
    }
}
