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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class that handles the exporting of the password database to file.
 */
public class FileExporter {
    private String filename;
    private String appVersion;

    /**
     * Exporter constructor.
     * 
     * @param filename   full path to the exported file.
     * @param appVersion passdroid version to be recorded in the exported file.
     */
    public FileExporter(String filename, String appVersion) {
        this.filename = filename;
        this.appVersion = appVersion;
    }

    /**
     * Export the password database unencrypted.
     * 
     * @param passwords  array of password entries that make up the passdroid
     *                   database.
     * @throws ExportException on failure.
     */
    public void exportCleartext(PasswordEntry [] passwords) throws ExportException {
        String xml = createXmlTree(passwords);
        try {
            writeToFile(filename, xml.getBytes());
        } catch (Exception ex) {
            throw new ExportException(ex.getMessage());
        }
    }

    /**
     * Export the password database encrypted. The format of the encrypted file
     * is:
     * [1] 's'
     * [1] 'q'
     * [1] 't'
     * [?] encrypted data 
     * 
     * @param key        AES key to use for encryption.
     * @param passwords  array of passwords to export
     * @throws ExportException on failure.
     */
    public void exportEncrypted(byte [] key, PasswordEntry [] passwords) throws ExportException {
        String xml = createXmlTree(passwords);

        byte [] magic = { 's', 'q', 't' };
        byte [] encrypted = Crypto.encryptAesCbc(key, xml);

        byte [] all = new byte[magic.length + encrypted.length];
        System.arraycopy(magic, 0, all, 0, magic.length);
        System.arraycopy(encrypted, 0, all, magic.length, encrypted.length);

        try {
            writeToFile(filename, all);
        } catch (Exception ex) {
            throw new ExportException(ex.getMessage());
        }
    }

    /**
     * Create XML tree from an array of passwords. This tree represents the
     * format that is stored in the exported files on disk.
     * 
     * @param passwords array of passwords.
     * @return String representing the XML tree.
     */
    private String createXmlTree(PasswordEntry [] passwords) {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<passdroid version=\"" + appVersion + "\">\n");
        for (PasswordEntry entry : passwords) {
            sb.append("  <system name=\"" + Utils.escapeXMLChars(entry.getDecSystem()) + "\">\n");
            if (entry.getDecUsername() != null) {
                String username = "<![CDATA[" + entry.getDecUsername().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
                sb.append("    <username>" + username + "</username>\n");
            }
            if (entry.getDecPassword() != null) {
                String password = "<![CDATA[" + entry.getDecPassword().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
                sb.append("    <password>" + password + "</password>\n");
            }
            if (entry.getDecNote() != null) {  
                String note = "<![CDATA[" + entry.getDecNote().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
                sb.append("    <note>" + note + "</note>\n");
            }
            if (entry.getDecUrl() != null) {  
                String url = "<![CDATA[" + entry.getDecUrl().replaceAll("]]>", "]]>]]><![CDATA[") + "]]>";
                sb.append("    <url>" + url + "</url>\n");
            }
            sb.append("  </system>\n");
        }
        sb.append("</passdroid>\n");

        return sb.toString();
    }

    /**
     * Write a byte array to file. If a previous file with the same name exists
     * it is truncated.
     * 
     * @param filename  full path to the file.
     * @param data      byte array to write to file.
     * @throws IOException on failure.
     */
    private void writeToFile(String filename, byte [] data) throws IOException {
        File f = new File(filename);
        if (f.exists()) {
            f.delete();
        }

        f.createNewFile();
        OutputStream os = new FileOutputStream(f);
        os.write(data);
        os.close();
    }
}
