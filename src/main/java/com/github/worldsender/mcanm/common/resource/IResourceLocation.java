package com.github.worldsender.mcanm.common.resource;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;

public interface IResourceLocation {
	/**
	 * For data that has no definite location, this is suitable (e.g. one-time streams)
	 */
	public static final IResourceLocation UNKNOWN_LOCATION = new IResourceLocation() {
		@Override
		public IResource open() throws IOException {
			throw new NoSuchFileException("Unknown location");
		}

		@Override
		public String getResourceName() {
			return "<unknown resource>";
		}

		@Override
		public boolean shouldCache() {
			return false;
		}

		@Override
		public void registerReloadListener(Consumer<IResourceLocation> reloadListener) {
			reloadListener.accept(this);
			// Don't store it, this location won't change
		}
	};

	/**
	 * Opens the resource location described by this resource location.
	 * 
	 * @return an empty optional if the resource is currently (expectedly) absent
	 * @throws IOException
	 *             if the resource is absent unexpectedly
	 */
	IResource open() throws IOException;

	/**
	 * Registers a listener that is being fired every time this resource changes. What "changes" means is deliberately
	 * left unspecified but a good example would be whenever the {@link Minecraft#getResourceManager()} changes, for
	 * example when a new resource pack is loaded.<br>
	 * <b>The initial stick is considered a change already, so this method should also fire the listener once.</b>
	 * 
	 * @param reloadListener
	 */
	void registerReloadListener(Consumer<IResourceLocation> reloadListener);

	/**
	 * A descriptive name of the resource, perhaps the path
	 * 
	 * @return
	 */
	String getResourceName();

	/**
	 * Returns false if this resource should not be cached, e.g. it is a one-time stream. When a
	 * {@link IResourceLocation} may get cached, {@link #open()} should succeed multiple times and return the same data
	 * every time, until the resource is reloaded.
	 * 
	 * @return
	 */
	boolean shouldCache();

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();
}
