package com.kingsun.thirddock.net

/**
 * @Description: 小鹅通常量
 * @Author: xiaolong.li
 * @CreateDate: 2022/3/15
 */
object Const {

    //合作方标识
    const val COOPERATION = "ZElephant"
    //合作方秘钥
    const val SECRET_KEY = "089f79cffa8af74e1966ba2fa34cd370"

    private const val BASE_URL = "http://183.47.42.218:9222/"
    //api地址
    const val API_URL = "${BASE_URL}aop/active"
    /*获取书本资源信息*/
    const val API_GET_THIRD_PARTY_BOOK_RESOURCE = "GetThirdPartyBookResource"
}