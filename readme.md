##### 前置条件

###### 1.调用方直提供的API进行书本资源下载

API信息：

```
地址（测试）：http://192.168.3.2:9222/aop/active
请求参数示例：
{
    "FunWay": 0,
    "FunName": "GetThirdPartyBookResource",
    "Info": "{\"BookID\":\"534\",\"Cooperation\":\"ZElephant\",\"DeviceNo\":\"aff8bf9551d2d36e\",\"SecretKey\":\"089f79cffa8af74e1966ba2fa34cd370\"}"
}
返回参数示例：
{
    "Data": "{\"BookResource\":{\"ResourceUrl\":\"https://bskcdn.kingsun.cn/prod/waterdrop/534_1652814879223.zip\",\"Version\":\"1.0.2\",\"CreatedAt\":\"2022-05-01 00:00:00\"},\"Device\":\"Pe6049pVeoO2SrOD/pua+hfw6AYkWASZwFkZ6t6wy89v4szsd1h+gbW24Bhg0oyrxuQefTWoT5sYo7cwSO37ew==\"}",
    "ErrorCode": 0,
    "ErrorMsg": null,
    "RequestID": null,
    "Success": true,
    "SystemTime": "/Date(1654677650875)/"
}
```

备注：由于跨应用共享文件目录涉及到文件读写权限获取的问题，android高版本限制了应用的文件读写范围，执象的设备是自定制系统，建议下载后将资源包存放至同步学HD app的外部存储的应用专属空间，即/storage/emulated/0/Android/data/com.elephant.synstudy.custom/files目录下，这样可以避免存储授权操作，优化体验

##### 接入说明

###### 1.安装 同步学HD app

apk包位于Demo工程根目录下

###### 2.使用Scheme协议完成app跳转

代码示例（详见Demo）：

```
val params = JsonObject()
params.addProperty("Device",deviceId) 
params.addProperty("BookId",bookId)   
params.addProperty("ResourcePath",resourcePath)
params.addProperty("Version",resourceVersion)

val intent = Intent()
intent.data = Uri.parse("kingsun://com.elephant.synstudy?"+ URLEncoder.encode(params.toString(),"UTF-8"))
intent.action = "android.intent.action.VIEW"
intent.addCategory("android.intent.category.DEFAULT")
intent.addCategory("android.intent.category.BROWSABLE")
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

try {
   startActivity(intent)
}catch (e:Exception){
   e.printStackTrace()               
}
```

params参数字段说明：

|字段名|说明|备注|
| --- | --- | --- |
|Device|设备id|android高版本不建议或禁用一般应用获取IMEI等设备信息，这里定为使用android_Id , 获取android_id不需要授权，但是会受签名影响发生变化，对接时需进一步沟通|
|BookId|书本id|使用方直科技的书本id|
|ResourcePath|资源包下载到本地完整路径|跨应用共享文件目录涉及到文件读写权限获取的问题，android高版本限制了应用的文件读写范围，执象的设备是自定制系统，建议将资源包下载至同步学HD app的外部存储的应用专属空间，即/storage/emulated/0/Android/data/com.elephant.synstudy.custom/files目录下，这样可以避免存储授权操作，优化体验|
|Version|资源包版本|用于资源版本更新|



