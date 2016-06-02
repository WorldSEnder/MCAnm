package com.github.worldsender.mcanm.common.resource;

import java.io.IOException;
import java.util.Objects;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class MinecraftResourceLocation extends ResourceLocationAdapter {
	private /*static*/ class MinecraftResource extends ResourceAdapter {
		public MinecraftResource(ResourceLocation resLoc, IResourceManager manager) throws IOException {
			super(MinecraftResourceLocation.this, manager.getResource(resLoc).getInputStream());
		}
	}

	private final ResourceLocation loc;
	private final MinecraftResourcePool pool;

	/**
	 * Warning: You should probably use {@link MinecraftResourcePool#instance#makeResourceLocation(ResourceLocation)}
	 * else the resource won't listen to resource pack reloads.
	 * 
	 * @param loc
	 */
	public MinecraftResourceLocation(ResourceLocation loc) {
		this(MinecraftResourcePool.instance, loc);
	}

	/**
	 * Warning: You should probably use {@link MinecraftResourcePool#instance#makeResourceLocation(ResourceLocation)}
	 * else the resource won't listen to resource pack reloads.
	 * 
	 * @param loc
	 */
	public MinecraftResourceLocation(MinecraftResourcePool pool, ResourceLocation loc) {
		this.loc = Objects.requireNonNull(loc);
		this.pool = Objects.requireNonNull(pool);
	}

	@Override
	public IResource open() throws IOException {
		return new MinecraftResource(loc, pool.getCurrentResourceManager());
	}

	@Override
	public String getResourceName() {
		return loc.toString();
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
