/*    
    This file is part of the Passdroid password management software.
    
    Copyright (C) 2009-2011  Magnus Eriksson <eriksson.mag@gmail.com>

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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.util.Log;

public class FileImporter {
	private String filename;
	private String appVersion;
	private boolean loaded;
	private PasswordEntry[] passwordEntries;
	
	public FileImporter(String filename, String appVersion) {
		this.filename = filename;
		this.appVersion = appVersion;
		loaded = false;
	}
	
	public void parse() throws FileImporterException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream input = createInputStream();
			//Document doc = builder.parse(new File(filename));
			Document doc = builder.parse(input);
			Element root = doc.getDocumentElement();
			NodeList items = root.getElementsByTagName("passdroid");
			if (items.getLength() != 1) {
				throw new FileImporterException("Invalid file format");
			}
			
			Node versionNode = items.item(0).getAttributes().getNamedItem("version");
			if (versionNode == null) {
				throw new FileImporterException("Missing version attribute on passdroid tag");
			}
			String version = versionNode.getNodeValue();
			Log.d(FileImporter.class.getName(), "Import file version: " + version);
						
			parseImportFile(version, items.item(0));
			loaded = true;
		} catch (ParserConfigurationException ex) {
			throw new FileImporterException(ex);
		} catch (IOException ex) {
			throw new FileImporterException(ex);
		} catch (SAXException ex) {
			throw new FileImporterException(ex);
		}
	}
	
	/**
	 * Tries to extract the exported file version without any XML parsing. This
	 * is needed since the (invalid) exported files are sometimes not accepted
	 * by the SAX parser.
	 * 
	 * @return The extracted file version if it exists. Else null.
	 * 
	 * @throws IOException
	 */
	private String getFileVersion() throws IOException {
		String version = null;
		FileInputStream fstream = new FileInputStream(filename);
		
		System.out.println("Getting file version\n");

		DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    
	    String s;
    	Pattern p = Pattern.compile(".*passdroid version=\\\"([0-9]+.[0-9]+)\\\".*");
	    while ((s = br.readLine()) != null) {
	    	Matcher m = p.matcher(s);
	    	System.out.println("Checking row '" + s + "'");
	    	if (m.matches()) {
	    		System.out.println("Got version " + m.group(1));
	    		version = m.group(1);
	    		break;
	    	}
	    }
	    
	    br.close();
	    in.close();
	    fstream.close();
	    
	    return version;
	}
	
	private InputStream createInputStream() throws IOException, FileImporterException {
		String version = getFileVersion();
		
		if (version == null) {
			throw new FileImporterException("Unable to get exported file version");
		}
		
		Version fileVersion = Version.parse(version);
		Version compVersion = new Version(1, 6);
		
		if (fileVersion.compareTo(compVersion) > 0) {
			// Version 1.7 or later is passed as a FileInputStream instance.
			System.out.println("Creating FileInputStream");
			return new FileInputStream(filename);
		}
		
		System.out.println("Creating ByteArrayInputStream");
		
		/* 
		 * If the version is 1.6 or earlier we need to pass the exported file
		 * through a filter to escape some non-allowed characters so we 
		 * read the file to memory, escapes the non-allowed characters and
		 * creates a StringInputStream instance from the string.
		 */

		FileInputStream fstream = new FileInputStream(filename);
		
	    DataInputStream in = new DataInputStream(fstream);
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    
	    String s;
	    StringBuilder sb = new StringBuilder();
    	Pattern p = Pattern.compile("(.*)<system name=\\\"([^\"]+)\\\">.*");
	    while ((s = br.readLine()) != null) {
	    	Matcher m = p.matcher(s);
	    	if (m.matches()) {
	    		sb.append(m.group(1) + "<system name=\"" + 
	    				  Utils.escapeXMLChars(m.group(2)) + "\">\n");
	    	} else {
	    		sb.append(s + "\n");
	    	}
	    }
	    
	    br.close();
	    in.close();
	    fstream.close();
	    
	    System.out.println("Converted XML: " + sb.toString());
	    
	    return new ByteArrayInputStream(sb.toString().getBytes("UTF-8"));
	}
		
	private void parseImportFile(String version, Node root) throws FileImporterException {
		ImportFileParser parser = null;
		Version fileVersion, appVersion;
		
		try {
			fileVersion = Version.parse(version);
			appVersion = Version.parse(this.appVersion);
		} catch (NumberFormatException ex) {
			throw new FileImporterException(ex);
		}
		
		if (fileVersion.compareTo(appVersion) > 0) {
			throw new FileImporterException("Import file version (" + 
					fileVersion + ") is larger than the app version (" +
					appVersion +")");
		}
		
		parser = new ImportFileParser_v_1_0();		
		parser.parse(root);
	}
	
	public PasswordEntry [] getPasswordEntries() {
		return passwordEntries;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	private interface ImportFileParser {
		void parse(Node root);
	}
	
	private class ImportFileParser_v_1_0 implements ImportFileParser {
		public ImportFileParser_v_1_0() {}

		@Override
		public void parse(Node root) {
			ArrayList<PasswordEntry> entries = new ArrayList<PasswordEntry>();
			
			NodeList nodes = root.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				if (nodes.item(i).getNodeName().equals("system")) {
					PasswordEntry entry = parseSystemNode(nodes.item(i));
					entries.add(entry);
				}
			}
			
			passwordEntries = entries.toArray(new PasswordEntry [entries.size()]);
		}
		
		private PasswordEntry parseSystemNode(Node system) {
			String name  = system.getAttributes().getNamedItem("name").getNodeValue();
			String username = "";
			String password = "";
			
			NodeList nodes = system.getChildNodes();
			
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if (node.getNodeName().equals("username")) {
					if (node.getFirstChild() != null) {
						username = node.getFirstChild().getNodeValue();
					}
				} else if (node.getNodeName().equals("password")) {
					if (node.getFirstChild() != null) {
						password = node.getFirstChild().getNodeValue();
					}
				}
			}
			
			PasswordEntry entry = new PasswordEntry();
			entry.setDecSystem(name);
			entry.setDecUsername(username);
			entry.setDecPassword(password);

			return entry;
		}
	}
}
