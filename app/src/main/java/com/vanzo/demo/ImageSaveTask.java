package com.vanzo.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class ImageSaveTask extends AsyncTask {

	public static final String TAG = ImageSaveTask.class.getSimpleName();
	private byte[] data;
	private String fileName;
	private int id;

	public ImageSaveTask(byte[] data, String fileName, int id) {
		this.data = data;
		this.fileName = fileName;
		this.id = id;
	}

	@Override
	protected Object doInBackground(Object[] objects) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inMutable = true;
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
//			Bitmap bitmap = AddTimeWatermark(BitmapFactory.decodeByteArray(data, 0,data.length));
		long time = System.currentTimeMillis();
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
		Log.w(TAG, id + " AddTimeWatermark use " + (System.currentTimeMillis() - time) + "ms");
		bitmap = AddTimeWatermark(bitmap);
		if (null == bitmap) {
			return null;
		}
		File file = new File(fileName);
		BufferedOutputStream bos;
		try {
			long time1 = System.currentTimeMillis();
			bos = new BufferedOutputStream(new FileOutputStream(file));
			bitmap.compress(Bitmap.CompressFormat.JPEG, 50, bos);
			bos.flush();
			bos.close(); // 关闭输出流
			Log.w(TAG, id + " stopEncode saving bitmap use " + (System.currentTimeMillis() - time1) + "ms");

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.gc();
		}
		return null;
	}

	private static Bitmap AddTimeWatermark(Bitmap mBitmap) {
		//获取原始图片与水印图片的宽与高
		int mBitmapWidth = mBitmap.getWidth();
		int mBitmapHeight = mBitmap.getHeight();
		Canvas mCanvas = new Canvas(mBitmap);
		//向位图中开始画入MBitmap原始图片
		mCanvas.drawBitmap(mBitmap, 0, 0, null);
		//添加文字
		Paint mPaint = new Paint();
		String time = System.currentTimeMillis() + "";
		mPaint.setColor(Color.WHITE);
		mPaint.setTextSize(30);
		mCanvas.drawText(time, 50, mBitmapHeight - 140, mPaint);
		mCanvas.drawText("1234567890", 50, mBitmapHeight - 100, mPaint);
		mCanvas.save(Canvas.ALL_SAVE_FLAG);
		mCanvas.restore();
		return mBitmap;
	}
}
