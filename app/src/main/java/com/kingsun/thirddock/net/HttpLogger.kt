package com.kingsun.thirddock.net

import android.util.Log
import okhttp3.logging.HttpLoggingInterceptor

class HttpLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        Log.d("XiaoE_HttpLogInfo", message)
    }
}