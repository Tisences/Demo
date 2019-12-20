package com.vanzo.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 上报图片时，选择拍照上传时显示的自定义Camera控件
 *
 * @author Yao
 */
public class VideoCap extends Activity implements OnClickListener,
		SurfaceHolder.Callback,
		MediaCodecCenter.MediaCodecCenterCallback,
		Camera.PreviewCallback {

	public static final String TAG = VideoCap.class.getSimpleName();
	private ImageView startOrStopIcon;

	private Camera camera;
	private SurfaceView mSurfaceView;

	private boolean isEncoding = false;
	private boolean isRecording = false;
	private boolean isPrepare = false;
	private MediaCodecSaveControl saveControl;
	private MediaCodecCenter encodeCenter;
	private ExecutorService mExecutor;

	private static final int VIDEO_WIDTH = 1920;
	private static final int VIDEO_HEIGHT = 1080;
	private static final int VIDEO_FRAME = 10;

	private static final int VIDEO_CUTOFF_DURATION = 60 * 1000;
	private static final int VIDEO_PRE_DURATION = 15;

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
		startOrStopIcon = findViewById(R.id.start_or_stop_icon);
		mSurfaceView = findViewById(R.id.surface);
		mSurfaceView.getHolder().addCallback(this);
		startOrStopIcon.setOnClickListener(this);
		startOrStopIcon.setImageResource(isRecording ? R.drawable.ic_video_session_stop : R.drawable.ic_video_session_start);
		findViewById(R.id.cut_icon).setOnClickListener(this);
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
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if (camera == null)
				camera = Camera.open();
			if (camera != null) {
				camera.setPreviewDisplay(holder);
				Parameters params = camera.getParameters();
				params.setPreviewFormat(ImageFormat.NV21);// 图片格式
				params.setPreviewSize(1920, 1080);
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
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
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
			encodeCenter.init(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FRAME, VIDEO_FRAME * VIDEO_WIDTH * VIDEO_HEIGHT / 10);
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

	@Override
	public void onVideoFrameCoded(ByteBuffer buffer, MediaCodec.BufferInfo info) {
		if (saveControl != null && isEncoding) {
			MediaCodec.BufferInfo tempInfo = new MediaCodec.BufferInfo();
			tempInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags);
			byte[] temp = new byte[tempInfo.size];
			buffer.get(temp, tempInfo.offset, tempInfo.size);
			saveControl.addVideoFrameData(temp, tempInfo);
		}
	}

	@Override
	public void onAudioFrameCoded(ByteBuffer buffer, MediaCodec.BufferInfo info) {
		if (saveControl != null && isEncoding) {
			byte[] temp = new byte[info.size];
			buffer.get(temp);
			MediaCodec.BufferInfo tempInfo = new MediaCodec.BufferInfo();
			tempInfo.set(info.offset, info.size, info.presentationTimeUs, info.flags);
			saveControl.addAudioFrameData(temp, tempInfo);
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (data != null && encodeCenter != null && isEncoding) {
			byte[] nv12 = new byte[data.length];
			NV21ToNV12(data, nv12, VIDEO_WIDTH, VIDEO_HEIGHT);
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
}
