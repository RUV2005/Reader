package com.danmo.reader.common.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 触感反馈工具类
 * 为视障用户提供物理交互确认
 */
object HapticUtils {

    /**
     * 触发轻微震动 (Tick)
     * 用于普通按钮点击、切换 Tab 等
     */
    fun triggerTick(context: Context) {
        vibrate(context, 10)
    }

    /**
     * 触发明显震动 (Impact)
     * 用于识别成功、文件打开成功等关键节点
     */
    fun triggerImpact(context: Context) {
        vibrate(context, 30)
    }

    /**
     * 触发双次震动 (Success)
     * 用于 OCR 完成等喜悦时刻
     */
    fun triggerSuccess(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 100, 20), -1))
        } else {
            vibrator.vibrate(20)
        }
    }

    private fun vibrate(context: Context, duration: Long) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duration)
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
