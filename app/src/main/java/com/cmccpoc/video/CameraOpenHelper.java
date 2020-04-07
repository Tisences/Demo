package com.cmccpoc.video;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * camera2 工具类
 */

public class CameraOpenHelper {

	public static final String TAG = CameraOpenHelper.class.getSimpleName();

	private Context context;
	private SurfaceView surfaceView;
	private Size previewSize;
	private String cameraId;
	CameraManager cameraManager;
	CameraDevice cameraDevice;
	CameraCaptureSession captureSession;

	private ImageReader imageReader;
	private Surface surface;

	private HandlerThread mBackgroundThread;

	private Handler mBackgroundHandler;
	private PreviewFrameCallback frameCallback;

	public void setFrameCallback(PreviewFrameCallback frameCallback) {
		this.frameCallback = frameCallback;
	}

	private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
		@Override
		public void onOpened(@NonNull CameraDevice camera) {
			cameraDevice = camera;
			createCameraPreviewSession();
		}

		@Override
		public void onDisconnected(@NonNull CameraDevice camera) {
			cameraDevice.close();
			cameraDevice = null;
		}

		@Override
		public void onError(@NonNull CameraDevice camera, int error) {
			cameraDevice.close();
			cameraDevice = null;
		}
	};

	private ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
		@Override
		public void onImageAvailable(ImageReader reader) {
			Image image = reader.acquireNextImage();
			ByteBuffer buffer = image.getPlanes()[0].getBuffer();
			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			Log.w(TAG, "onImageAvailable " + data.length);
//			if (frameCallback != null) {
//				frameCallback.onFrame(data);
//			}
			image.close();
		}
	};

	public CameraOpenHelper(Context context, SurfaceView surfaceView, Size previewSize) {
		this.context = context;
		this.surfaceView = surfaceView;
		this.previewSize = previewSize;
		startBackgroundThread();
		try {
			setCameraId();
		} catch (Exception e) {
			e.printStackTrace();
		}
		setImageReader();
	}

	private void startBackgroundThread() {
		mBackgroundThread = new HandlerThread(CameraOpenHelper.class.getSimpleName());
		mBackgroundThread.start();
		mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
	}

	private void stopBackgroundThread() {
		mBackgroundThread.quitSafely();
		try {
			mBackgroundThread.join();
			mBackgroundThread = null;
			mBackgroundHandler = null;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void initSurface() {
		Point outSize = new Point();
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		windowManager.getDefaultDisplay().getRealSize(outSize);
		int width = outSize.x;
		int height = width * 16 / 9;
		Log.w(TAG, "init surface " + width + "*" + height);
		ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
		layoutParams.width = width;
		layoutParams.height = height;
		surfaceView.setLayoutParams(layoutParams);
		surface = surfaceView.getHolder().getSurface();
	}

	private void setCameraId() throws Exception {
		cameraManager = context.getSystemService(CameraManager.class);
		if (cameraManager != null) {
			String[] cameraIds = cameraManager.getCameraIdList();
			for (String tempCameraId : cameraIds) {
				CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(tempCameraId);
				Integer face = characteristics.get(CameraCharacteristics.LENS_FACING);
				if (face != null && face == CameraCharacteristics.LENS_FACING_BACK) {
					cameraId = tempCameraId;
					StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
					if (map != null) {
						Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
								new CompareSizesByArea());
						Log.w(TAG, "camera id " + cameraId + " largest size " + largest);
					}
					break;
				}
			}
			if (cameraId == null) {
				throw new Exception("no back camera");
			}
		}
	}

	private void setImageReader() {
		imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 1);
		imageReader.setOnImageAvailableListener(imageAvailableListener, mBackgroundHandler);
	}

	public void openCamera() {
		initSurface();
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}
		try {
			cameraManager.openCamera(cameraId, stateCallback, mBackgroundHandler);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	public void closeCamera() {

		if (captureSession != null) {
			try {
				captureSession.stopRepeating();
			} catch (CameraAccessException e) {
				e.printStackTrace();
			} finally {
				captureSession.close();
				captureSession = null;
				if (cameraDevice != null) {
					cameraDevice.close();
					cameraDevice = null;
				}
				if (imageReader != null) {
					imageReader.close();
					imageReader = null;
				}
				stopBackgroundThread();
			}
		}
	}

	private CaptureRequest.Builder previewBuilder;

	private void createCameraPreviewSession() {
		try {
			previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
			previewBuilder.addTarget(surface);
			previewBuilder.addTarget(imageReader.getSurface());

			cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {

				@Override
				public void onConfigured(@NonNull CameraCaptureSession session) {
					captureSession = session;
					CaptureRequest previewRequest = previewBuilder.build();
					try {
						captureSession.setRepeatingRequest(previewRequest, null, mBackgroundHandler);
					} catch (CameraAccessException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession session) {

				}
			}, null);
		} catch (CameraAccessException e) {
			e.printStackTrace();
		}
	}

	public interface PreviewFrameCallback {
		void onFrame(byte[] data);
	}

	/**
	 * Compares two {@code Size}s based on their areas.
	 */
	static class CompareSizesByArea implements Comparator<Size> {

		@Override
		public int compare(Size lhs, Size rhs) {
			// We cast here to ensure the multiplications won't overflow
			return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
					(long) rhs.getWidth() * rhs.getHeight());
		}

	}
}
