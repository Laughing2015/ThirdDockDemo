package com.kingsun.thirddock.net

import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class BaseCallBack<T> {
    var mType: Type ?= getSuperclassTypeParameter(javaClass)
    abstract fun onRequestBefore(request: Request?)
    abstract fun onFailure(request: Request?, msg:String?, e: IOException?)

    /**
     * 请求成功时调用此方法
     */
    abstract fun onResponse(response: Response?)

    /**
     * 状态码大于200，小于300时调用此方法
     */
    abstract fun onSuccess(response: Response?, result: Any)

    /**
     * 状态码400，403，500等时调用此方法
     */
    abstract fun onError(response: Response?, code: Int , msg:String?, e: Exception?)

    companion object {
        fun getSuperclassTypeParameter(subClass: Class<*>): Type? {
            val superclass = subClass.genericSuperclass
            (superclass as ParameterizedType).apply {
                return actualTypeArguments[0]
            }
        }
    }

}