/*    
    This file is part of the Passdroid password management software.
    
    Copyright (C) 2009-2010  Magnus Eriksson <eriksson.mag@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.kodholken.passdroid;

import java.security.SecureRandom;
import java.util.Random;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

public class GeneratePasswordActivity extends TimeoutActivity {
	private static final String passwordLengthKey = "passwordLength";
	private static final String passwordCharsetKey = "passwordCharset";
	private static final String numericCharset = "0123456789";
	private static final String alphabeticCharset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String alphanumericCharset = alphabeticCharset + numericCharset;
	private static final String extendedCharset = alphanumericCharset + "!#@%&/()?+-_:;*=$";
	
	private EditText lengthField;
	private ImageButton incButton;
	private ImageButton decButton;
	private Spinner spinner;
	private boolean displayPassword;
	
	private static final int MAX_PASSWORD_LENGTH = 99;
	private static final int DEFAULT_PASSWORD_LENGTH = 10;
	
	// We save the generated password here for the caller to retrieve when we
	// have finished. 
	//       allows a single instance if the activity at the same time, but...
	private static String generatedPassword = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.generate_password);
		
		lengthField = (EditText)  findViewById(R.id.length_input);
		lengthField.setText("" + getPasswordLength());
		
		spinner = (Spinner) findViewById(R.id.charset);
		spinner.setSelection(getCharset());
		
		Bundle extras = getIntent().getExtras();
		if (extras != null && extras.containsKey("displayPassword")) {
			displayPassword = true;
		}
		
		setupIncButton();
		setupDecButton();
		setupCancelButton();
		setupGenerateButton();
	}
	
	private void setupIncButton() {
		incButton = (ImageButton) findViewById(R.id.increment);
		
		incButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int current = 0;
				try {
					current = Integer.parseInt(lengthField.getText().toString());
					if (current < MAX_PASSWORD_LENGTH) {
						current++;
					}
				} catch (NumberFormatException ex) {
					current = DEFAULT_PASSWORD_LENGTH;
				}
				
				lengthField.setText("" + current);
			}
		});
		
		incButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					incButton.setBackgroundResource(R.drawable.picker_up_pressed);
					System.out.println("Down");
					break;
				case MotionEvent.ACTION_UP:
					incButton.setBackgroundResource(R.drawable.picker_up);
					System.out.println("Up");
					break;
				}
				return false;
			}
			
		});
	}
	
	private void setupDecButton() {
		decButton = (ImageButton) findViewById(R.id.decrement);

		decButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int current = 0;
				try {
					current = Integer.parseInt(lengthField.getText().toString());
					if (current > 1) {
						current--;
					}
				} catch (NumberFormatException ex) {
					current = DEFAULT_PASSWORD_LENGTH;
				}
				
				lengthField.setText("" + current);
			}
		});
		
		decButton.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					decButton.setBackgroundResource(R.drawable.picker_down_pressed);
					break;
				case MotionEvent.ACTION_UP:
					decButton.setBackgroundResource(R.drawable.picker_down);
					break;
				}
				return false;
			}
			
		});
	}
	
	private void setupCancelButton() {
		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(0);
				finish();
			}
		});
	}
	
	private void setupGenerateButton() {
		Button generateButton = (Button) findViewById(R.id.generate_button);
		
		final Intent ri = this.getIntent();
		final Context context = this;
		generateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent result = new Intent(
	             	"com.kodholken.passdroid.PASSWORD_RESULT_ACTION");
				ri.putExtra("password", "new value");
				result.putExtra("password", generatedPassword);
				generatePassword();
				if (displayPassword) {
					Intent generateIntent = new Intent(context,
							                    DisplayPasswordActivity.class);
					generateIntent.putExtra("password",
							  GeneratePasswordActivity.getGeneratedPassword());
					startActivity(generateIntent);
				} else {
					setResult(1, result);
				}
				finish();
			}
		});
	}
	
	private void generatePassword() {
		generatedPassword = "";
		Random random = new SecureRandom();
		
		int pos = spinner.getSelectedItemPosition();
		if (pos == Spinner.INVALID_POSITION) {
			return ;
		}
		
		String charset;
		switch (pos) {
		case 0:
			charset = alphanumericCharset;
			break;
		case 1:
			charset = alphabeticCharset;
			break;
		case 2:
			charset = numericCharset;
			break;
		case 3:
			charset = extendedCharset;
			break;
		default:
			charset = alphanumericCharset;
		}
		
		int length = Integer.parseInt(lengthField.getText().toString());
		int i = 0;
		while (i++ < length) {
			generatedPassword += charset.charAt(random.nextInt(charset.length()));
		}
		
		storePasswordLength(length);
		storeCharset(pos);
	}
	
	public static String getGeneratedPassword() {
		return generatedPassword;
	}
	
	private int getPasswordLength() {
		return PreferenceManager.getDefaultSharedPreferences(this).
		                     getInt(passwordLengthKey, DEFAULT_PASSWORD_LENGTH);
	}
	
	private void storePasswordLength(int length) {
		PreferenceManager.getDefaultSharedPreferences(this).edit().
		                            putInt(passwordLengthKey, length).commit();
	}
	
	private int getCharset() {
		return PreferenceManager.getDefaultSharedPreferences(this).
							 getInt(passwordCharsetKey, 0);
	}
	
	private void storeCharset(int charset) {
		PreferenceManager.getDefaultSharedPreferences(this).edit().
								  putInt(passwordCharsetKey, charset).commit();
	}
}
