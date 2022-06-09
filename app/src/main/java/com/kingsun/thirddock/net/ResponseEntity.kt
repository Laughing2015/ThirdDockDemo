package com.kingsun.thirddock.net

data class ResponseData(
    val Data: String?,
    val ErrorCode: Int,
    val ErrorMsg: String?,
    val RequestID: String?,
    val Success: Boolean,
    val SystemTime: String?
)

data class ThirdPartyBookResource(
    val BookResource: BookResource?,
    val Device: String?
)

data class BookResource(
    val CreatedAt: String?,
    val ResourceUrl: String?,
    val Version: String?
)