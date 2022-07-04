package com.kingsun.thirddock

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kingsun.custom.BookUseInfo
import com.kingsun.custom.IAppManager
import com.kingsun.custom.IOnAppListener
import com.kingsun.thirddock.Permissions.RC_MANAGE_APP_ALL_FILES_ACCESS_PERM
import com.kingsun.thirddock.databinding.ActivityMainBinding
import com.kingsun.thirddock.net.Const.API_GET_THIRD_PARTY_BOOK_RESOURCE
import com.kingsun.thirddock.net.Const.API_URL
import com.kingsun.thirddock.net.Const.COOPERATION
import com.kingsun.thirddock.net.Const.SECRET_KEY
import com.kingsun.thirddock.net.OkHttpHelper
import com.kingsun.thirddock.net.RequestCallBack
import com.kingsun.thirddock.net.ResponseData
import com.kingsun.thirddock.net.ThirdPartyBookResource
import com.kingsun.thirddock.util.*
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import okhttp3.Request
import okhttp3.Response
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.util.*


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks {
    lateinit var binding: ActivityMainBinding
    private lateinit var mOkHttpHelper: OkHttpHelper
    var timer:Timer?=null
    var timerTask:TimerTask?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mOkHttpHelper = OkHttpHelper.instance
        binding.btnConfirm.setOnClickListener {
            if (binding.etBookId.text.toString().isNullOrEmpty()) {
                "书本id不能为空".toast(this)
                return@setOnClickListener
            }
            checkAndRequestPermission()
        }
        binding.etDeviceId.setText(getDeviceId())

        EasyFloat.with(this).setLayout(R.layout.aidl_layout).setShowPattern(ShowPattern.ALL_TIME)
            .show()
        binding.root.post {
            handleFloatLayout()
        }
    }

    /**
     * @Author： fanda
     * @des：处理悬浮窗口事件，用于 AIDL 服务交互
     */
    private fun handleFloatLayout() {
        EasyFloat.getFloatView()?.apply {
            findViewById<Button>(R.id.btn_exit_app).setOnClickListener {
                // 关闭应用
                withRemoteErrorHandling {
                    mAppManager.exitApp()
                    // 解绑服务
                    if (isServiceConnected) unbindService(mServiceConnection)
                    isServiceConnected = false
                }
            }
        }
    }

    // start -------------- 远程服务相关处理 -------------- //

    companion object {
        const val TAG = "MainActivity"
        const val PACKAGE_NAME = "com.elephant.synstudy.custom"
        const val REMOTE_SERVICE_ACTION = "com.kingsun.synstudy.custom.service.action"
        const val WHAT_BOOK_USE_INFO = 1
    }

    private var isServiceConnected = false
    private lateinit var mAppManager: IAppManager
    private val mMyHandler = MyHandler(this)

    private fun bindRemoteService() {
        val intent = Intent(REMOTE_SERVICE_ACTION).apply { `package` = PACKAGE_NAME }
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "服务已连接：onServiceConnected")
            isServiceConnected = true
            mAppManager = IAppManager.Stub.asInterface(service)
            withRemoteErrorHandling { mAppManager.registerOnAppListener(mOnAppListener) }
            try {
                service?.linkToDeath(mDeathRecipient, 0)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "服务异常中断：onServiceDisconnected")
            isServiceConnected = false
            withRemoteErrorHandling { mAppManager.unRegisterOnAppListener(mOnAppListener) }
            // 重新绑定服务
            bindRemoteService()
        }

    }

    /**
     * @Author： fanda
     * @des： Binder 死亡代理
     */
    private val mDeathRecipient = IBinder.DeathRecipient {
        //该方法在client 端线程池中运行，远程服务进程异常，重新连接服务
        bindRemoteService()
    }

    /**
     * @Author： fanda
     * @des： 在服务连接成功时 调用远程服务方法，并捕获异常
     */
    private fun withRemoteErrorHandling(handler: () -> Unit) {
        if (isServiceConnected) {
            try {
                handler()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        } else {
            "服务中断".toast(this)
        }
    }

    private val mOnAppListener = object : IOnAppListener.Stub() {
        override fun onBookUseInfoCallback(bookUseInfo: BookUseInfo) {
            // 该回调在客户端 Binder 线程池执行，需切换主线程进行操作
            mMyHandler.sendMessage(Message.obtain().apply {
                obj = bookUseInfo
                what = WHAT_BOOK_USE_INFO
            })
        }
    }

    // 静态自定义 Handler 类
    private class MyHandler(activity: MainActivity?) : Handler() {
        private val mReference: WeakReference<MainActivity>
        override fun handleMessage(msg: Message) {
            // 更新UI等操作
            if (mReference.get() != null) {
                when (msg.what) {
                    WHAT_BOOK_USE_INFO -> {
                        Log.d(TAG, "获取书本使用信息: ${msg.obj as BookUseInfo}")
                    }
                }
            }
        }

        init {
            mReference = WeakReference(activity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTimer()
        // 解绑服务
        if (isServiceConnected) unbindService(mServiceConnection)
        mMyHandler.removeCallbacksAndMessages(null)
    }

    // end -------------- 远程服务相关处理 -------------- //

    /**
     *@Desc 存储权限检查
     *@Author xiaolong.li
     *@Time 2022/6/9 14:11
     */
    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {
                doAfterGranted()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.setData(Uri.parse("package:$packageName"))
                startActivityForResult(intent, RC_MANAGE_APP_ALL_FILES_ACCESS_PERM)
            }
        } else {
            if (EasyPermissions.hasPermissions(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {
                //已有权限
                doAfterGranted()
            } else {
                //没有权限
                if (!EasyPermissions.somePermissionDenied(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    //弹出权限意图弹窗
                    val permissionRequest: PermissionRequest =
                        PermissionRequest.Builder(
                            this,
                            Permissions.RC_STORAGE_PERM,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )
                            .setNegativeButtonText(
                                resources.getString(R.string.str_txt_cancel)
                            )
                            .setPositiveButtonText(
                                resources.getString(R.string.str_txt_confirm)
                            )
                            .setRationale(
                                resources
                                    .getString(R.string.str_txt_storage_permission_request_tips)
                            )
                            .build()
                    showPurposeTipsBeforeRequest(
                        this,
                        resources
                            .getString(R.string.str_txt_storage_permission_request_tips),
                        permissionRequest
                    )
                } else {
                    //请求权限
                    EasyPermissions.requestPermissions(
                        this,
                        resources
                            .getString(R.string.str_txt_storage_permission_request_tips),
                        Permissions.RC_STORAGE_PERM,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
            }
        }
    }

    /**
     *@Desc 获取存储权限后的操作
     *@Author xiaolong.li
     *@Time 2022/6/9 14:12
     */
    @AfterPermissionGranted(Permissions.RC_STORAGE_PERM)
    private fun doAfterGranted() {
        requestBookInfo()
    }

    /**
     *@Desc 调api获取书本资源信息
     *@Author xiaolong.li
     *@Time 2022/6/9 14:12
     */
    private fun requestBookInfo() {
        val info = JsonObject()
        val requestData = JsonObject()
        requestData.addProperty("FunWay", 0) //0（固定）
        requestData.addProperty("FunName", API_GET_THIRD_PARTY_BOOK_RESOURCE) //接口方法名
        info.addProperty("BookID", binding.etBookId.text.toString()) //方直书本id
        info.addProperty("Cooperation", COOPERATION) //合作方（固定）
        info.addProperty("DeviceNo", getDeviceId())  //设备信息（传Androidid）
        info.addProperty("SecretKey", SECRET_KEY)   //合作方秘钥（由方直科技分配）
        requestData.addProperty("Info", info.toString())
        mOkHttpHelper.post(
            API_URL,
            requestData.toString(),
            object : RequestCallBack<ResponseData?>(this@MainActivity) {
                override fun onFailure(request: Request?, msg: String?, e: IOException?) {
                    msg?.toast(this@MainActivity)
                }

                override fun onSuccess(response: Response?, result: Any) {
                    (result as ResponseData).apply {
                        if (result.Success) {
                            var bookResource: ThirdPartyBookResource? = null
                            try {
                                bookResource = Gson().fromJson(
                                    result.Data,
                                    ThirdPartyBookResource::class.java
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (bookResource == null) {
                                "书本数据异常".toast(this@MainActivity)
                            } else {
                                downloadBook(bookResource)
                            }
                        } else {
                            result.ErrorMsg?.toast(this@MainActivity)
                        }
                    }
                }

                override fun onError(
                    response: Response?,
                    code: Int,
                    msg: String?,
                    e: java.lang.Exception?
                ) {
                    msg?.toast(this@MainActivity)
                }
            })
    }

    /**
     *@Desc 下载书本资源包
     *@Author xiaolong.li
     *@Time 2022/6/9 14:14
     */
    private fun downloadBook(bookResource: ThirdPartyBookResource?) {
        bookResource?.BookResource?.ResourceUrl?.let {
            val desPath =
                SDCardUtils.getSDCardRootPath() + "TbxTest" + File.separator + binding.etBookId.text.toString() + ".zip"
            val desFile = File(desPath)
            if (desFile.exists()) {
                openTbxHD(bookResource, desPath)
                return
            } else {
                File(desFile.parent).mkdirs()
            }
            SingleDownloadHelper.startSingleTask(it,
                desPath,
                object : DownLoadCallBack {
                    override fun onDownLoadReady(offset: Long, total: Long) {
                        binding.tvLoadTips.visibility = View.VISIBLE
                        binding.pbProgress.visibility = View.VISIBLE
                        binding.pbProgress.progress = 0
                    }

                    override fun onDownLoadComplete(savePath: String) {
                        openTbxHD(bookResource, savePath)
                        binding.tvLoadTips.visibility = View.GONE
                        binding.pbProgress.visibility = View.GONE
                    }

                    override fun onDownLoadFail(state: String, errorMsg: String, speed: String) {
                        errorMsg.toast(this@MainActivity)
                        binding.tvLoadTips.visibility = View.GONE
                        binding.pbProgress.visibility = View.GONE
                    }

                    override fun onDownLoading(offset: Long, total: Long, speed: String) {
                        binding.pbProgress.progress =
                            (offset.toFloat() / total.toFloat() * 100f).toInt()
                    }

                }
            )
        }
    }

    /**
     *@Desc 跳转方直 同步学HD app
     *@Author xiaolong.li
     *@Time 2022/6/9 14:15
     */
    private fun openTbxHD(bookResource: ThirdPartyBookResource?, savePath: String?) {
        bookResource?.apply {
            val params = JsonObject()
            params.addProperty("Device", Device)
            params.addProperty("BookId", binding.etBookId.text.toString().toInt())
            savePath?.let {
                params.addProperty("ResourcePath", it)
            }
            BookResource?.Version?.let {
                params.addProperty("Version", it)
            }
            deepLinkApk(params)
        }
    }

    /**
     *@Desc scheme方式带参跳转
     *@Author xiaolong.li
     *@Time 2022/6/9 14:15
     */
    @SuppressLint("QueryPermissionsNeeded")
    private fun deepLinkApk(params: JsonObject?) {
        params?.apply {
            val intent = Intent()
            intent.data = Uri.parse(
                "kingsun://com.elephant.synstudy?" + URLEncoder.encode(
                    this.toString(),
                    "UTF-8"
                )
            )
            intent.action = "android.intent.action.VIEW"
            intent.addCategory("android.intent.category.DEFAULT")
            intent.addCategory("android.intent.category.BROWSABLE")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            try {
                startActivity(intent)
                cancelTimer()
                timer = Timer()
                timerTask = object : TimerTask(){
                    override fun run() {
                        if(!isServiceConnected){
                            bindRemoteService()
                        }else{
                            cancelTimer()
                        }
                    }
                }
                timer?.schedule(timerTask,1000,1000)
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.toast(this@MainActivity)
            }
        }
    }

    fun cancelTimer(){
        timerTask?.cancel()
        timer?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_MANAGE_APP_ALL_FILES_ACCESS_PERM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                doAfterGranted()
            } else {
                "存储权限获取失败".toast(this)
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            //存在权限勾选不在提示并拒绝
            permissionPermanentlyDeniedTips(this)
        }
    }

    override fun onRationaleAccepted(requestCode: Int) {

    }

    override fun onRationaleDenied(requestCode: Int) {

    }

    private fun showPurposeTipsBeforeRequest(
        host: Activity,
        rationale: String,
        permissionRequest: PermissionRequest
    ) {
        val builder = AlertDialog.Builder(host)
        builder.setCancelable(false)
            .setMessage(rationale)
            .setNegativeButton(
                host.resources.getString(R.string.str_txt_cancel)
            ) { _, _ ->
            }
            .setPositiveButton(
                host.resources.getString(R.string.str_txt_confirm)
            ) { _, _ -> EasyPermissions.requestPermissions(permissionRequest) }
            .show()
    }

    private fun permissionPermanentlyDeniedTips(host: Activity) {
        //存在权限勾选不在提示并拒绝
        AppSettingsDialog.Builder(host)
            .setTitle("")
            .setRationale(host.resources.getString(R.string.str_txt_storage_permission_forbid_tips))
            .build().show()
    }
}

object Permissions {
    const val RC_STORAGE_PERM = 1006
    const val RC_MANAGE_APP_ALL_FILES_ACCESS_PERM = 1007
}