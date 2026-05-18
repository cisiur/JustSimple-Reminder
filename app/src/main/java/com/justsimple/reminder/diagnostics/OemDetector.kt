package com.justsimple.reminder.diagnostics

import android.os.Build

enum class OemBrand {
    XIAOMI, SAMSUNG, ONEPLUS, HUAWEI, OPPO, VIVO, REALME, OTHER
}

object OemDetector {
    fun detect(): OemBrand {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
            brand.contains("redmi") || brand.contains("poco") -> OemBrand.XIAOMI
            manufacturer.contains("samsung") -> OemBrand.SAMSUNG
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> OemBrand.ONEPLUS
            manufacturer.contains("huawei") || brand.contains("huawei") -> OemBrand.HUAWEI
            manufacturer.contains("oppo") -> OemBrand.OPPO
            manufacturer.contains("vivo") -> OemBrand.VIVO
            manufacturer.contains("realme") -> OemBrand.REALME
            else -> OemBrand.OTHER
        }
    }

    fun isXiaomiBased(): Boolean = detect() == OemBrand.XIAOMI
}

