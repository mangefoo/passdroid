package com.kodholken.passdroid;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PasswordAdapter extends BaseAdapter {
	private LayoutInflater mInflater;
	private PasswordEntry [] entries;
	private boolean showUsername;

	public PasswordAdapter(Context context, PasswordEntry [] entries, boolean showUsername) {
		this.entries = entries;
		this.showUsername = showUsername;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return entries.length;
	}

	/**
	 * @see android.widget.ListAdapter#getItem(int)
	 */
	public Object getItem(int position) {
		return position;
	}

	/**
	 * @see android.widget.ListAdapter#getItemId(int)
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * @see android.widget.ListAdapter#getView(int, android.view.View,
	 *      android.view.ViewGroup)
	 */
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.password_row, null);

			holder = new ViewHolder();
			holder.system = (TextView) convertView.findViewById(R.id.system);
			holder.username = (TextView) convertView.findViewById(R.id.username);
			holder.separator = (TextView) convertView.findViewById(R.id.separator);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// Bind the data efficiently with the holder.
		holder.system.setText(entries[position].getDecSystem());
		if (showUsername && entries[position].getDecUsername().length() > 0) {
			holder.username.setText(entries[position].getDecUsername());
			holder.username.setVisibility(View.VISIBLE);
			holder.separator.setVisibility(View.VISIBLE);
		} else {
			// Use non-bold font when we don't display the username
			if (!showUsername) {
				Typeface tf = holder.system.getTypeface();
				holder.system.setTypeface(Typeface.create(tf, Typeface.NORMAL));
			}
			holder.username.setVisibility(View.GONE);
			holder.separator.setVisibility(View.GONE);
		}

		return convertView;
	}

	static class ViewHolder {
		TextView system;
		TextView username;
		TextView separator;
	}

}
