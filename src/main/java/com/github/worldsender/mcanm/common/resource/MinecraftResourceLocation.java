package com.github.worldsender.mcanm.common.resource;

import java.io.IOException;
import java.util.Objects;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class MinecraftResourceLocation implements IResourceLocation {
	private final ResourceLocation loc;

	public MinecraftResourceLocation(ResourceLocation loc) {
		this.loc = Objects.requireNonNull(loc);
	}

	@Override
	public IResource openWith(IResourceManager manager) throws IOException {
		return new MinecraftResource(loc, manager);
	}

	@Override
	public boolean changesWithResourceManager() {
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loc == null) ? 0 : loc.hashCode());
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
		if (!(obj instanceof MinecraftResourceLocation)) {
			return false;
		}
		MinecraftResourceLocation other = (MinecraftResourceLocation) obj;
		if (loc == null) {
			if (other.loc != null) {
				return false;
			}
		} else if (!loc.equals(other.loc)) {
			return false;
		}
		return true;
	}
}
