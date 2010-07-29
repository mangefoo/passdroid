package com.kodholken.passdroid;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class FileExporter {
	private String filename;
	private String appVersion;
	
	public FileExporter(String filename, String appVersion) {
		this.filename = filename;
		this.appVersion = appVersion;
	}
	
	public boolean export(PasswordEntry [] passwords, boolean overwrite) throws ExportException {
		File f = new File(filename);
		if (f.exists()) {
			if (!overwrite) {
				throw new ExportException("Already exist");
			}

			f.delete();
		}
		
		try {
			f.createNewFile();
			PrintStream ps = new PrintStream(f);
			ps.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			ps.println("<passdroid version=\"" + appVersion + "\">");
			for (PasswordEntry entry : passwords) {
				ps.println("  <system name=\"" + entry.getDecSystem() + "\">");
				if (entry.getDecUsername() != null) {
					ps.println("    <username>" + entry.getDecUsername() + "</username>");
				}
				if (entry.getDecPassword() != null) {
					ps.println("    <password>" + entry.getDecPassword() + "</password>");
				}
				ps.println("  </system>");
			}
			ps.println("</passdroid>");
			ps.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
