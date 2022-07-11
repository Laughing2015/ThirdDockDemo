// IAppManager.aidl
package com.kingsun.custom;

import com.kingsun.custom.IOnAppListener;
import com.kingsun.custom.BookUseInfo;

//  APP 交互接口

interface IAppManager {

     // 关闭应用
     void exitApp();

     // 显示隐藏设置按钮
     void toggleSettingsButton(boolean visible);

     // 显示隐藏评测按钮
     void toggleEvaluationButton(boolean visible);

     // 注册三方客户端监听
     void registerOnAppListener(IOnAppListener listener);

     // 取消三方客户端监听
     void unRegisterOnAppListener(IOnAppListener listener);

}