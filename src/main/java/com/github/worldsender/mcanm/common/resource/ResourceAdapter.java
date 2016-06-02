package com.github.worldsender.mcanm.common.resource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class ResourceAdapter implements IResource {
	private static DataInputStream convertChecked(InputStream source) throws IOException {
		if (source == null) {
			throw new IOException("Can't read from null");
		}
		return new DataInputStream(source);
	}

	private final DataInputStream dis;
	private final IResourceLocation resLoc;

	public ResourceAdapter(InputStream managed) throws IOException {
		this(IResourceLocation.UNKNOWN_LOCATION, managed);
	}

	public ResourceAdapter(IResourceLocation parent, InputStream managed) throws IOException {
		this(parent, convertChecked(managed));
	}

	public ResourceAdapter(IResourceLocation parent, DataInputStream managed) {
		dis = Objects.requireNonNull(managed);
		resLoc = parent;
	}

	@Override
	public void close() throws IOException {
		dis.close();
	}

	@Override
	public DataInputStream getInputStream() {
		return dis;
	}

	@Override
	public IResourceLocation getOrigin() {
		return resLoc;
	}
}
