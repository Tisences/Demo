package com.vanzo.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;

public class AudioActivity extends Activity {

	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_audio);
	}

	public void start() throws Exception {
		audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		mediaPlayer = MediaPlayer.create(this, R.raw.sherlock);
		mediaPlayer.setLooping(true);
		audioManager.setMode(AudioManager.MODE_NORMAL);
		mediaPlayer.start();
	}

	public void stop() throws Exception {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
			audioManager.setMode(AudioManager.MODE_NORMAL);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
//		try {
//			startEncode();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE));

	}

	@Override
	protected void onStop() {
		super.onStop();
//		try {
//			stopEncode();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}
}
