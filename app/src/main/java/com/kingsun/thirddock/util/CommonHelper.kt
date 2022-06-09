package com.kingsun.thirddock.util

import android.content.Context
import android.provider.Settings
import android.widget.Toast
import java.io.File

/**
 * @Description: 通用方法
 * @Author: xiaolong.li
 * @CreateDate: 2022/6/8
 */
fun String.toast(context:Context,duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context,this,duration).show()
}

fun Context.getDeviceId():String{
    var deviceId = ""
    try{
       deviceId = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }catch (e:Exception){
        e.printStackTrace()
    }
    return deviceId
}

// 删除文件或文件夹
fun deleteFileOrDir(dirPath: String) = deleteFileOrDir(File(dirPath))

private fun deleteFileOrDir(file: File) {
    if (file.isFile) {
        deleteFileSafely(file)
        return
    }
    if (file.isDirectory) {
        val childFile = file.listFiles()
        if (childFile == null || childFile.isEmpty()) {
            deleteFileSafely(file)
            return
        }
        for (f in childFile) {
            deleteFileOrDir(f)
        }
        deleteFileSafely(file)
    }
}

/**
 * 安全删除文件.
 *
 * @param file
 * @return
 */
fun deleteFileSafely(file: File?): Boolean {
    if (file != null) {
        val tmpPath = file.parent + File.separator + System.currentTimeMillis()
        val tmp = File(tmpPath)
        file.renameTo(tmp)
        return tmp.delete()
    }
    return false
}