package com.kingsun.thirddock.net

import android.content.Context
import okhttp3.Request
import okhttp3.Response

abstract class RequestCallBack<T>(protected var mContext: Context) : BaseCallBack<T>() {
    override fun onRequestBefore(request: Request?) {}
    override fun onResponse(response: Response?) {}
}