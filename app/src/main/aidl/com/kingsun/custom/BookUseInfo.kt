package com.kingsun.custom

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BookUseInfo(
    var bookId: Int,                        // 书本 ID
    var unitId: Int = 0,                    // 当前选中目录父ID
    var lessonId: Long = 0L,                // 当前选中目录ID
    var pageId: Int = 0,                    // 页码
    var resourceId: Int = 0,                // 资源ID
    var resourceSubType: Int = 0,           // 课件资源类型【52 - 交互动画 ，53 - 交互课件，54 - 生字 ，55 - 拼音课件】
    var resourceType: Int = 0,              // 同步资源类型【5 - 动画配音 ，15 - 语文朗读，2 - 跟读评测】
    var resourceCategory: Int = 0,          // 资源分类【1、同步资源； 2、教辅资源】
    var bookUseTime: Long = 0L,             // 书本使用时长，单位秒
    var evaluateText: String = "",          // 评测文本
    var evaluateAudioPath: String = "",     // 评测音频本地路径
) : Parcelable