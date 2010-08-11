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
				//String system = "<![CDATA[" + entry.getDecSystem().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
				ps.println("  <system name=\"" + entry.getDecSystem() + "\">");
				if (entry.getDecUsername() != null) {
					String username = "<![CDATA[" + entry.getDecUsername().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
					ps.println("    <username>" + username + "</username>");
				}
				if (entry.getDecPassword() != null) {
					String password = "<![CDATA[" + entry.getDecPassword().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
					ps.println("    <password>" + password + "</password>");
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
