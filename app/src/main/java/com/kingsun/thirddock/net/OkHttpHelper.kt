package com.kingsun.thirddock.net

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.JsonParseException
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class OkHttpHelper private constructor() {
    private val handler: Handler
    private val gson: Gson
    operator fun get(url: String, callBack: BaseCallBack<*>) {
        get(url, null, callBack)
    }

    operator fun get(url: String, params: Map<String, Any?>?, callBack: BaseCallBack<*>) {
        val request = buildGetRequest(url, params)
        doRequest(request, callBack)
    }

    fun post(url: String, params: Map<String, Any?>?, callBack: BaseCallBack<*>) {
        post(url, params, null, callBack)
    }

    fun post(url: String, params: String, callBack: BaseCallBack<*>) {
        post(url, params, null, callBack)
    }

    fun post(
        url: String,
        params: Map<String, Any?>?,
        headers: Map<String, String>?,
        callBack: BaseCallBack<*>
    ) {
        val request = buildPostRequest(url, params, headers)
        doRequest(request, callBack)
    }

    fun post(
        url: String,
        params: String,
        headers: Map<String, String>?,
        callBack: BaseCallBack<*>
    ) {
        val request = buildPostRequest(url, params, headers)
        doRequest(request, callBack)
    }

    private fun doRequest(request: Request, callBack: BaseCallBack<*>) {
        callBack.onRequestBefore(request)
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callbackFailed(callBack, call.request(), e)
            }

            override fun onResponse(call: Call, response: Response) {
                callbackResponse(callBack, response)
                if (response.isSuccessful) {
                    var resultStr:String? = null //数据结果字符串
                    var exception: Exception?=null //异常
                    response.body()?.apply {
                        try {
                            resultStr = string()
                        }catch (e: IOException){
                            exception = e //IO异常
                        }
                    }
                    if(resultStr.isNullOrEmpty()){
                        //body数据异常
                        callbackError(callBack, response, response.code(),
                            RESPONSE_DATA_EMPTY, exception)
                    }else{
                        //body数据正常
                        if (callBack.mType === String::class.java) {
                            //以String类型返回
                            callbackSuccess(callBack, response, resultStr!!)
                        } else {
                            //以泛型类型结果返回
                            var any:Any?=null
                            try {
                                any = gson.fromJson<Any>(resultStr, callBack.mType)
                            } catch (e: JsonParseException) {
                                exception = e
                            }
                            if(any!=null) {
                                //数据不为空
                                callbackSuccess(callBack, response, any)
                            }else{
                                //数据解析异常
                                callbackError(callBack, response, response.code(),
                                    RESPONSE_DATA_PARSE_ERROR, exception)
                            }
                        }
                    }
                }else {
                    callbackError(callBack, response, response.code(), RESPONSE_ERROR, null)
                }
            }
        })
    }

    private fun callbackResponse(callBack: BaseCallBack<*>, response: Response) {
        handler.post { callBack.onResponse(response) }
    }

    private fun callbackFailed(callBack: BaseCallBack<*>, request: Request, e: IOException) {
        handler.post { callBack.onFailure(request, REQUEST_FAILED, e) }
    }

    private fun callbackSuccess(callBack: BaseCallBack<*>, response: Response, any:Any) {
        handler.post { callBack.onSuccess(response, any) }
    }

    private fun callbackError(
        callBack: BaseCallBack<*>,
        response: Response,
        code: Int,
        msg:String?,
        e: Exception?
    ) {
        handler.post { callBack.onError(response, code,msg, e) }
    }

    private fun buildPostRequest(
        url: String,
        params: Map<String, Any?>?,
        headers: Map<String, String>? = null
    ): Request {
        return buildRequest(url, params, headers, HttpMethodType.POST)
    }

    private fun buildPostRequest(
        url: String,
        body: String,
        headers: Map<String, String>?
    ): Request {
        val builder = Request.Builder().url(url)
        val requestBody = buildRequestBody(headers, body)
        builder.post(requestBody)
        if (headers != null) {
            for ((key, value) in headers) {
                builder.header(key, value)
            }
        }
        return builder.build()
    }

    private fun buildGetRequest(
        url: String,
        params: Map<String, Any?>?,
        headers: Map<String, String>? = null
    ): Request {
        return buildRequest(url, params, headers, HttpMethodType.GET)
    }

    private fun buildRequest(
        url: String,
        params: Map<String, Any?>?,
        headers: Map<String, String>?,
        methodType: HttpMethodType
    ): Request {
        var mUrl = url
        val builder = Request.Builder().url(mUrl)
        if (methodType == HttpMethodType.GET) {
            mUrl = buildUrlParams(mUrl, params)
            builder.url(mUrl)
            builder.get()
        } else if (methodType == HttpMethodType.POST) {
            val body = buildFormData(params)
            builder.post(body)
        }
        if (headers != null) {
            for ((key, value) in headers) {
                builder.header(key, value)
            }
        }
        return builder.build()
    }

    private fun buildUrlParams(url: String, params: Map<String, Any?>?): String {
        var mUrl = url
        if (params == null || params.isEmpty()) {
            return mUrl
        }
        val sb = StringBuilder()
        for ((key, value) in params) {
            sb.append(key).append("=").append(value?.toString() ?: "")
            sb.append("&")
        }
        var s = sb.toString()
        if (s.endsWith("&")) {
            s = s.substring(0, s.length - 1)
        }
        mUrl = if (mUrl.indexOf("?") > 0) {
            "$url&$s"
        } else {
            "$url?$s"
        }
        return mUrl
    }

    private fun buildFormData(params: Map<String, Any?>?): RequestBody {
        val builder = FormBody.Builder()
        if (params != null) {
            for ((key, value) in params) {
                builder.add(key, value?.toString() ?: "")
            }
        }
        return builder.build()
    }

    private fun buildRequestBody(headers: Map<String, String>?, body: String): RequestBody {
        var contentType: String? = null
        if (headers != null) {
            contentType = headers["Content-Type"]
        }
        if (contentType == null) {
            contentType = "application/json;charset=UTF-8"
        }
        return RequestBody.create(MediaType.parse(contentType), body)
    }

    private enum class HttpMethodType {
        GET, POST
    }

    companion object {
        const val REQUEST_FAILED = "接口请求失败"
        const val RESPONSE_ERROR = "接口返回异常"
        const val RESPONSE_DATA_PARSE_ERROR = "接口返回数据解析异常"
        const val RESPONSE_DATA_EMPTY = "接口返回空数据"
        const val TIME_OUT = 15L //超时时间
        private lateinit var okHttpClient: OkHttpClient
        val instance: OkHttpHelper
            get() = OkHttpHelper()
    }

    init {
        val logInterceptor = HttpLoggingInterceptor(HttpLogger())
        logInterceptor.level = HttpLoggingInterceptor.Level.BODY
        okHttpClient = OkHttpClient.Builder()
            .readTimeout(TIME_OUT, TimeUnit.SECONDS)
            .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
            .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
            .addNetworkInterceptor(logInterceptor)
            .build()
        gson = Gson()
        handler = Handler(Looper.getMainLooper())
    }
}