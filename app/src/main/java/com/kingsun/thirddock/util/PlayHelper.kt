package com.kingsun.thirddock.util

import android.media.MediaPlayer
import java.io.File

/**
 * 作者：fanda
 * 描述：播放SD卡文件夹下的音频
 */
@JvmOverloads
fun playFromSdCard(
    player: MediaPlayer?,
    localPath: String,
    errorCallback: ((msg:String) -> Unit) = {},
    completeCallback: (() -> Unit) = {}
) {
    try {
        val sound = File(localPath)
        if (sound.exists()) {
            player?.reset()
            player?.setDataSource(localPath)
            player?.prepareAsync()
            player?.setOnPreparedListener { it.start() }
            player?.setOnCompletionListener {
                completeCallback()
            }
            player?.setOnErrorListener { mp, _, _ ->
                mp.stop()
                errorCallback("播放异常")
                true
            }
        } else {
            errorCallback("文件不存在")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        errorCallback(e.message?: "未知异常")
    }
}