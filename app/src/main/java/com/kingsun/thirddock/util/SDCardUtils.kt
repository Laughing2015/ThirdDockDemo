package com.kingsun.thirddock.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.kingsun.thirddock.util.CloseUtils.closeIO
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object SDCardUtils {
    /**
     * 判断SD卡是否可用
     *
     * @return true : 可用<br></br>false : 不可用
     */
    fun isSDCardEnable(): Boolean {
        try {
            return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        } catch (e: RuntimeException) {
        }
        return false
    }

    /**
     * 获取SD卡路径
     *
     * @return SD卡路径
     */
    fun getSDCardRootPath(): String? {
        return if (!isSDCardEnable()) null else Environment.getExternalStorageDirectory().path + File.separator
    }

    /**
     * 获取SD卡路径
     *
     * 先用shell，shell失败再普通方法获取，一般是/storage/emulated/0/
     *
     * @return SD卡路径
     */
    fun getSDCardPath(): String? {
        if (!isSDCardEnable()) return null
        val cmd = "cat /proc/mounts"
        val run = Runtime.getRuntime()
        var bufferedReader: BufferedReader? = null
        try {
            val p = run.exec(cmd)
            bufferedReader = BufferedReader(InputStreamReader(BufferedInputStream(p.inputStream)))
            var lineStr: String
            while (bufferedReader.readLine().also { lineStr = it } != null) {
                if (lineStr.contains("sdcard") && lineStr.contains(".android_secure")) {
                    val strArray = lineStr.split(" ".toRegex()).toTypedArray()
                    if (strArray.size >= 5) {
                        return strArray[1].replace("/.android_secure", "") + File.separator
                    }
                }
                if (p.waitFor() != 0 && p.exitValue() == 1) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            closeIO(bufferedReader)
        }
        return Environment.getExternalStorageDirectory().path + File.separator
    }

    /**
     * 获取SD卡data路径
     *
     * @return SD卡data路径
     */
    fun getDataPath(): String? {
        return if (!isSDCardEnable()) null else Environment.getExternalStorageDirectory().path + File.separator + "data" + File.separator
    }

    /**
     * 获取SD卡剩余空间
     *
     * @return SD卡剩余空间
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun getFreeSpace(): String? {
        if (!isSDCardEnable()) return null
        val stat = StatFs(getSDCardPath())
        val blockSize: Long
        val availableBlocks: Long
        availableBlocks = stat.availableBlocksLong
        blockSize = stat.blockSizeLong
        return byte2FitMemorySize(availableBlocks * blockSize)
    }

    /**
     * 字节数转合适内存大小
     *
     * 保留3位小数
     *
     * @param byteNum 字节数
     * @return 合适内存大小
     */
    @SuppressLint("DefaultLocale")
    fun byte2FitMemorySize(byteNum: Long): String? {
        return if (byteNum < 0) {
            "shouldn't be less than zero!"
        } else if (byteNum < MemoryConstant.KB) {
            String.format("%.3fB", byteNum.toDouble() + 0.0005)
        } else if (byteNum < MemoryConstant.MB) {
            java.lang.String.format("%.3fKB", byteNum.toDouble() / MemoryConstant.KB + 0.0005)
        } else if (byteNum < MemoryConstant.GB) {
            java.lang.String.format("%.3fMB", byteNum.toDouble() / MemoryConstant.MB + 0.0005)
        } else {
            java.lang.String.format("%.3fGB", byteNum.toDouble() / MemoryConstant.GB + 0.0005)
        }
    }

    /**
     * 获取SD卡信息
     *
     * @return SDCardInfo
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun getSDCardInfo(): String? {
        if (!isSDCardEnable()) return null
        val sd = SDCardInfo()
        sd.isExist = true
        val sf = StatFs(Environment.getExternalStorageDirectory().path)
        sd.totalBlocks = sf.blockCountLong
        sd.blockByteSize = sf.blockSizeLong
        sd.availableBlocks = sf.availableBlocksLong
        sd.availableBytes = sf.availableBytes
        sd.freeBlocks = sf.freeBlocksLong
        sd.freeBytes = sf.freeBytes
        sd.totalBytes = sf.totalBytes
        return sd.toString()
    }

    class SDCardInfo {
        var isExist = false
        var totalBlocks: Long = 0
        var freeBlocks: Long = 0
        var availableBlocks: Long = 0
        var blockByteSize: Long = 0
        var totalBytes: Long = 0
        var freeBytes: Long = 0
        var availableBytes: Long = 0
        override fun toString(): String {
            return """
            isExist=$isExist
            totalBlocks=$totalBlocks
            freeBlocks=$freeBlocks
            availableBlocks=$availableBlocks
            blockByteSize=$blockByteSize
            totalBytes=$totalBytes
            freeBytes=$freeBytes
            availableBytes=$availableBytes
            """.trimIndent()
        }
    }

    /*是否支持intent*/
    fun isCallingSupported(context: Context, intent: Intent?): Boolean {
        var result = true
        val manager = context.packageManager
        val infos = manager.queryIntentActivities(intent!!, 0)
        if (infos.size <= 0) {
            result = false
        }
        return result
    }

    /**
     * 获取SDCard 可用容量大小
     *
     * @return MB
     */
    fun getAvaliableBytes(): Long {
        val file = Environment.getExternalStorageDirectory()
        val statFs = StatFs(file.path)
        return if (isSDCardEnable()) {
            // 可用的blocks的数量
            val availableBolocks: Long = statFs.availableBlocksLong
            // 单个block的大小
            val blockSize: Long = statFs.blockSizeLong
            // sd卡的剩余空间
            availableBolocks * blockSize
        } else {
            -1
        }
    }
}