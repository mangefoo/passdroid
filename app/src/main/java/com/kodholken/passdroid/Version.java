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

/**
 * Container for version numbers
 */
public class Version implements Comparable<Version> {
    private int major;
    private int minor;

    public Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    /**
     * Create a Version instance from a string on the format "<major>.<minor>".
     * 
     * @param str  the version string
     * @return     Version instance
     * @throws NumberFormatException if the supplied string does not correspond
     *                               to the required format.
     */
    public static Version parse(String str) throws NumberFormatException {
        int dec = str.indexOf('.');
        if (dec <= 0) {
            throw new NumberFormatException("Invalid version " + str);
        }

        String major = str.substring(0, dec);
        String minor = str.substring(dec + 1);

        int intMajor = Integer.parseInt(major);
        int intMinor = Integer.parseInt(minor);

        if (intMajor < 0 || intMinor < 0) {
            throw new NumberFormatException("Invalid version " + str);
        }

        return new Version(intMajor, intMinor);
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    /**
     * Implementation of Comparable<Version>.compareTo()
     */
    @Override
    public int compareTo(Version another) {
        if (major == another.major) {
            if (minor == another.minor) {
                return 0;
            }

            return (minor - another.minor);
        }

        return (major - another.major);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Version)) {
            return false;
        }

        return (compareTo((Version) object) == 0);
    }

    @Override
    public String toString() {
        return major + "." + minor;
    }
}
