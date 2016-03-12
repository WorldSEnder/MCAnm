package com.github.worldsender.mcanm.common.resource;

import java.io.IOException;
import java.net.URL;

import com.github.worldsender.mcanm.common.util.CallResolver;

import net.minecraft.client.resources.IResourceManager;

public class EmbeddedResourceLocation implements IResourceLocation {
	private final URL url;

	public EmbeddedResourceLocation(String name) {
		this(name, CallResolver.INSTANCE.getCallingClass().getClassLoader());
	}

	public EmbeddedResourceLocation(String name, ClassLoader loader) {
		this(loader.getResource(name));
	}

	public EmbeddedResourceLocation(URL url) {
		this.url = url;
	}

	@Override
	public boolean changesWithResourceManager() {
		return false;
	}

	@Override
	public IResource openWith(IResourceManager manager) throws IOException {
		return new EmbeddedResource(url);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof EmbeddedResourceLocation)) {
			return false;
		}
		EmbeddedResourceLocation other = (EmbeddedResourceLocation) obj;
		if (url == null) {
			if (other.url != null) {
				return false;
			}
		} else if (!url.equals(other.url)) {
			return false;
		}
		return true;
	}
}
