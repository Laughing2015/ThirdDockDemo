// IOnAppListener.aidl
package com.kingsun.custom;
import com.kingsun.custom.BookUseInfo;

// 提供给三方使用的 APP 交互监听接口
interface IOnAppListener {

    // 进入课本回调
    void onEnterBookCallback(in BookUseInfo bookUseInfo);

    // 关闭应用回调
    void onExitAppCallback(in BookUseInfo bookUseInfo);

    // 书本使用时长回调
    void onBookUseTimeCallback(in BookUseInfo bookUseInfo);

    // 书页切换回调
    void onChangeBookPageCallback(in BookUseInfo bookUseInfo);

    // 打开书本资源
    void onOpenBookResourceCallback(in BookUseInfo bookUseInfo);

    // 评测
    void onEvaluateCallback(in BookUseInfo bookUseInfo);
}