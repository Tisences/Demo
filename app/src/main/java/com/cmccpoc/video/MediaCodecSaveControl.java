package com.cmccpoc.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 音视频流保存，实现预录制，分段录制
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MediaCodecSaveControl {

	public static final String TAG = MediaCodecSaveControl.class.getSimpleName();
	private static final int AUDIO_TRACK = 1;
	private static final int VIDEO_TRACK = 2;

	private Context context;

	private static final long SAVING_STOP_DELAY = (long) (1.0 * 1000);

	private MediaMuxerSave muxerSave1;
	private MediaMuxerSave muxerSave2;
	private MediaFormat audioFormat;
	private MediaFormat videoFormat;
	private int videoPreRecordDuration;
	private ArrayBlockingQueue<VideoDataPerSecond> videoDataBlockingQueue;

	private long lastDataPerSecondStartMillis = 0;
	private VideoDataPerSecond currentVideoDataPerSecond;
	private boolean isSaving = false;
	private boolean isCheckStage = false;

	private ByteBuffer buffer = ByteBuffer.allocate(999999);

	public MediaCodecSaveControl(Context context, int videoPreRecordDuration) {
		this.context = context;
		this.videoPreRecordDuration = videoPreRecordDuration;
		this.videoDataBlockingQueue = new ArrayBlockingQueue<>(this.videoPreRecordDuration * 2 + 4);
	}

	public void setMediaFormat(MediaFormat audioFormat, MediaFormat videoFormat) {
		this.audioFormat = audioFormat;
		this.videoFormat = videoFormat;
	}

	public void addVideoFrameData(byte[] data, MediaCodec.BufferInfo info) {
		addMediaData(VIDEO_TRACK, data, info);
	}

	public void addAudioFrameData(byte[] data, MediaCodec.BufferInfo info) {
		addMediaData(AUDIO_TRACK, data, info);
	}

	private String createNewFilePath() {
		return "sdcard/DCIM/VID_" + System.currentTimeMillis() + ".mp4";
	}


	private synchronized void addMediaData(int index, byte[] data, MediaCodec.BufferInfo info) {
		if (isSaving && !isCheckStage) {
			if (currentVideoDataPerSecond != null) {
				for (VideoData videoData : currentVideoDataPerSecond.videoDataList) {
					writeMuxerData(videoData.trackIndex, videoData.data, videoData.info);
				}
				currentVideoDataPerSecond = null;
			}
			writeMuxerData(index, data, info);
		} else {
			stageMuxerData(index, data, info);
		}
	}

	/**
	 * 每500ms暂时存储数据到内存
	 *
	 * @param index index
	 * @param data  data
	 * @param info  bufferInfo
	 */
	private synchronized void stageMuxerData(int index, byte[] data, MediaCodec.BufferInfo info) {
		VideoData temp = new VideoData(index, data, info);
		long currentMillis = System.currentTimeMillis();
		//比较时间 是否是在500ms之内的
		if (currentMillis - lastDataPerSecondStartMillis > 500) {
			lastDataPerSecondStartMillis = currentMillis;
			if (currentVideoDataPerSecond != null) {
				int capacity = videoDataBlockingQueue.remainingCapacity();
//				Log.d(TAG, "queue capacity " + capacity);
				if (capacity <= 4) {
					videoDataBlockingQueue.poll();
				}
				videoDataBlockingQueue.offer(currentVideoDataPerSecond);
			}
			currentVideoDataPerSecond = new VideoDataPerSecond();
			currentVideoDataPerSecond.videoDataList.add(temp);
		} else {
			if (currentVideoDataPerSecond != null) {
				currentVideoDataPerSecond.videoDataList.add(temp);
			}
		}
	}

	/**
	 * 写入数据到视频文件
	 *
	 * @param index index
	 * @param data  data
	 * @param info  bufferInfo
	 */
	private synchronized void writeMuxerData(int index, byte[] data, MediaCodec.BufferInfo info) {
//		Log.w(TAG, "writeMuxerData " + index + " Data " + data.length + " " + " " + info.size + " " + info.presentationTimeUs);
		buffer.clear();
		if (data.length < info.size) {
			info.size = data.length;
		}
		if (info.size <= buffer.capacity()) {
			buffer.put(data, 0, info.size);
		}
		if (muxerSave1 != null) {
			if (index == AUDIO_TRACK) {
				muxerSave1.writeAudioTrackData(buffer, info);
			} else if (index == VIDEO_TRACK) {
				muxerSave1.writeVideoTrackData(buffer, info);
			}
		}
		if (muxerSave2 != null) {
			if (index == AUDIO_TRACK) {
				muxerSave2.writeAudioTrackData(buffer, info);
			} else if (index == VIDEO_TRACK) {
				muxerSave2.writeVideoTrackData(buffer, info);
			}
		}
	}

	private void checkStageData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				VideoDataPerSecond videoDataPerSecond;
				do {
					videoDataPerSecond = videoDataBlockingQueue.poll();
					if (videoDataPerSecond != null) {
						for (VideoData videoData : videoDataPerSecond.videoDataList) {
							writeMuxerData(videoData.trackIndex, videoData.data, videoData.info);
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				} while (videoDataPerSecond != null && isCheckStage);
				isCheckStage = false;
				Log.w(TAG, "CheckStage over");
			}
		}).start();
	}

	public void startSaveVideo() {
		muxerSave1 = new MediaMuxerSave(context, createNewFilePath());
		muxerSave1.start(audioFormat, videoFormat);
		isCheckStage = true;
		isSaving = true;
		checkStageData();
	}

	public void cutOffMediaSave() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (muxerSave1 != null) {
						muxerSave2 = new MediaMuxerSave(context, createNewFilePath());
						muxerSave2.start(audioFormat, videoFormat);
						Thread.sleep(SAVING_STOP_DELAY);
						if (muxerSave1 != null) {
							muxerSave1.stop();
							muxerSave1.release();
							muxerSave1 = null;
						}
					} else if (muxerSave2 != null) {
						muxerSave1 = new MediaMuxerSave(context, createNewFilePath());
						muxerSave1.start(audioFormat, videoFormat);
						Thread.sleep(SAVING_STOP_DELAY);
						if (muxerSave2 != null) {
							muxerSave2.stop();
							muxerSave2.release();
							muxerSave2 = null;
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void stopSaveVideo() {
		this.videoDataBlockingQueue = new ArrayBlockingQueue<>(this.videoPreRecordDuration * 2 + 4);
		currentVideoDataPerSecond = null;
		isSaving = false;
		isCheckStage = false;
		if (muxerSave1 != null) {
			muxerSave1.stop();
			muxerSave1.release();
			muxerSave1 = null;
		}
		if (muxerSave2 != null) {
			muxerSave2.stop();
			muxerSave2.release();
			muxerSave2 = null;
		}
	}

	//每秒存储的视频数据
	private class VideoDataPerSecond {
		private List<VideoData> videoDataList = new ArrayList<>(30);
	}

	private class VideoData {
		private int trackIndex;
		private byte[] data;
		private MediaCodec.BufferInfo info;

		VideoData(int trackIndex, byte[] data, MediaCodec.BufferInfo info) {
			this.trackIndex = trackIndex;
			this.data = data;
			this.info = info;
		}
	}
}
