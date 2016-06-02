package com.github.worldsender.mcanm.common.resource;

import java.io.Closeable;
import java.io.DataInputStream;

public interface IResource extends Closeable {
	/**
	 * Gets the currently open DataInputStream.
	 * 
	 * @return
	 */
	DataInputStream getInputStream();

	default String getResourceName() {
		return getOrigin().getResourceName();
	}

	/**
	 * Gets the origin of the resource. If the resource has no conventional, then
	 * 
	 * @return
	 */
	IResourceLocation getOrigin();
}
