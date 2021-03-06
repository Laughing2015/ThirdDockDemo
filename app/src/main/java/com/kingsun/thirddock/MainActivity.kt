package com.kingsun.thirddock

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
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
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var visibleSettingsButton = false
    private var visibleEvaluationButton = false
    private var isDirectlyOpen = false
    private var mMediaPlayer = MediaPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mOkHttpHelper = OkHttpHelper.instance
        binding.btnConfirm.setOnClickListener {
            if (binding.etBookId.text.toString().isNullOrEmpty()) {
                "??????id????????????".toast(this)
                return@setOnClickListener
            }
            checkAndRequestPermission()
        }
        binding.btnOpen.setOnClickListener {
            // ???????????????????????????????????????APP?????????????????? zip ??????????????????????????????????????????????????????????????????????????????
            isDirectlyOpen = true
            binding.btnConfirm.performClick()
        }
        binding.etDeviceId.setText(getDeviceId())

        mMyHandler.sendEmptyMessageDelayed(FLOAT_LAYOUT, 1000L)
    }

    /**
     * @Author??? fanda
     * @des???????????????????????????????????? AIDL ????????????
     */
    private fun handleFloatLayout() {
        // ??????????????????
        EasyFloat.with(this@MainActivity).setLayout(R.layout.aidl_layout)
            .setGravity(Gravity.CENTER_HORIZONTAL)
            .setShowPattern(ShowPattern.ALL_TIME)
            .show()
    }

    /**
     * @Author??? fanda
     * @des???????????????????????????
     */
    private fun handleLocalAudio(localPath: String) {
        // ????????????
        playFromSdCard(mMediaPlayer, localPath, completeCallback = {
            Log.d(TAG, "????????????")
        }, errorCallback = {
            Log.d(TAG, "????????????")
        })
    }

    // start -------------- ???????????????????????? -------------- //

    companion object {
        const val TAG = "MainActivity"
        const val PACKAGE_NAME = "com.elephant.synstudy.custom"
        const val REMOTE_SERVICE_ACTION = "com.kingsun.synstudy.custom.service.action"
        const val LOCAL_AUDIO_PATH =
            "/storage/emulated/0/TbxTest/1.0.5/28/28/audio/fc356a49aa1bce6e4dbde5156a47c2bf"
        const val UNBIND_SERVICE = 2
        const val FLOAT_LAYOUT = 3
        const val LOCAL_AUDIO = 4
    }

    private var isServiceConnected = false

    // ????????? AIDL ????????????????????????????????????????????????????????????????????????????????????
    private var isEnterBookSuccess = false
    private lateinit var mAppManager: IAppManager
    private val mMyHandler = MyHandler(this)

    private fun bindRemoteService() {
        val intent = Intent(REMOTE_SERVICE_ACTION).apply { `package` = PACKAGE_NAME }
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unBindRemoteService() {
        // ????????????
        if (isServiceConnected) unbindService(mServiceConnection)
        isServiceConnected = false
        isEnterBookSuccess = false
        EasyFloat.hide()
    }

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isServiceConnected = true
            mAppManager = IAppManager.Stub.asInterface(service)
            try {
                Log.d(TAG, "??????????????????onServiceConnected -> $service")
                mAppManager.registerOnAppListener(mOnAppListener)
            } catch (e: RemoteException) {
                e.printStackTrace()
                Log.d(TAG, "??????????????????${e.message}")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "?????????????????????onServiceDisconnected")
            isServiceConnected = false
            isEnterBookSuccess = false
        }
    }

    /**
     * @Author??? fanda
     * @des??? ??????????????????????????????????????????????????????????????? ??????????????????????????????????????????
     */
    private fun withRemoteErrorHandling(description: String = "", handler: () -> Unit) {
        if (isServiceConnected && isEnterBookSuccess) {
            try {
                handler()
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
        } else {
            "???????????????...$description".toast(this)
        }
    }

    private val mOnAppListener = object : IOnAppListener.Stub() {

        override fun onEnterBookCallback(bookUseInfo: BookUseInfo) {
            isEnterBookSuccess = true
            Log.d(TAG, "????????????????????? $bookUseInfo")
            // ??????????????? ???????????????????????????????????????????????????????????? onEnterBookCallback ????????????????????????????????????????????????????????????APP?????????????????????
            // ?????????????????????????????? ?????????APK?????????????????????????????????????????????????????????????????????
//            mAppManager.toggleSettingsButton(false)
//            mAppManager.toggleEvaluationButton(false)
        }

        override fun onExitAppCallback(bookUseInfo: BookUseInfo) {
            Log.d(TAG, "onExitAppCallback??? $bookUseInfo")
            // APK ?????????????????????????????????????????????
            mMyHandler.sendMessage(Message.obtain().apply {
                what = UNBIND_SERVICE
            })
        }

        override fun onBookUseTimeCallback(bookUseInfo: BookUseInfo) {
            // ???????????????????????????????????????????????????
            Log.d(TAG, "onBookUseTimeCallback -> $bookUseInfo")
        }

        override fun onChangeBookPageCallback(bookUseInfo: BookUseInfo) {
            // ??????????????????
            Log.d(TAG, "onChangeBookPageCallback -> $bookUseInfo")
        }

        override fun onOpenBookResourceCallback(bookUseInfo: BookUseInfo) {
            // ????????????????????????
            Log.d(TAG, "onOpenBookResourceCallback -> $bookUseInfo")
        }

        override fun onEvaluateCallback(bookUseInfo: BookUseInfo) {
            // ??????
            Log.d(TAG, "onEvaluateCallback -> $bookUseInfo")
            handleLocalAudio(bookUseInfo.evaluateAudioPath)
        }

    }

    // ??????????????? Handler ???
    private class MyHandler(activity: MainActivity?) : Handler() {
        private val mReference: WeakReference<MainActivity>
        override fun handleMessage(msg: Message) {
            // ??????UI?????????
            if (mReference.get() != null) {
                when (msg.what) {
                    UNBIND_SERVICE -> {
                        mReference.get()?.let {
                            Log.d(TAG, "????????????")
                            it.unBindRemoteService()
                        }
                    }
                    FLOAT_LAYOUT -> {
                        mReference.get()?.handleFloatLayout()
                    }
                    LOCAL_AUDIO -> {
                        mReference.get()?.handleLocalAudio(LOCAL_AUDIO_PATH)
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
        // ????????????
        unBindRemoteService()
        mMyHandler.removeCallbacksAndMessages(null)
    }

    // end -------------- ???????????????????????? -------------- //

    /**
     *@Desc ??????????????????
     *@Author xiaolong.li
     *@Time 2022/6/9 14:11
     */
    private fun checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // ????????????????????????
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
                //????????????
                doAfterGranted()
            } else {
                //????????????
                if (!EasyPermissions.somePermissionDenied(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    //????????????????????????
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
                    //????????????
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
     *@Desc ??????????????????????????????
     *@Author xiaolong.li
     *@Time 2022/6/9 14:12
     */
    @AfterPermissionGranted(Permissions.RC_STORAGE_PERM)
    private fun doAfterGranted() {
        requestBookInfo()
    }

    /**
     *@Desc ???api????????????????????????
     *@Author xiaolong.li
     *@Time 2022/6/9 14:12
     */
    private fun requestBookInfo() {
        val info = JsonObject()
        val requestData = JsonObject()
        requestData.addProperty("FunWay", 0) //0????????????
        requestData.addProperty("FunName", API_GET_THIRD_PARTY_BOOK_RESOURCE) //???????????????
        info.addProperty("BookID", binding.etBookId.text.toString()) //????????????id
        info.addProperty("Cooperation", COOPERATION) //?????????????????????
        info.addProperty("DeviceNo", getDeviceId())  //??????????????????Androidid???
        info.addProperty("SecretKey", SECRET_KEY)   //??????????????????????????????????????????
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
                                "??????????????????".toast(this@MainActivity)
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
     *@Desc ?????????????????????
     *@Author xiaolong.li
     *@Time 2022/6/9 14:14
     */
    private fun downloadBook(bookResource: ThirdPartyBookResource?) {
        bookResource?.BookResource?.ResourceUrl?.let {
            val desPath =
                SDCardUtils.getSDCardRootPath() + "TbxTest" + File.separator + binding.etBookId.text.toString() + ".zip"
            val desFile = File(desPath)
            if (isDirectlyOpen) {
                openTbxHD(bookResource, desPath)
                isDirectlyOpen = false
                return
            } else {
                if (desFile.exists()) {
                    openTbxHD(bookResource, desPath)
                    return
                } else {
                    File(desFile.parent).mkdirs()
                }
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

                    override fun onDownLoadFail(
                        state: String,
                        errorMsg: String,
                        speed: String
                    ) {
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
     *@Desc ???????????? ?????????HD app
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

        EasyFloat.show()
//        Log.d(TAG,"???????????????${EasyFloat.getFloatView()}")
        binding.root.post {
            EasyFloat.getFloatView()?.apply {
                findViewById<Button>(R.id.btn_exit_app).setOnClickListener {
                    // ????????????
                    withRemoteErrorHandling("exitApp") {
                        mAppManager.exitApp()
                        unBindRemoteService()
                    }
                }
                findViewById<Button>(R.id.btn_visible_settings).setOnClickListener {
                    // ????????????????????????
                    withRemoteErrorHandling("toggleSettingsButton") {
                        mAppManager.toggleSettingsButton(visibleSettingsButton)
                        visibleSettingsButton = !visibleSettingsButton
                    }
                }
                findViewById<Button>(R.id.btn_visible_evaluation).setOnClickListener {
                    // ????????????????????????
                    withRemoteErrorHandling("toggleEvaluationButton") {
                        mAppManager.toggleEvaluationButton(visibleEvaluationButton)
                        visibleEvaluationButton = !visibleEvaluationButton
                    }
                }
                findViewById<Button>(R.id.btn_local_audio).setOnClickListener {
                    // ????????????????????????
                    handleLocalAudio(LOCAL_AUDIO_PATH)
                }
            }
        }
    }

    /**
     *@Desc scheme??????????????????
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
                timerTask = object : TimerTask() {
                    override fun run() {
                        if (!isServiceConnected) {
                            bindRemoteService()
                        } else {
                            cancelTimer()
                        }
                    }
                }
                timer?.schedule(timerTask, 1000, 1000)
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.toast(this@MainActivity)
            }
        }
    }

    fun cancelTimer() {
        timerTask?.cancel()
        timer?.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_MANAGE_APP_ALL_FILES_ACCESS_PERM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                doAfterGranted()
            } else {
                "????????????????????????".toast(this)
            }
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            //???????????????????????????????????????
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
        //???????????????????????????????????????
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