package com.vanzo.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

public class DemoActivity extends Activity {


	private Spinner spinner;
	private int rate = 9000000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);
		spinner = findViewById(R.id.spinner);
		findViewById(R.id.to_camera).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startCamera(rate);
			}
		});
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				rate = Integer.parseInt(getResources().getStringArray(R.array.bitrate_value)[position]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	private void startCamera(int rate) {
//		Intent intent = new Intent(this, BitrateActivity.class);
//		intent.putExtra("rate", rate);
//		startActivity(intent);
	}
}
