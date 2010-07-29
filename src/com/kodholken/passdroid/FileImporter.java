package com.kodholken.passdroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
			Document doc = builder.parse(new File(filename));
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
