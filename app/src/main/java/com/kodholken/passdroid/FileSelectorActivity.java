package com.kodholken.passdroid;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileSelectorActivity extends TimeoutListActivity {
	private ListView fileList;
	private TextView directoryView;
	private boolean displayAll;
	private String currentDir;
    private LayoutInflater inflater;
    private FileListAdapter fileListAdapter;
    private CheckBox filterCheckbox;
    
    private String [] requestFiles;
    
    public static final int RESULT_ERROR = Activity.RESULT_FIRST_USER;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!Session.getInstance().isLoggedIn()) {
            finish();
            return ;
        }

        setContentView(R.layout.file_selector);
        
        fileList = (ListView) findViewById(android.R.id.list);
        fileList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				handleClick((File) fileListAdapter.getItem(position));				
			}
        });
        
        directoryView = (TextView) findViewById(R.id.directory);
        
        filterCheckbox = (CheckBox) findViewById(R.id.filter);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	requestFiles = extras.getStringArray("files");
        }
        
        if (requestFiles != null) {
        	displayAll = false;
        } else {
        	displayAll = true;
        	filterCheckbox.setVisibility(View.GONE);
        }
    	filterCheckbox.setChecked(displayAll);
    	
        filterCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				displayAll = isChecked;
				updateFileList();
			}
        });

        currentDir = getIntent().getStringExtra("directory");
        if (currentDir == null) {
        	currentDir = "/";
        }
        
        inflater = LayoutInflater.from(this);
        fileListAdapter = new FileListAdapter();
        fileList.setAdapter(fileListAdapter);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	updateFileList();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // User press home back button
            finish();
            overridePendingTransition(0, 0);
            return true;
        }
        
        return false;
    }
    
    private void handleClick(File file) {
    	if (file.isDirectory()) {
    		try {
    			if (file.getName().equals("..")) {
    				currentDir = file.getCanonicalPath();
    				if (currentDir.equals("")) {
    					currentDir = "/";
    				}
    			} else {
    				currentDir = file.getAbsolutePath();
    			}
    		} catch (IOException ex) {
    			ex.printStackTrace();
    			currentDir = "/";
    		}
    		updateFileList();
    	} else if (file.isFile() && file.canRead()) {
    		Intent data = new Intent();
    		try {
				data.putExtra("filename", file.getCanonicalPath());
	    		setResult(RESULT_OK, data);
			} catch (IOException e) {
				e.printStackTrace();
				setResult(RESULT_ERROR);
			}
    		
    		finish();
    	}
    }
    
    private void updateFileList() {
    	ArrayList<File> list = new ArrayList<File>();

    	if (!displayAll && requestFiles != null) {
    		directoryView.setText("Detected import files");
    		for (String file : requestFiles) {
    			list.add(new File(file));
    		}
    	} else {
    		File dir = new File(currentDir);

    		if (!dir.isDirectory()) {
    			// Should not happen
    			return;
    		}

    		if (!dir.canRead()) {
    			return;
    		}

    		directoryView.setText(currentDir);

    		boolean isRoot = currentDir.equals("/");
    		
    		if (!isRoot) {
    			list.add(new File(currentDir + File.separator + ".."));
    		}

    		// Some implementations are missing File.canExecute()
    		boolean haveCanExecute = true;
    		try {
    			new File("/").canExecute();
    		} catch (NoSuchMethodError ex) {
    			haveCanExecute = false;
    		}

    		for (String file : dir.list()) {
    			File f = new File(currentDir + File.separator + file);
				// Only show directories we can read
    			if (f.isDirectory() && f.canRead()) {
    				if (!haveCanExecute || f.canExecute()) {
    					list.add(f);
    				}
    			} else if (f.isFile() && f.canRead()) {
    				list.add(f);
    			}
    		}

    		java.util.Collections.sort(list, new FileComparator());
    	}
    	fileListAdapter.setFiles(list);
    	fileListAdapter.setShowFullPath(!displayAll);
    	fileListAdapter.notifyDataSetChanged();
    }
    
    private class FileComparator implements Comparator<File> {
		@Override
		public int compare(File lhs, File rhs) {
			if (lhs.isDirectory() == rhs.isDirectory()) {
				return lhs.compareTo(rhs);
			}
			
			return lhs.isDirectory() ? -1 : 1;
		}
    }
    
    private class FileListAdapter extends BaseAdapter {
    	ArrayList<File> files;
    	boolean showFullPath = false;
    	
    	public void setFiles(ArrayList<File> files) {
			this.files = files;
		}
    	
    	public void setShowFullPath(boolean showFullPath) {
			this.showFullPath = showFullPath;
		}
    	
		@Override
		public int getCount() {
			if (files == null) {
				return 0;
			}

			return files.size();
		}

		@Override
		public Object getItem(int position) {
			return files.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
	        ViewHolder holder;

	        if (convertView == null) {
	            convertView = inflater.inflate(R.layout.file_row, null);

	            holder = new ViewHolder();
	            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
	            holder.filename = (TextView) convertView.findViewById(R.id.filename);
	            holder.date = (TextView) convertView.findViewById(R.id.date);

	            convertView.setTag(holder);
	        } else {
	            holder = (ViewHolder) convertView.getTag();
	        }

	        File file = files.get(position);
	        if (file.isDirectory()) {
	        	holder.icon.setImageDrawable(getResources().getDrawable(R.drawable.folder));
	        	holder.date.setVisibility(View.GONE);
	        } else {
	        	holder.icon.setImageDrawable(getResources().getDrawable(R.drawable.document));
	        	holder.date.setVisibility(View.VISIBLE);
	        	SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd hh:mm");
	        	holder.date.setText("Last modified at " + dt.format(new Date(file.lastModified())));
	        }
	        
	        if (!showFullPath) {
	        	holder.filename.setText(file.getName());
	        } else {
	        	holder.filename.setText(file.getAbsolutePath());
	        }

	        return convertView;
	    }

	    class ViewHolder {
	    	ImageView icon;
	        TextView  filename;
	        TextView  date;
	    }
    }
}
