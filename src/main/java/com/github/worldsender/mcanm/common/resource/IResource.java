package com.github.worldsender.mcanm.common.resource;

import java.io.Closeable;
import java.io.DataInputStream;
import java.util.Optional;

public interface IResource extends Closeable {
	/**
	 * Gets the currently open DataInputStream.
	 * 
	 * @return
	 */
	DataInputStream getInputStream();

	String getResourceName();

	Optional<IResourceLocation> getOrigin();
}
