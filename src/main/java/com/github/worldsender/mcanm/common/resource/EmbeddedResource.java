package com.github.worldsender.mcanm.common.resource;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import com.github.worldsender.mcanm.common.util.CallResolver;

public class EmbeddedResource extends ResourceAdapter {
	private final URL url;

	private static DataInputStream createStream(URL url) throws IOException {
		if (url == null) {
			throw new IOException("Couldn't open the stream");
		}
		return new DataInputStream(url.openStream());
	}

	public EmbeddedResource(String name) throws IOException {
		this(name, CallResolver.INSTANCE.getCallingClass().getClassLoader());
	}

	public EmbeddedResource(String name, ClassLoader loader) throws IOException {
		this(loader.getResource(name));
	}

	public EmbeddedResource(URL url) throws IOException {
		super(createStream(url));
		this.url = url;
	}

	@Override
	public String getResourceName() {
		return url.toString();
	}

	@Override
	public Optional<IResourceLocation> getOrigin() {
		return Optional.of(new EmbeddedResourceLocation(url));
	}
}
