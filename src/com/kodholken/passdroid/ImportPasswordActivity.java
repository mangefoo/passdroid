package com.kodholken.passdroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;

public class ImportPasswordActivity extends TimeoutActivity {
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.import_password);
        
        password = (EditText) findViewById(R.id.password);
        
        findViewById(R.id.ok_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent("com.kodholken.passdroid.IMPORT_PASSWORD_RESULT_ACTION");
                result.putExtra("password", password.getText().toString());
                setResult(RESULT_OK, result);
                finish();
            }
        });
        
        findViewById(R.id.cancel_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}
