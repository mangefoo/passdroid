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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
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
    private PasswordEntry[] passwordEntries;

    public FileImporter(String filename, String appVersion) {
        this.filename = filename;
        this.appVersion = appVersion;
    }

    public void parse() throws FileImporterException {
        try {
            parse(createInputStream());
        } catch (IOException ex) {
            throw new FileImporterException(ex);
        }
    }

    public void parse(InputStream input) throws FileImporterException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(input);
            Element root = doc.getDocumentElement();
            if (root == null || !root.getTagName().equals("passdroid")) {
                throw new FileImporterException("Invalid file format: " + (root == null ? "<empty>" : root.getTagName()));
            }

            Node versionNode = root.getAttributes().getNamedItem("version");
            if (versionNode == null) {
                throw new FileImporterException("Missing version attribute on passdroid tag");
            }
            String version = versionNode.getNodeValue();
            Log.d(FileImporter.class.getName(), "Import file version: " + version);
            parseImportFile(version, root);
        } catch (ParserConfigurationException ex) {
            throw new FileImporterException(ex);
        } catch (IOException ex) {
            throw new FileImporterException(ex);
        } catch (SAXException ex) {
            throw new FileImporterException(ex);
        }
    }

    public boolean isEncrypted() throws FileImporterException {
        InputStream is = null;

        try {
            byte [] sig = new byte[3];
            is = new FileInputStream(new File(filename));
            if (is.read(sig) != 3) {
                throw new FileImporterException("Could not read signature");
            }

            if (sig[0] != 's' || sig[1] != 'q' || sig[2] != 't') {
                return false;
            }
        } catch (Exception e) {
            throw new FileImporterException(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }

        return true;
    }
    
    public String getFilename() {
    	return filename;
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

        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));

        String s;
        Pattern p = Pattern.compile(".*passdroid version=\\\"([0-9]+.[0-9]+)\\\".*");
        while ((s = br.readLine()) != null) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
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
            return new FileInputStream(filename);
        }

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
            String note = "";
            String url = "";

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
                } else if (node.getNodeName().equals("note")) {
                    if (node.getFirstChild() != null) {
                        note = node.getFirstChild().getNodeValue();
                    }
                } else if (node.getNodeName().equals("url")) {
                    if (node.getFirstChild() != null) {
                        url = node.getFirstChild().getNodeValue();
                    }
                }
            }

            PasswordEntry entry = new PasswordEntry();
            entry.setDecSystem(name);
            entry.setDecUsername(username);
            entry.setDecPassword(password);
            entry.setDecNote(note);
            entry.setDecUrl(url);

            return entry;
        }
    }

    public void parseEncrypted(byte[] key) throws FileImporterException {
        try {
            File file = new File(filename);
            long size = file.length();

            // File needs to be atleast 3 bytes to have room for encryption signature
            if (size < 3) {
                throw new FileImporterException("File too small: " + size);
            }

            size -= 3;
            byte[] buffer = new byte[(int) size];

            FileInputStream fstream = new FileInputStream(filename);

            if (fstream.read(buffer, 0, 3) != 3) {
                throw new FileImporterException("Failed to read signature");
            }

            int nread, hasRead = 0;
            while (hasRead < size) {
                nread = fstream.read(buffer, hasRead, (int) (size - hasRead));
                if (nread == -1) {
                    break;
                }
                hasRead += nread;
            }
            fstream.close();

            if (hasRead != size) {
                throw new FileImporterException("Failed to read encrypted data");
            }

            byte[] decrypted = Crypto.decryptAesCbc(key, buffer);
            if (decrypted == null) {
                throw new FileImporterException("Decryption failed");
            }

            InputStream is = new ByteArrayInputStream(decrypted, 2 /* Skip salt */, decrypted.length - 1);
            parse(is);
        } catch (IOException ex) {
            throw new FileImporterException(ex);
        }
    }
}
