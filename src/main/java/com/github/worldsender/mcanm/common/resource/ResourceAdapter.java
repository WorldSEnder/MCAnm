package com.github.worldsender.mcanm.common.resource;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class ResourceAdapter implements IResource {
	private final DataInputStream dis;

	public ResourceAdapter(InputStream managed) throws IOException {
		if (managed == null) {
			throw new IOException("Can't read from null");
		}
		dis = new DataInputStream(managed);
	}

	public ResourceAdapter(DataInputStream managed) {
		dis = Objects.requireNonNull(managed);
	}

	@Override
	public void close() throws IOException {
		dis.close();
	}

	@Override
	public DataInputStream getInputStream() {
		return dis;
	}
}
