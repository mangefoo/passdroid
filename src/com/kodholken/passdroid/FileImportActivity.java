/*    
    This file is part of the Passdroid password management software.
    
    Copyright (C) 2009-2012  Magnus Eriksson <eriksson.mag@gmail.com>

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

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class FileImportActivity extends SherlockTimeoutActivity {
    private Button cancelButton;
    private Button importButton;
    private TextView importDesc2;
    private String importFile;
    private FileImporter fileImporter;
    
    private static final int IMPORT_PASSWORD_RESULT_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        importFile = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/passdroid_db.xml";

        this.setContentView(R.layout.file_import);

        cancelButton = (Button) this.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        boolean hasFile = fileExists(importFile);

        importButton = (Button) this.findViewById(R.id.import_button);

        importDesc2 = (TextView) this.findViewById(R.id.import_desc_2);
        if (!hasFile) {
            String desc2 = formatString(
                    getString(R.string.import_description_file_missing),
                    importFile, 0, "");
            importDesc2.setText(desc2);
            importDesc2.setTextColor(Color.rgb(255, 50, 50));
        } else {
            fileImporter = new FileImporter(importFile, Utils.getVersion(this));
            
            try {
                if (fileImporter.isEncrypted()) {
                    String desc2 = getString(R.string.import_description_encrypted_file_present, importFile);
                    importDesc2.setText(desc2);
                    importDesc2.setTextColor(Color.rgb(50, 150, 50));
                    importButton.setEnabled(true);
                    importButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            importEncrypted();
                        } 
                    });
                } else {
                    fileImporter.parse();
                    String desc2 = formatString(
                            getString(R.string.import_description_file_present),
                            importFile,
                            fileImporter.getPasswordEntries().length, "");
                    importDesc2.setText(desc2);
                    importDesc2.setTextColor(Color.rgb(50, 150, 50));
                    importButton.setEnabled(true);
                    importButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            importUnencrypted();
                        } 
                    });
                }
            } catch (FileImporterException ex) {
                ex.printStackTrace();
                String desc2 = formatString(
                        getString(R.string.import_description_failure),
                        importFile, 0, ex.getMessage());
                importDesc2.setText(desc2);
                importDesc2.setTextColor(Color.rgb(255, 150, 150));
            }
        }
    }

    private void importEncrypted() {
        try {
            /**
             * First we try to decrypt the file using our current master password. If that
             * fails we ask the user to supply a different password.
             */
            fileImporter.parseEncrypted(Session.getInstance().getKey());
            
            // If we get here the key was correct
            
            PasswordModel model = PasswordModel.getInstance(this);
            model.setPasswords(fileImporter.getPasswordEntries());
            showDialog(getString(R.string.success), fileImporter.getPasswordEntries().length + " entries imported.");
        } catch (FileImporterException ex) {
            ex.printStackTrace();
            // Something failed when decrypting the file, try with a user supplied password even though
            // the reason might have been something other that an invalid key.

            Intent i = new Intent(this, ImportPasswordActivity.class);
            startActivityForResult(i, IMPORT_PASSWORD_RESULT_ID);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == IMPORT_PASSWORD_RESULT_ID && resultCode == RESULT_OK && data.getExtras() != null) {
            String password = data.getExtras().getString("password");
            
            try {
                fileImporter.parseEncrypted(Crypto.hmacFromPassword(password));
                PasswordModel model = PasswordModel.getInstance(this);
                model.setPasswords(fileImporter.getPasswordEntries());
                showDialog(getString(R.string.success), fileImporter.getPasswordEntries().length + " entries imported from encrypted file.");
            } catch (FileImporterException ex) {
                showDialog(getString(R.string.failure), "Failed to decrypt the file. Please make sure you entered the correct password.");
            }
        }
    }
    
    private void importUnencrypted() {
        PasswordModel model = PasswordModel.getInstance(this);
        if (model.setPasswords(fileImporter.getPasswordEntries())) {
            deleteFileDialog(getString(R.string.success),
                             fileImporter.getPasswordEntries().length +
                             " entries imported. Do you want to delete the imported file from the device?");
        } else {
            showDialog(getString(R.string.failure), "Import failed.");
        }
    }

    private void showDialog(String title, String message) {
        AlertDialog alertDialog;

        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.show();
    }

    private void deleteFileDialog(String title, String message) {
        AlertDialog alertDialog;

        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                new File(importFile).delete();
                finish();
            }
        });
        alertDialog.setButton2(getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        alertDialog.show();
    }

    private final boolean fileExists(String filename) {
        File f = new File(filename);
        return f.isFile();
    }

    private String formatString(String template, String file, int nFiles,
            String error) {
        return template.replaceAll("%f", file)
                .replaceAll("%n", Integer.valueOf(nFiles).toString())
                .replaceAll("%e", error);
    }
}
