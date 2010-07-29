package com.kodholken.passdroid;

public class FileImporterException extends Exception {
	private static final long serialVersionUID = 1L;

	public FileImporterException(String message) {
		super(message);
	}
	
	public FileImporterException(Exception ex) {
		super(ex);
	}
}
