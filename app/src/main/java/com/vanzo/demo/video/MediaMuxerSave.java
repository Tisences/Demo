package com.vanzo.demo.video;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaMuxerSave {
	private Context context;

	private int audioTrack = -1;
	private int videoTrack = -1;
	private MediaMuxer muxer;
	private String path;
	private boolean isSaving;
	private boolean isDeleted;
	private long startSavingMillis = 0;

	MediaMuxerSave(Context context, String path) {
		this.context = context;
		if (path == null || path.isEmpty()) {
			return;
		}
		this.path = path;
		try {
			muxer = new MediaMuxer(this.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void start(MediaFormat audioFormat, MediaFormat videoFormat) {
		if (muxer != null) {
			audioTrack = muxer.addTrack(audioFormat);
			videoTrack = muxer.addTrack(videoFormat);
			if (audioTrack >= 0 && videoTrack >= 0) {
				muxer.start();
				startSavingMillis = System.currentTimeMillis();
				isSaving = true;
			}
		}

	}

	public void writeAudioTrackData(ByteBuffer buffer, MediaCodec.BufferInfo info) {
		if (muxer != null && isSaving) {
			try {
				muxer.writeSampleData(audioTrack, buffer, info);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void writeVideoTrackData(ByteBuffer buffer, MediaCodec.BufferInfo info) {
		if (muxer != null && isSaving) {
			try {
				muxer.writeSampleData(videoTrack, buffer, info);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		audioTrack = -1;
		videoTrack = -1;
		isSaving = false;
		if (muxer != null) {
			muxer.stop();
			if (System.currentTimeMillis() - startSavingMillis <= 3000) {
				isDeleted = delete();
			}
		}
	}

	public void release() {
		if (muxer != null) {
			muxer.release();
			muxer = null;
			if (!isDeleted) {
				scanFile(context, path);
			}
		}
	}

	public boolean delete() {
		File file = new File(path);
		if (file.exists()) {
			return file.delete();
		}
		return false;
	}

	public static void scanFile(Context context, String filePath) {
		if (filePath == null || filePath.isEmpty()) {
			return;
		}
		Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		Uri uri = Uri.fromFile(new File(filePath));// 固定写法
		intent.setData(uri);
		context.sendBroadcast(intent);
	}
}