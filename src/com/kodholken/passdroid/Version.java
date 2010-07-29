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
