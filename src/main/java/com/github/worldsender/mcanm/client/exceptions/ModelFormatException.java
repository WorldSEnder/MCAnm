package com.github.worldsender.mcanm.client.exceptions;

public class ModelFormatException extends IllegalArgumentException {
	/**
	 *
	 */
	private static final long serialVersionUID = -4000239134878464297L;

	public ModelFormatException() {
		super();
	}

	public ModelFormatException(String message) {
		super(message);
	}

	public ModelFormatException(Throwable cause) {
		super(cause);
	}

	public ModelFormatException(String message, Throwable cause) {
		super(message, cause);
	}
}
