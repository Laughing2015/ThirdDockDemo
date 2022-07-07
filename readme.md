#### 前置条件

##### 1.调用方直提供的API进行书本资源下载

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

##### 1.安装 同步学HD app

apk包位于Demo工程根目录下

##### 2.文件配置

在 module 下的 build.gradle 内添加如下配置：


```
// 该插件可选，只是为了方便实体类序列化，如果不用，自行实现 parcelable 序列化操作
apply plugin: 'kotlin-parcelize'

   android {     
        // 指定 aidl 文件的放置路径
        sourceSets{
            main {
                java.srcDirs = ['src/main/java', 'src/main/aidl']
            }
        }
    }
```

在 AndroidManifest.xml 中添加如下权限

```
<!--申请服务权限，才能连接服务-->
<uses-permission android:name="com.kingsun.custom.permission.ACCESS_APP_SERVICE"/>
```

##### 3.AIDL 文件处理

把 Demo 中的 aidl 文件夹拷贝到项目中，跟 java 项目平级，AIDL 文件和对应的实体为双方约定的服务接口及数据体。

注：如果服务端 APK 有相关的 AIDL 更新，客户端要同步更新，不要更改 aidl 目录下的任何文件以及包名等，避免服务不一致导致异常。

##### 4.使用Scheme协议完成app跳转

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

##### 5.连接远程服务

步骤1.  一定要先通过 bindService 绑定服务，因为不能确保打开APP的时候服务完全启动，建议循环进行绑定直到绑定成功，具体操作看 demo。

步骤2.  再注册监听事件，在绑定服务成功后，建议立刻注册监听事件，这样能让服务器的接口回调尽快返回。

步骤3.  等待 APP 服务端回调 【进入课本成功回调】，在前提下，再主动调用各种接口功能，回调的接口功能在监听事件中进行回调。

步骤4.  解绑服务，重置标志字段。

注意：

1. 一定要等待 APP 服务端回调 【进入课本成功回调】再调用 AIDL 接口方法 ，确保服务端完全启动并保证APP数据初始化完成 （除了监听事件的交互方法在绑定服务成功后立刻调用）

2. 在主动关闭应用或APP 内部关闭应用回调时，主动进行解绑服务操作，重置标志，避免后续 AIDL 交互出现异常

##### 6.数据交互

1. 目前所有交互数据都在 BookUseInfo 实体中，所有的回调方法都会返回该对象，具体的值根据各个接口回调进行填充，在对应的回调方法中获取对应的值即可。

2. IAppManager 接口表示客户端主动调用的所有功能方法，IOnAppListener 接口表示服务端操作会进行的回调功能。

注：Demo工程内包含完整的调用API下载资源、scheme协议跳转流程、AIDL 接口功能交互，运行Demo可看具体效果。