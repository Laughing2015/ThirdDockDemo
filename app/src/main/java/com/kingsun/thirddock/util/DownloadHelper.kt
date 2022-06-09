package com.kingsun.thirddock.util

import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.OkDownload
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.kotlin.enqueue4WithSpeed
import java.io.File

/**
 * @Author： fanda
 * @des： 单任务下载工具类
 */
object SingleDownloadHelper {

    private const val TAG = "SingleDownloadHelper"
    private var mDownLoadCallBack: DownLoadCallBack? = null
    private var mSingleTaskSavePath = ""
    private var task: DownloadTask? = null
    private var taskId: Int = -1

    /**
     * 作者：fanda
     * 描述：开始单任务下载，默认重新下载，不断点下载
     */
    fun startSingleTask(url: String, savePath: String, callBack: DownLoadCallBack,needBreakPoint: Boolean?=false) {
        mSingleTaskSavePath = savePath
        mDownLoadCallBack = callBack
        task = DownloadTask.Builder(url, File(savePath)).setMinIntervalMillisCallbackProcess(400)
            .setConnectionCount(1).setReadBufferSize(8192).setFlushBufferSize(32768).build()
        taskId = task?.id ?: -1
        startDownload(needBreakPoint)
    }

    /**
     * 作者：fanda
     * 描述：断点下载为继续下载，不断点下载就是重新下载
     */
    fun startDownload(needBreakPoint: Boolean?) {
        if (needBreakPoint == false && taskId != -1) {
            // 去除断点下载信息
            OkDownload.with().breakpointStore().remove(taskId)
            deleteDownloadFile()
        }
        var totalLength: Long = 0
        task?.enqueue4WithSpeed(
            onInfoReadyWithSpeed = { _, info, _, _ ->
                totalLength = info.totalLength
                mDownLoadCallBack?.onDownLoadReady(info.totalOffset, totalLength)
            },
            onProgressWithSpeed = { _, currentOffset, taskSpeed ->
                mDownLoadCallBack?.onDownLoading(currentOffset, totalLength, taskSpeed.speed())
            }
        ) { task, cause, realCause, taskSpeed ->
            when (cause) {
                EndCause.COMPLETED -> {
                    mDownLoadCallBack?.onDownLoadComplete(task.file!!.absolutePath)
                }
                EndCause.ERROR -> {
                    var tips = ""
                    realCause?.let { tips = it.toString() }
                    mDownLoadCallBack?.onDownLoadFail(
                        cause.toString(),
                        tips,
                        taskSpeed.averageSpeed()
                    )
                }
                else -> {
                }
            }
        }
    }

    /**
     * 作者：fanda
     * 描述：取消下载
     */
    fun cancelDownload() {
        task?.cancel()
    }

    /**
     * 作者：fanda
     * 描述：移除下载回调
     */
    fun removeDownLoadCallBack() {
        if (mDownLoadCallBack != null) {
            mDownLoadCallBack = null
        }
    }

    /**
     * 作者：fanda
     * 描述：删除下载文件
     */
    fun deleteDownloadFile() {
        if (mSingleTaskSavePath.isNotEmpty()) {
            val result = deleteFileOrDir(mSingleTaskSavePath)
        }
    }
}

/**
 * 作者：fanda
 * 时间：2020/8/14 17:39
 * 描述：接口回调接口实现
 */
open class DefaultDownLoadCallBack : DownLoadCallBack {

    override fun onDownLoadReady(offset: Long, total: Long) {}

    override fun onDownLoadComplete(savePath: String) {}

    override fun onDownLoadFail(state: String, errorMsg: String, speed: String) {}

    override fun onDownLoading(offset: Long, total: Long, speed: String) {}

}

/*接口回调*/
interface DownLoadCallBack {
    fun onDownLoadReady(offset: Long, total: Long)
    fun onDownLoadComplete(savePath: String)
    fun onDownLoadFail(state: String, errorMsg: String, speed: String)
    fun onDownLoading(offset: Long, total: Long, speed: String)
}

interface QueueDownloadCallBack {
    fun onDownLoadComplete(savePath: String, downloadMsg: DownloadMsg)
    fun onDownLoadFail(errorMsg: String)
}

interface DownloadMsg {
    val savePath: String
    val url: String
}
