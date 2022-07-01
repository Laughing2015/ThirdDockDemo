// IOnAppListener.aidl
package com.kingsun.custom;
import com.kingsun.custom.BookUseInfo;

// 提供给三方使用的 APP 交互监听接口
interface IOnAppListener {
    // 书本使用信息回调
    void onBookUseInfoCallback(in BookUseInfo bookUseInfo);
}