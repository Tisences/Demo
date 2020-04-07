package com.cmccpoc.video;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;


import java.io.ByteArrayOutputStream;

/**
 * 给Camera onPreviewFrame回调数据加水印
 * 支持NV21数据格式 params.setPreviewFormat(ImageFormat.NV21)
 */

public class YuvWaterMark {
	public static final String TAG = YuvWaterMark.class.getSimpleName();

	static {
		System.loadLibrary("YuvWaterMark");
	}

	/**
	 * 初始化时间水印
	 *
	 * @param frameWidth  相机宽
	 * @param frameHeight 相机高
	 * @param rotation    旋转角度,0,90,180,270
	 */
	public static native void init(int frameWidth, int frameHeight, int rotation);


	/**
	 * 设置水印信息
	 *
	 * @param index       水印编号
	 * @param offX        位置X轴
	 * @param offY        位置Y轴
	 * @param mark_width  水印宽度
	 * @param mark_height 水印高度
	 * @param mark_value  水印资源
	 */

	public static native void setWaterMarkValueByte(int index, int offX, int offY, int mark_width, int mark_height, byte[] mark_value);

	public static native void resetWaterMarkValueByte(int index);

	/**
	 * 释放内存
	 */
	public static native void release();

	public static native void addMark(byte[] yuvInData, byte[] outYvuData);


	/**
	 * nv12 与nv21区别
	 * NV12: YYYYYYYY UVUV     =>YUV420SP
	 * NV21: YYYYYYYY VUVU     =>YUV420SP
	 * rgb 转 nv21
	 *
	 * @param argb
	 * @param width
	 * @param height
	 * @return
	 */
	public static native byte[] argbIntToNV21Byte(int[] argb, int width, int height);

	/**
	 * rgb 转nv12
	 *
	 * @param argb
	 * @param width
	 * @param height
	 * @return
	 */
	public static native byte[] argbIntToNV12Byte(int[] argb, int width, int height);

	/**
	 * rgb 转灰度 nv
	 * 也就是yuv 中只有 yyyy 没有uv 数据
	 *
	 * @param argb
	 * @param width
	 * @param height
	 * @return
	 */
	public static native byte[] argbIntToGrayNVByte(int[] argb, int width, int height);

	/**
	 * nv21 转 nv 12
	 *
	 * @param nv21Src  源数据
	 * @param nv12Dest 目标数组
	 * @param width    数组长度 len=width*height*3/2
	 * @param height
	 */
	public static native void nv21ToNv12(byte[] nv21Src, byte[] nv12Dest, int width, int height);

	/**
	 * @param bitmap cannot be used after call this function
	 * @param width  the width of bitmap
	 * @param height the height of bitmap
	 * @return return the NV21 byte array, length = width * height * 3 / 2
	 */
	public static byte[] bitmapToNV21(Bitmap bitmap, int width, int height) {
		int[] argb = new int[width * height];
		bitmap.getPixels(argb, 0, width, 0, 0, width, height);
		byte[] nv21 = argbIntToNV21Byte(argb, width, height);
		return nv21;
	}

	/**
	 * @param bitmap cannot be used after call this function
	 * @param width  the width of bitmap
	 * @param height the height of bitmap
	 * @return return the NV12 byte array, length = width * height * 3 / 2
	 */
	public static byte[] bitmapToNV12(Bitmap bitmap, int width, int height) {
		int[] argb = new int[width * height];
		bitmap.getPixels(argb, 0, width, 0, 0, width, height);
		byte[] nv12 = argbIntToNV12Byte(argb, width, height);
		return nv12;
	}

	/**
	 * @param bitmap cannot be used after call this function
	 * @param width  the width of bitmap
	 * @param height the height of bitmap
	 * @return return the NV12 byte array, length = width * height
	 */
	public static byte[] bitmapToGrayNV(Bitmap bitmap, int width, int height) {
		int[] argb = new int[width * height];
		bitmap.getPixels(argb, 0, width, 0, 0, width, height);
		byte[] nv12 = argbIntToGrayNVByte(argb, width, height);
		return nv12;
	}


	/**
	 * java 版，速度比c 慢
	 *
	 * @param nv21
	 * @param width
	 * @param height
	 */
	public static void NV21ToNV12(byte[] nv21, int width, int height) {
		if (nv21 == null) {
			return;
		}
		int framesize = width * height;
		int j = 0;
		int end = framesize + framesize / 2;
		byte temp = 0;
		for (j = framesize; j < end; j += 2)//u v
		{
			temp = nv21[j];
			nv21[j] = nv21[j + 1];
			nv21[j + 1] = temp;
		}
	}

	public static byte[] cameraFrameToArray(byte[] yuv, int w, int h) {
		return cameraFrameToArray(yuv, w, h, 100);
	}

	public static byte[] cameraFrameToArray(byte[] yuv, int w, int h, int quality) {
		YuvImage img = new YuvImage(yuv, ImageFormat.NV21,
				w, h, null);
		Rect rect = new Rect(0, 0, w, h);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		img.compressToJpeg(rect, quality, os);

		byte[] tmp = os.toByteArray();//裁剪后的人脸图
//		Utils.close(os);
		return tmp;
	}

	public static Bitmap fromText(String text, float textSize) {

		String[] splits = text.split("\n");

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setTextSize(textSize);
		paint.setTextAlign(Paint.Align.LEFT);
		paint.setColor(Color.WHITE);


		int width = 0;
		for (String value : splits) {
			width = Math.max(width, (int) paint.measureText(value));
		}

		Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		int height = (fm.descent - fm.ascent) + (int) textSize * (splits.length - 1);

		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.BLACK);
		for (int i = 0; i < splits.length; i++) {
			canvas.drawText(splits[i], 0, (fm.leading - fm.ascent) + textSize * i, paint);
		}
		canvas.save();

		return bitmap;
	}

	public static void addWaterMark(int index, int offX, int offY, String markText, int textSize) {
		if (index < 8) {
			Bitmap markBitmap = fromText(markText, textSize);
			byte[] markValue = bitmapToGrayNV(markBitmap, markBitmap.getWidth(), markBitmap.getHeight());
			setWaterMarkValueByte(index, offX, offY, markBitmap.getWidth(), markBitmap.getHeight(), markValue);
		} else {
			Log.e(TAG, "need index < 8 ,bug now index = " + index);
		}
	}
}
