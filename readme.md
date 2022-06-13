#### 前置条件

###### 1.调用方直提供的API进行书本资源下载

API信息（目前提供的是测试环境，需要定向开放才可访问，调试前需将执象调用方的外网ip提供给方直进行开通）：

```
地址（测试）：http://183.47.42.218:9222/aop/active
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

请求字段说明：

|   字段名    |  类型  |      说明      | 备注                                                         |
| :---------: | :----: | :------------: | :----------------------------------------------------------- |
|   FunWay    |  Int   |    接口方式    | 必传（固定传0）                                              |
|   FunName   | String |   接口方法名   | 必传                                                         |
|   BookID    |  Int   | 方直科技书本id | 必传                                                         |
| Cooperation | String |    合作方名    | 必传                                                         |
|  DeviceNo   | String |     设备id     | 必传（android高版本不建议或禁用一般应用获取IMEI等设备信息，<br/>这里暂定为使用android_Id , 获取android_id不需要授权，<br/>但是会受签名影响发生变化，对接时需进一步沟通） |
|  SecretKey  | String |   合作方秘钥   | 必传（由方直分配）                                           |

返回字段说明：

|   字段名    |  类型  |       说明       | 备注                                                         |
| :---------: | :----: | :--------------: | :----------------------------------------------------------- |
| ResourceUrl | String | 书本资源下载链接 | 由于跨应用共享文件目录涉及到文件读写权限获取的问题，<br/>android高版本限制了应用的文件读写范围，执象的设备是自定制系统，<br/>建议下载后将资源包存放至同步学HD app的外部存储的应用专属空间，即<br/>/storage/emulated/0/Android/data/com.elephant.synstudy.custom/files<br/>目录下，这样可以避免存储授权操作，优化体验 |
|   Version   | String |  书本资源版本号  |                                                              |
|   Device    | String |   设备鉴权信息   |                                                              |

#### 接入说明

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

|字段名|类型|说明|备注|
| --- | :-: | --- | --- |
|Device|String|设备鉴权信息|必传（GetThirdPartyBookResource接口返回字段）|
|BookId|Int|书本id|必传（使用方直科技的书本id）|
|CatalogueId|Int|目录id|可选|
|ResourcePath|String|资源包下载到本地完整路径|必传|
|Version|String|资源包版本|必传（GetThirdPartyBookResource接口返回字段，用于处理资源版本更新）|

###### 3.书本学习记录获取

退出电子书页面会保留以下信息至ResourcePath的父级路径下/study_record/bookid.json中，自行读取文件内容获取

完整路径示例：/storage/emulated/0/Android/data/com.elephant.synstudy.custom/files/study_record/534.json

内容示例：

```
{
    "bookId": 534,//方直书本id
    "catalogueId":55763,//方直目录id
    "pageNum": 1 //书页页码号
}
```



注：Demo工程内包含完整的调用API下载资源、scheme协议跳转流程，运行Demo可看具体效果