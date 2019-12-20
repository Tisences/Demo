//package com.vanzo.demo;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.graphics.Bitmap;
//import android.graphics.Color;
//import android.hardware.Camera;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Message;
//import android.os.PowerManager;
//import android.os.RemoteException;
//import android.os.SystemClock;
//import android.provider.MediaStore;
//import android.provider.Settings;
//import android.text.format.Formatter;
//import android.util.Log;
//import android.view.KeyEvent;
//import android.view.MotionEvent;
//import android.view.SurfaceView;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Chronometer;
//import android.widget.ImageView;
//import android.widget.RelativeLayout;
//import android.widget.TextView;
//
//import java.io.File;
//
//
//public class BitrateActivity extends Activity implements View.OnClickListener,
//		MediaCodecSaveControl.CameraPreviewCallback,
//		Chronometer.OnChronometerTickListener,
//		MediaCodecSaveControl.VideoRecordCallback,
//		MediaCodecSaveControl.TakePictureCallback,
//		MediaCodecSaveControl.CameraSwitchCallback {
//
//	private static final String TAG = BitrateActivity.class.getSimpleName();
//	private static final String MOBLE_LAW_STATUS = "moble_law_status";
//	private static final String MOBLE_LAW_RECORDING_STATUS = "moble_law_recording_status";
//
//
//	private static final int DEFAULT_CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_BACK;
//	public static final long MIN_STORAGE_SPACE = 100000000;
//
//	private int cameraId = DEFAULT_CAMERA_ID;
//
//	private static final int STATE_IDLE = 11;
//	private static final int STATE_PREPARE = 12;
//	private static final int STATE_SAVING_PHOTO = 14;
//	private static final int STATE_TAKING_VIDEO = 15;
//	private static final int STATE_SAVING_VIDEO = 16;
//
//	private SurfaceView mSurfaceView;
//	private ImageView mTakePhotoOrVideo;
//
//	private Chronometer mCurrentChronometer;
//	private TextView mTotalSpace;
//
//	private MediaCodecSaveControl cameraRecord;
//	private PowerManager.WakeLock mWakeLock;
//	private int state;
//	private int rate;
//
//	@SuppressLint("InvalidWakeLockTag")
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_mobile_law);
//		state = STATE_IDLE;
//		PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//		if (powerManager != null) {
//			mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MobileLaw");
//		}
//		rate = getIntent().getIntExtra("rate",9000000);
//		initView();
//		refreshSpace();
//		refreshUI();
//	}
//
//	private void initView() {
//		mSurfaceView = (SurfaceView) findViewById(R.id.law_surface_view);
//		mTakePhotoOrVideo = (ImageView) findViewById(R.id.law_take_photo_and_video);
//		mTakePhotoOrVideo.setOnClickListener(this);
//		mCurrentChronometer = (Chronometer) findViewById(R.id.law_current_chronometer);
//		mTotalSpace = (TextView) findViewById(R.id.law_total_space);
//		cameraRecord = new MediaCodecSaveControl(this, cameraId, mSurfaceView);
//		cameraRecord.setCameraPreviewCallback(this);
//		cameraRecord.setCameraSwitchCallback(this);
//		cameraRecord.setTakePictureCallback(this);
//		cameraRecord.setVideoRecordCallback(this);
//	}
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		cameraRecord.init(300,rate);
//	}
//
//	@Override
//	protected void onPause() {
//		super.onPause();
//		if (state == STATE_TAKING_VIDEO) {
//			cameraRecord.stopRecord();
//		}
//	}
//
//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
//	}
//
//	public void refreshSpace() {
//		mCurrentChronometer.setOnChronometerTickListener(this);
//	}
//
//	@Override
//	public void onChronometerTick(Chronometer chronometer) {
//		long usableSpace = getStorageSize();
//		mTotalSpace.setText("" + Formatter.formatFileSize(this, usableSpace));
//		if (usableSpace <= MIN_STORAGE_SPACE) {
//			if (state == STATE_TAKING_VIDEO) {
//				cameraRecord.stopRecord();
//			}
//		}
//		// TODO: 19-8-6 led control
//	}
//
//	private long getStorageSize() {
//		long usableSpace = 0;
//		usableSpace = Environment.getExternalStorageDirectory().getFreeSpace();
//		return usableSpace;
//	}
//
//	@Override
//	public void onClick(View v) {
//		switch (v.getId()) {
//			case R.id.law_take_photo_and_video:
//				takeVideo();
//				break;
//		}
//	}
//
//	private void refreshUI() {
//		switch (state) {
//			case STATE_IDLE:
//				mTakePhotoOrVideo.setEnabled(false);
//				break;
//			case STATE_PREPARE:
//				mTakePhotoOrVideo.setEnabled(true);
//				mTakePhotoOrVideo.setImageResource(R.drawable.ic_video_session_start);
//				mCurrentChronometer.setTextColor(Color.WHITE);
//				mCurrentChronometer.stopEncode();
//				break;
//			case STATE_SAVING_PHOTO:
//				break;
//			case STATE_TAKING_VIDEO:
//				mTakePhotoOrVideo.setEnabled(true);
//				mTakePhotoOrVideo.setImageResource(R.drawable.ic_video_session_stop);
//				mCurrentChronometer.setBase(SystemClock.elapsedRealtime());
//				mCurrentChronometer.setTextColor(Color.RED);
//				mCurrentChronometer.startEncode();
//				break;
//			case STATE_SAVING_VIDEO:
//				break;
//		}
//	}
//
//	private void takeVideo() {
//		if (state == STATE_PREPARE) {
//			if (getStorageSize() <= MIN_STORAGE_SPACE) {
//			} else {
//				cameraRecord.startRecord();
//			}
//		} else if (state == STATE_TAKING_VIDEO) {
//			cameraRecord.stopRecord();
//		}
//	}
//
//	@Override
//	public void onCameraSwitchComplete(Camera camera, int cameraId) {
//		state = STATE_PREPARE;
//		refreshUI();
//	}
//
//	@Override
//	public void onCameraPreviewStart(int width, int height) {
//		state = STATE_PREPARE;
//		if (width > height) {
//			changeVideoSize(width, height);
//		} else {
//			changeVideoSize(height, width);
//		}
//		refreshUI();
//	}
//
//	@Override
//	public void onCameraPreviewStop() {
//		state = STATE_IDLE;
//		refreshUI();
//	}
//
//	@Override
//	public void onPreviewFrame(byte[] data, Camera camera) {
//	}
//
//
//	@Override
//	public void onTakePictureComplete(String path) {
//
//	}
//
//	@Override
//	public void onVideoRecordStart() {
//		if (mWakeLock != null) {
//			mWakeLock.acquire();
//		}
//	}
//
//	@Override
//	public void onVideoSegStart() {
//		state = STATE_TAKING_VIDEO;
//		refreshUI();
//	}
//
//	@Override
//	public void onVideoSegStop() {
//		state = STATE_SAVING_VIDEO;
//		refreshUI();
//	}
//
//	@Override
//	public void onVideoRecordStop() {
//		if (mWakeLock != null) {
//			mWakeLock.release();
//		}
//	}
//
//	@Override
//	public void onVideoSaveComplete(String filePath) {
//		state = STATE_PREPARE;
//		refreshUI();
//		Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
//		Uri uri = Uri.fromFile(new File(filePath));// 固定写法
//		intent.setData(uri);
//		sendBroadcast(intent);
//	}
//
//	private void changeVideoSize(int mvWidth, int mvHeight) {
//		//根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
//		float max;
//		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//		assert wm != null;
//		int width = wm.getDefaultDisplay().getWidth();
//
//		int height = wm.getDefaultDisplay().getHeight();
//
//		//竖屏模式下按视频宽度计算放大倍数值
//		max = Math.max((float) mvWidth / (float) width, (float) mvHeight / (float) height);
//
//		//视频宽高分别/最大倍数值 计算出放大后的视频尺寸
//		mvWidth = (int) Math.ceil((float) mvWidth / max);
//		mvHeight = (int) Math.ceil((float) mvHeight / max);
//
//		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mvWidth, mvHeight);
//		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
//		mSurfaceView.setLayoutParams(params);
//	}
//}
