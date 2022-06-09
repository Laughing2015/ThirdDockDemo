package com.kingsun.thirddock.util

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

object MemoryConstant {
    /**
     * Byte与Byte的倍数
     */
    const val BYTE = 1

    /**
     * KB与Byte的倍数
     */
    const val KB = 1024

    /**
     * MB与Byte的倍数
     */
    const val MB = 1048576

    /**
     * GB与Byte的倍数
     */
    const val GB = 1073741824

    @IntDef(BYTE, KB, MB, GB)
    @Retention(RetentionPolicy.SOURCE)
    annotation class Unit
}