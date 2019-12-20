package com.vanzo.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.TextCallBack;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.lang.reflect.Method;

public class UpgradeActivity extends Activity {

	public static final String TAG = "UpgradeActivity";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);

//		findViewById(R.id.update_self).setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				updateSelf();
//			}
//		});
//		TextView version = findViewById(R.id.version_info);
//		version.setText("versionName:" + getVerName(this) + " | versionCode:" + getVersionCode(this));

	}


	/**
	 * 获取当前本地apk的版本
	 *
	 * @param mContext
	 * @return
	 */
	public static int getVersionCode(Context mContext) {
		int versionCode = 0;
		try {
			//获取软件版本号，对应AndroidManifest.xml下android:versionCode
			versionCode = mContext.getPackageManager().
					getPackageInfo(mContext.getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return versionCode;
	}

	/**
	 * 获取版本号名称
	 *
	 * @param context 上下文
	 * @return
	 */
	public static String getVerName(Context context) {
		String verName = "";
		try {
			verName = context.getPackageManager().
					getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return verName;

	}

	private void updateSelf() {
		Log.w(TAG, "update self");
		install("mnt/sdcard/Download/Demo.apk");
	}

	public void install(String fileName) {
		try {
			Uri uri = Uri.fromFile(new File(fileName));
			// 通过Java反射机制获取android.os.ServiceManager
			Class<?> clazz = Class.forName("android.os.ServiceManager");
			Method method = clazz.getMethod("getService", String.class);
			IBinder iBinder = (IBinder) method.invoke(null, "package");
			IPackageManager ipm = IPackageManager.Stub.asInterface(iBinder);
			ipm.installPackageAsUser(uri.getPath(),
					new PackageInstallObserver(),
					2,
					getPackageName(),
					0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 用于显示结果
	class PackageInstallObserver extends IPackageInstallObserver2.Stub {

		@Override
		public void onUserActionRequired(Intent intent) throws RemoteException {
			Log.w(TAG, "onUserActionRequired");
		}

		@Override
		public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) throws RemoteException {
			Log.w(TAG, "onPackageInstalled");
		}
	}
}
