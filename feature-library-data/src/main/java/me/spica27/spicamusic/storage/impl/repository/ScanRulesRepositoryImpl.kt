package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.storage.api.IScanRulesRepository
import me.spica27.spicamusic.storage.api.ScanFormat
import me.spica27.spicamusic.storage.api.ScanRules

/**
 * 扫描规则仓库：基于 DataStore 持久化。
 * 数值以字符串存储（沿用 PreferencesManager 的既有习惯），格式集合以逗号分隔。
 */
class ScanRulesRepositoryImpl(
    private val preferencesManager: PreferencesManager,
) : IScanRulesRepository {
    override fun getRulesFlow(): Flow<ScanRules> =
        combine(
            preferencesManager.getString(PreferencesManager.Keys.SCAN_MIN_DURATION_SEC),
            preferencesManager.getString(PreferencesManager.Keys.SCAN_MIN_FILE_SIZE_KB),
            preferencesManager.getString(PreferencesManager.Keys.SCAN_ENABLED_FORMATS),
        ) { minDurationSec, minFileSizeKb, enabledFormats ->
            ScanRules(
                minDurationMs = parseMinDurationSec(minDurationSec) * 1000L,
                minFileSizeBytes = (minFileSizeKb.toLongOrNull()?.coerceAtLeast(0L) ?: 0L) * 1024L,
                enabledFormatKeys = parseFormatKeys(enabledFormats),
            )
        }.distinctUntilChanged()

    override suspend fun getRulesSync(): ScanRules = getRulesFlow().first()

    override suspend fun setMinDurationSec(seconds: Int) {
        preferencesManager.setString(
            PreferencesManager.Keys.SCAN_MIN_DURATION_SEC,
            seconds.coerceAtLeast(0).toString(),
        )
    }

    override suspend fun setMinFileSizeKb(kb: Int) {
        preferencesManager.setString(
            PreferencesManager.Keys.SCAN_MIN_FILE_SIZE_KB,
            kb.coerceAtLeast(0).toString(),
        )
    }

    override suspend fun setEnabledFormats(keys: Set<String>) {
        val sanitized = keys.filter { it in ScanFormat.allKeys }
        preferencesManager.setString(
            PreferencesManager.Keys.SCAN_ENABLED_FORMATS,
            sanitized.joinToString(","),
        )
    }

    private fun parseMinDurationSec(raw: String): Long =
        raw.toLongOrNull()?.coerceAtLeast(0L) ?: ScanRules.DEFAULT_MIN_DURATION_SEC.toLong()

    /** 未设置过 → 全部格式；解析结果为空（异常数据）也回退到全部格式 */
    private fun parseFormatKeys(raw: String): Set<String> {
        if (raw.isBlank()) return ScanFormat.allKeys
        val keys = raw.split(",").map { it.trim() }.filter { it in ScanFormat.allKeys }.toSet()
        return keys.ifEmpty { ScanFormat.allKeys }
    }
}
