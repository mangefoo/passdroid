package com.kodholken.passdroid;

import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

public class DisplayPasswordActivity extends TimeoutActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.display_password);
				
		((TextView) findViewById(R.id.password)).setText(
								getIntent().getExtras().getString("password"));

	}
}
