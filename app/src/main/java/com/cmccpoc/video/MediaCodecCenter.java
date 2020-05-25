package com.cmccpoc.video;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.SystemClock;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 编码器 编码视频流和音频流
 * MediaCodec
 */

public class MediaCodecCenter {


	public static final String TAG = MediaCodecCenter.class.getSimpleName();
	private final Object LOCK = new Object();

	private Context context;

	private static final String AUDIO_MIME = MediaFormat.MIMETYPE_AUDIO_AAC;   //音频编码格式AAC
	private static final String VIDEO_MIME = MediaFormat.MIMETYPE_VIDEO_AVC;   //视频编码格式AVC
	private MediaCodec audioMediaCodec;		//编码器，用于音频编码
	private MediaCodec videoMediaCodec;		//编码器，用于视频编码

	private Thread mAudioThread;
	private Thread mVideoThread;

	private AudioRecord audioRecord;   		//采集音频
	private static final int FRAME_INTERVAL = 1;        //视频编码关键帧，1秒一关键帧
	private static final int AUDIO_RATE = 128000;   //音频编码的密钥比特率

	private int bufferSize;
	private long nanoTime;
	private long lastAudioPresentationTimeUs;
	private long lastVideoPresentationTimeUs;
	private int fpsTime;
	private boolean isEncoding = false;

	private byte[] newVideoFrameData;
	private byte[] lastVideoFrameData;
	private boolean hasNewData = false;

	private MediaFormat videoFormat;
	private MediaFormat audioFormat;

	private MediaCodecCenterCallback centerCallback = null;

	public MediaCodecCenter(Context context) {
		this.context = context;
	}

	public void setCenterCallback(MediaCodecCenterCallback centerCallback) {
		this.centerCallback = centerCallback;
		if (audioFormat != null && videoFormat != null) {
			centerCallback.onPrepare(audioFormat, videoFormat);
		}
	}

