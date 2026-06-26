package com.danmo.reader.common.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * 语言环境管理工具类
 */
object LocaleUtils {

    /**
     * 应用选定的语言设置并返回包装后的 Context
     */
    fun applyLocale(context: Context, languageTag: String): Context {
        if (languageTag == "system") return context

        val locale = when (languageTag) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.SIMPLIFIED_CHINESE
        }

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}
