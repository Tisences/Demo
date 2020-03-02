package com.vanzo.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.vanzo.demo.jni.YuvWaterMark;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 上报图片时，选择拍照上传时显示的自定义Camera控件
 *
 * @author Yao
 */
public class Video extends Activity implements OnClickListener,
		SurfaceHolder.Callback,
		TextureView.SurfaceTextureListener,
		MediaCodecCenter.MediaCodecCenterCallback,
		Camera.AutoFocusCallback,
		Camera.PreviewCallback {

	public static final String TAG = Video.class.getSimpleName();
	private ImageView startOrStopIcon;

	private Camera camera;
	private SurfaceView mSurfaceView;

	private boolean isEncoding = false;
	private boolean isRecording = false;
	private boolean isPrepare = false;
	private MediaCodecSaveControl saveControl;
	private MediaCodecCenter encodeCenter;
	private ExecutorService mExecutor;

	private static final int VIDEO_WIDTH = 1080;
	private static final int VIDEO_HEIGHT = 1920;
	private static final int VIDEO_FRAME = 20;
	private SimpleDateFormat mFormat;

	private static final int VIDEO_CUTOFF_DURATION = 60 * 1000;
	private static final int VIDEO_PRE_DURATION = 15;
	private Timer timer;

	@SuppressLint("HandlerLeak")
	private Handler curHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == 11) {
				cut();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.photo_camera);
		InitDataResource();
		requestPower();
		saveControl = new MediaCodecSaveControl(this, VIDEO_PRE_DURATION);
		encodeCenter = new MediaCodecCenter(this);
		encodeCenter.setCenterCallback(this);

	}

	private void InitDataResource() {
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, cameraInfo);
		Log.w(TAG, "camera orientation " + cameraInfo.orientation);

		YuvWaterMark.init(VIDEO_HEIGHT, VIDEO_WIDTH, 90);
		startOrStopIcon = findViewById(R.id.start_or_stop_icon);
		mSurfaceView = findViewById(R.id.surface);
		mSurfaceView.getHolder().addCallback(this);
		startOrStopIcon.setOnClickListener(this);
		startOrStopIcon.setImageResource(isRecording ? R.drawable.ic_video_session_stop : R.drawable.ic_video_session_start);
		findViewById(R.id.cut_icon).setOnClickListener(this);
		long start = SystemClock.uptimeMillis();

		YuvWaterMark.addWaterMark(1, 100, 100, "上海凡卓通讯科技股份有限公司", 20);
		YuvWaterMark.addWaterMark(2, 100, 130, "上海市闵行区秀文路898号", 20);

		long stop = SystemClock.uptimeMillis();
		Log.w("zts", "init water mark use time " + (stop - start));
		String pattern = "yyyy-MM-dd HH:mm:ss";//日期格式
		mFormat = new SimpleDateFormat(pattern, Locale.CHINA);
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				YuvWaterMark.addWaterMark(0, 100, 160, mFormat.format(new Date()), 20);
			}
		}, 0, 1000);
	}


	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.start_or_stop_icon) {
			if (isPrepare) {
				if (isRecording) {
					isRecording = false;
					stopRecording();
				} else {
					isRecording = true;
					startRecording();
				}
			}
			startOrStopIcon.setImageResource(isRecording ? R.drawable.ic_video_session_stop : R.drawable.ic_video_session_start);
		} else if (v.getId() == R.id.cut_icon) {
			cut();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
			stopEncoder();
		}
		if (timer != null) {
			timer.cancel();
		}
		YuvWaterMark.release();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.w(TAG, "surfaceCreated");
		try {
			if (camera == null)
				camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
			if (camera != null) {
				camera.setPreviewDisplay(holder);
				Parameters params = camera.getParameters();
				params.setPreviewFormat(ImageFormat.NV21);// 图片格式

				List<Camera.Size> previews = params.getSupportedPreviewSizes();
				for (Camera.Size size : previews) {
					Log.i(TAG, "support size " + size.width + "*" + size.height);
				}
				List<int[]> rangs = params.getSupportedPreviewFpsRange();
				for (int[] rang : rangs) {
					Log.i(TAG, "support rang " + Arrays.toString(rang));
				}
				params.setPreviewSize(VIDEO_HEIGHT, VIDEO_WIDTH);
				params.setPreviewFpsRange(VIDEO_FRAME * 1000, VIDEO_FRAME * 1000);
				params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
				camera.setDisplayOrientation(90);
				camera.setParameters(params);
				camera.setPreviewCallback(this);
				camera.startPreview();
				camera.autoFocus(this);
				camera.cancelAutoFocus();
				startEncoder();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.w(TAG, "surfaceDestroyed");
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
			stopEncoder();
		}
	}

	public void requestPower() {
		//判断是否已经赋予权限
		if (ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA)
				!= PackageManager.PERMISSION_GRANTED) {
			//如果应用之前请求过此权限但用户拒绝了请求，此方法将返回 true。
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.CAMERA)) {//这里可以写个对话框之类的项向用户解释为什么要申请权限，并在对话框的确认键后续再次申请权限
				//申请权限，字符串数组内是一个或多个要申请的权限，1是申请权限结果的返回参数，在onRequestPermissionsResult可以得知申请结果
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1);
			}
		}
	}


	private void startEncoder() {
		if (mExecutor == null) {
			mExecutor = Executors.newSingleThreadExecutor();
		}
		try {
			encodeCenter.init(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FRAME, VIDEO_FRAME * VIDEO_WIDTH * VIDEO_HEIGHT / 4);
		} catch (IOException e) {
			e.printStackTrace();
		}
		isEncoding = true;
		mExecutor.execute(recordRunnable);
	}

	private void stopEncoder() {
		isEncoding = false;
		curHandler.removeMessages(11);
	}

	private void startRecording() {
		if (saveControl != null) {
			isRecording = true;
			saveControl.startSaveVideo();
			curHandler.sendEmptyMessageDelayed(11, VIDEO_CUTOFF_DURATION);
		}
	}

	private void stopRecording() {
		if (saveControl != null) {
			isRecording = false;
			saveControl.stopSaveVideo();
		}
	}

	private void cut() {
		if (saveControl != null && isRecording) {
			saveControl.cutOffMediaSave();
			curHandler.sendEmptyMessageDelayed(11, VIDEO_CUTOFF_DURATION);
		}
	}

	private Runnable recordRunnable = new Runnable() {

		@Override
		public void run() {
			try {
				encodeCenter.startCodec();
				while (isEncoding) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				encodeCenter.stopCodec();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public void onPrepare(MediaFormat audioFormat, MediaFormat videoFormat) {
		if (saveControl != null) {
			saveControl.setMediaFormat(audioFormat, videoFormat);
			isPrepare = true;
		}
	}

	private long lastVideoFrameCodedMillis = 0;

	@Override
	public void onVideoFrameCoded(ByteBuffer buffer, MediaCodec.BufferInfo info) {
		if (saveControl != null && isEncoding) {
//			Log.w(TAG, "onVideoFrameCoded " + (System.currentTimeMillis() - lastVideoFrameCodedMillis));
//			lastVideoFrameCodedMillis = System.currentTimeMillis();
			byte[] temp = new byte[info.size];
			buffer.get(temp, info.offset, info.size);
			MediaCodec.BufferInfo tempInfo = new MediaCodec.BufferInfo();
			tempInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags);
			saveControl.addVideoFrameData(temp, tempInfo);
		}
	}

	private long lastAudioFrameCodedMillis = 0;

	@Override
	public void onAudioFrameCoded(ByteBuffer buffer, MediaCodec.BufferInfo info) {
		if (saveControl != null && isEncoding) {
//			Log.w(TAG, "onAudioFrameCoded " + (System.currentTimeMillis() - lastAudioFrameCodedMillis));
//			lastAudioFrameCodedMillis = System.currentTimeMillis();
			byte[] temp = new byte[info.size];
			buffer.get(temp, info.offset, info.size);
			MediaCodec.BufferInfo tempInfo = new MediaCodec.BufferInfo();
			tempInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags);
			saveControl.addAudioFrameData(temp, tempInfo);
		}
	}

	private long lastPreviewFrameMillis = 0;

	private long addMarkUseMillis = 0;
	private int count = 0;

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		if (data != null && encodeCenter != null && isEncoding) {
//			Log.w(TAG, "onPreviewFrame " + (System.currentTimeMillis() - lastPreviewFrameMillis));
//			lastPreviewFrameMillis = System.currentTimeMillis();
			byte[] nv12 = new byte[data.length];
			long start = SystemClock.uptimeMillis();
			YuvWaterMark.addMark(data, nv12);
			long time = SystemClock.uptimeMillis() - start;
			addMarkUseMillis += time;
			count++;
			Log.w(TAG, "add water mark time=" + time + " ms " + addMarkUseMillis / count);
			encodeCenter.feedVideoFrameData(nv12);
		}
	}

	private static void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
		if (nv21 == null || nv12 == null) return;
		int frameSize = width * height;
		System.arraycopy(nv21, 0, nv12, 0, frameSize);
		for (int j = 0; j < frameSize / 2; j += 2) {
			nv12[frameSize + j - 1] = nv21[j + frameSize];
		}
		for (int j = 0; j < frameSize / 2; j += 2) {
			nv12[frameSize + j] = nv21[j + frameSize - 1];
		}
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		Log.w(TAG, "surfaceCreated");
		try {
			if (camera == null)
				camera = Camera.open();
			if (camera != null) {
				camera.setPreviewTexture(surface);
				Parameters params = camera.getParameters();
				params.setPreviewFormat(ImageFormat.NV21);// 图片格式
				params.setPreviewSize(1920, 1080);
				params.setPreviewFpsRange(VIDEO_FRAME, VIDEO_FRAME);
				camera.setParameters(params);
				camera.setPreviewCallback(this);
				camera.startPreview();
				startEncoder();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.w(TAG, "surfaceDestroyed");
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
			stopEncoder();
		}
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {

	}

	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.w(TAG, "onAutoFocus " + success);
//		Camera.Parameters params = camera.getParameters(); //有时对焦失败重新设置对焦模式
//		params.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//		camera.setParameters(params);
	}

