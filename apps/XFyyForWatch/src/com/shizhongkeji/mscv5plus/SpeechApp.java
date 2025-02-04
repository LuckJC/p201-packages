package com.shizhongkeji.mscv5plus;

import android.app.Activity;
import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class SpeechApp extends Application {
	static Activity context;

	@Override
	public void onCreate() {
		// 应用程序入口处调用,避免手机内存过小，杀死后台进程,造成SpeechUtility对象为null
		// 设置你申请的应用appid
		StringBuffer param = new StringBuffer();
		param.append("appid=" + getString(R.string.app_id));
		param.append(",");
		// 设置使用v5+
		param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
		SpeechUtility.createUtility(SpeechApp.this, param.toString());
		super.onCreate();
	}
}
