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
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FileImportActivity extends SherlockTimeoutActivity {
	private LinearLayout importLayout;
	private LinearLayout searchingLayout;
    private Button cancelButton;
    private Button importButton;
    private TextView importDesc2;
    private FileImporter fileImporter;
    private boolean searchingForFiles;
    private boolean prepared;
    
    // We search this many levels in the filesystem when looking for import files
    private static final int FS_SEARCH_DEPTH = 3;
    
    private static final String IMPORT_PASSWORD_FILE_NAME = "passdroid_db.xml";
    
    private static final int IMPORT_PASSWORD_RESULT_ID = 1;
    private static final int SELECT_FILE_RESULT_ID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.file_import);
        
        importLayout = (LinearLayout) findViewById(R.id.import_layout);
        searchingLayout = (LinearLayout) findViewById(R.id.searching_layout);

        cancelButton = (Button) this.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        importButton = (Button) this.findViewById(R.id.import_button);
        importDesc2 = (TextView) this.findViewById(R.id.import_desc_2);
        
        searchingForFiles = false;
        prepared = false;
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (!prepared && !searchingForFiles) {
    		searchingForFiles = true;
    		new ImportFileFinderTask().execute();
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
        
        searchingLayout.setVisibility(View.GONE);
        importLayout.setVisibility(View.VISIBLE);
        
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
        } else if (requestCode == SELECT_FILE_RESULT_ID) {
        	if (resultCode == RESULT_OK && data.getExtras() != null) {
        		String filename = data.getStringExtra("filename");
        		if (filename != null) {
        			prepareImport(filename);
        		}
        	} else {
        		// Something went wrong when selecting import file or the 
        		// user pressed the back button. Since we don't have any
        		// import file to work with we close down.
        		finish();
        	}
        }
    }
    
    private void importUnencrypted() {
        PasswordModel model = PasswordModel.getInstance(this);
        if (model.setPasswords(fileImporter.getPasswordEntries())) {
            deleteFileDialog(getString(R.string.success),
                             fileImporter.getPasswordEntries().length +
                             " entries imported. Do you want to delete the imported file from the device?",
                             fileImporter.getFilename());
        } else {
            showDialog(getString(R.string.failure), "Import failed.");
        }
    }

    private void showDialog(String title, String message) {
        AlertDialog alertDialog;

        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.show();
    }

    private void deleteFileDialog(String title, String message, final String filename) {
        AlertDialog alertDialog;

        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                new File(filename).delete();
                finish();
            }
        });
        alertDialog.setButton(Dialog.BUTTON_NEGATIVE, getString(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        alertDialog.show();
    }

    private String formatString(String template, String file, int nFiles,
            String error) {
        return template.replaceAll("%f", file)
                .replaceAll("%n", Integer.valueOf(nFiles).toString())
                .replaceAll("%e", error);
    }
    
    private void prepareImport(String filename) {
        fileImporter = new FileImporter(filename, Utils.getVersion(FileImportActivity.this));
        
        try {
            if (fileImporter.isEncrypted()) {
                String desc2 = getString(R.string.import_description_encrypted_file_present, filename);
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
                        filename,
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
                    filename, 0, ex.getMessage());
            importDesc2.setText(desc2);
            importDesc2.setTextColor(Color.rgb(255, 50, 50));
        }
        
        prepared = true;
    }
    
    private class ImportFileFinderTask extends AsyncTask<Void, Void, List<String>> {
		@Override
		protected List<String> doInBackground(Void... params) {
			ArrayList<String> paths = new ArrayList<String>();

			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				findImportFiles(paths, Environment.getExternalStorageDirectory(), FS_SEARCH_DEPTH);
			}
			
			// This variable is present on, at least, some Samsung devices and is a 
			// colon separated list with paths to "secondary" storage such as removeable
			// SD cards and external USB drives.
			String secondaryStorage = System.getenv("SECONDARY_STORAGE");
			if (secondaryStorage == null) {
				return paths;
			}

			for (String sec : secondaryStorage.split(":")) {
				File f = new File(sec);
				if (f.isDirectory()) {
					findImportFiles(paths, f, FS_SEARCH_DEPTH);
				}
			}

			return paths;
		}
		
		@Override
		protected void onPostExecute(List<String> result) {
	    	Intent intent = new Intent(FileImportActivity.this, FileSelectorActivity.class);
	    	
	    	if (result.size() > 0) {
	    		String [] fileArray = new String[result.size()];
	    		result.toArray(fileArray);
	    		intent.putExtra("files", fileArray);
	    	}
	    	
	    	startActivityForResult(intent, SELECT_FILE_RESULT_ID);
			
			searchingForFiles = false;
		}

		private void findImportFiles(ArrayList<String> paths, File dir, int fsSearchDepth) {
			if (!dir.isDirectory()) {
				return;
			}
			
			if (!dir.canRead()) {
				return;
			}
			
			// Apparently dir.list() can return null even though the dir.isDirectory() returned true
			String [] filenames = dir.list();
			if (filenames == null) {
				return;
			}

			for (String filename : filenames) {
				String absFile = dir.getAbsolutePath() + File.separator + filename;
				File file = new File(absFile);

				if (fsSearchDepth > 1 && file.isDirectory()) {
					findImportFiles(paths, file, fsSearchDepth - 1);
				} else if (file.isFile() && filename.equals(IMPORT_PASSWORD_FILE_NAME)) {
					paths.add(absFile);
				}
			}
		}
    }
}