	public int init(int width, int height, int frameRate, int videoRate) throws IOException {
		int sampleRateInHz = 22050;
		int channelConfig = 1;
		int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
		int codecProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
		MediaFormat format = MediaFormat.createAudioFormat(AUDIO_MIME, sampleRateInHz, channelConfig);
		format.setInteger(MediaFormat.KEY_AAC_PROFILE, codecProfile);
		format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_RATE);
		audioMediaCodec = MediaCodec.createEncoderByType(AUDIO_MIME);
		audioMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat) * 2;
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig,
				audioFormat, bufferSize);

		fpsTime = 1000 / frameRate;
		MediaFormat videoFormat = MediaFormat.createVideoFormat(VIDEO_MIME, width, height);
		videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, videoRate);
		videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
		videoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
		videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, FRAME_INTERVAL);
		videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);

		videoMediaCodec = MediaCodec.createEncoderByType(VIDEO_MIME);
		videoMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		Bundle bundle = new Bundle();
		bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, videoRate);
		videoMediaCodec.setParameters(bundle);
		return 0;
	}


	public int startCodec() throws InterruptedException {
//		Log.d(TAG, "startCodec");
		nanoTime = System.nanoTime();
		lastAudioPresentationTimeUs = 0;
		synchronized (LOCK) {
			isEncoding = true;
			if (mAudioThread != null && mAudioThread.isAlive()) {
				mAudioThread.join();
			}
			if (mVideoThread != null && mVideoThread.isAlive()) {
				mVideoThread.join();
			}

			audioMediaCodec.start();
			audioRecord.startRecording();

			mAudioThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (isEncoding) {
						if (audioStep()) {
							break;
						}
						SystemClock.sleep(5);
					}
				}
			});
			mAudioThread.start();

			videoMediaCodec.start();
			mVideoThread = new Thread(new Runnable() {
				@Override
				public void run() {
					while (isEncoding) {
						long time = System.currentTimeMillis();
						if (newVideoFrameData != null) {
							if (videoStep(newVideoFrameData)) {
								break;
							}
						}
						long lt = System.currentTimeMillis() - time;
						if (fpsTime > lt) {
							try {
								Thread.sleep(fpsTime - lt);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			});
			mVideoThread.start();
		}
		return 0;
	}

	public void stopCodec() {
//		Log.d(TAG, "stopCodec");
		try {
			synchronized (LOCK) {
				isEncoding = false;

				mAudioThread.join();
				mVideoThread.join();


				audioRecord.stop();
				audioRecord.release();

				audioMediaCodec.stop();
				audioMediaCodec.release();
				audioMediaCodec = null;

				videoMediaCodec.stop();
				videoMediaCodec.release();
				videoMediaCodec = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void feedVideoFrameData(final byte[] data) {
		hasNewData = true;
		newVideoFrameData = data;
	}

	private ByteBuffer getInputBuffer(MediaCodec codec, int index) {
		return codec.getInputBuffer(index);
	}

	private ByteBuffer getOutputBuffer(MediaCodec codec, int index) {
		return codec.getOutputBuffer(index);
	}

	private boolean audioStep() {
		int inputIndex = audioMediaCodec.dequeueInputBuffer(-1);
		if (inputIndex >= 0) {
			ByteBuffer buffer = getInputBuffer(audioMediaCodec, inputIndex);
			buffer.clear();
			int length = audioRecord.read(buffer, bufferSize);
			audioMediaCodec.queueInputBuffer(inputIndex, 0, length, (System.nanoTime() - nanoTime) / 1000,
					isEncoding ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		}
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		int outputIndex;
		do {
			outputIndex = audioMediaCodec.dequeueOutputBuffer(info, 0);
			if (outputIndex >= 0) {
				ByteBuffer buffer = getOutputBuffer(audioMediaCodec, outputIndex);
				if (info.size > 0 && info.presentationTimeUs > 0 && centerCallback != null) {
					if (info.presentationTimeUs > lastAudioPresentationTimeUs) {
						lastAudioPresentationTimeUs = info.presentationTimeUs;
						centerCallback.onAudioFrameCoded(buffer, info);
					}
				}
				audioMediaCodec.releaseOutputBuffer(outputIndex, false);
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					return true;
				}
			} else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				audioFormat = audioMediaCodec.getOutputFormat();
				checkMediaFormatPrepare();
			}
		} while (outputIndex >= 0);
		return false;
	}

	private void checkMediaFormatPrepare() {
		if (audioFormat != null && videoFormat != null && centerCallback != null) {
			centerCallback.onPrepare(audioFormat, videoFormat);
		}
	}


	private boolean videoStep(byte[] data) {
		int inputIndex = videoMediaCodec.dequeueInputBuffer(-1);
		if (inputIndex >= 0) {
			if (hasNewData) {
				if (lastVideoFrameData == null) {
					lastVideoFrameData = new byte[data.length];
				}
				lastVideoFrameData = data;
				hasNewData = false;
			}
			ByteBuffer buffer = getInputBuffer(videoMediaCodec, inputIndex);
			buffer.clear();
			buffer.put(lastVideoFrameData);
			int length = lastVideoFrameData.length;
			videoMediaCodec.queueInputBuffer(inputIndex, 0, length, (System.nanoTime() - nanoTime) / 1000,
					isEncoding ? 0 : MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		}
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
		int outputIndex;
		do {
			outputIndex = videoMediaCodec.dequeueOutputBuffer(info, 0);
			if (outputIndex >= 0) {
				ByteBuffer buffer = getOutputBuffer(videoMediaCodec, outputIndex);
				if (info.size > 0 && info.presentationTimeUs > 0 && centerCallback != null) {
					if (info.presentationTimeUs > lastVideoPresentationTimeUs) {
						lastVideoPresentationTimeUs = info.presentationTimeUs;
						centerCallback.onVideoFrameCoded(buffer, info);
					}
				}
				videoMediaCodec.releaseOutputBuffer(outputIndex, false);
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					return true;
				}
			} else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				videoFormat = videoMediaCodec.getOutputFormat();
				checkMediaFormatPrepare();
			}
		} while (outputIndex >= 0);
		return false;
	}


	public interface MediaCodecCenterCallback {
		void onPrepare(MediaFormat audioFormat, MediaFormat videoFormat);

		void onVideoFrameCoded(ByteBuffer buffer, MediaCodec.BufferInfo info);

		void onAudioFrameCoded(ByteBuffer buffer, MediaCodec.BufferInfo info);
	}

}