//	public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
//		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
//		int i = 0;
//		for (int x = 0; x < imageWidth; x++) {
//			for (int y = imageHeight - 1; y >= 0; y--) {
//				yuv[i] = data[y * imageWidth + x];
//				i++;
//			}
//		}
//		i = imageWidth * imageHeight * 3 / 2 - 1;
//		for (int x = imageWidth - 1; x > 0; x = x - 2) {
//			for (int y = 0; y < imageHeight / 2; y++) {
//				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
//				i--;
//				yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
//						+ (x - 1)];
//				i--;
//			}
//		}
//		return yuv;
//	}
//
//	private static byte[] rotateYUV420Degree180(byte[] data, int imageWidth, int imageHeight) {
//		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
//		int i = 0;
//		int count = 0;
//		for (i = imageWidth * imageHeight - 1; i >= 0; i--) {
//			yuv[count] = data[i];
//			count++;
//		}
//		i = imageWidth * imageHeight * 3 / 2 - 1;
//		for (i = imageWidth * imageHeight * 3 / 2 - 1; i >= imageWidth
//				* imageHeight; i -= 2) {
//			yuv[count++] = data[i - 1];
//			yuv[count++] = data[i];
//		}
//		return yuv;
//	}
//
//	public static byte[] rotateYUV420Degree270(byte[] data, int imageWidth,
//											   int imageHeight) {
//		byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
//		int nWidth = 0, nHeight = 0;
//		int wh = 0;
//		int uvHeight = 0;
//		if (imageWidth != nWidth || imageHeight != nHeight) {
//			nWidth = imageWidth;
//			nHeight = imageHeight;
//			wh = imageWidth * imageHeight;
//			uvHeight = imageHeight >> 1;// uvHeight = height / 2
//		}
//
//		int k = 0;
//		for (int i = 0; i < imageWidth; i++) {
//			int nPos = 0;
//			for (int j = 0; j < imageHeight; j++) {
//				yuv[k] = data[nPos + i];
//				k++;
//				nPos += imageWidth;
//			}
//		}
//		for (int i = 0; i < imageWidth; i += 2) {
//			int nPos = wh;
//			for (int j = 0; j < uvHeight; j++) {
//				yuv[k] = data[nPos + i];
//				yuv[k + 1] = data[nPos + i + 1];
//				k += 2;
//				nPos += imageWidth;
//			}
//		}
//		return rotateYUV420Degree180(rotateYUV420Degree90(data, imageWidth, imageHeight), imageWidth, imageHeight);
//	}
}
