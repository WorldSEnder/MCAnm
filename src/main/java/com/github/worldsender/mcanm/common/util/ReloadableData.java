package com.github.worldsender.mcanm.common.util;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;

import net.minecraft.util.ResourceLocation;

public abstract class ReloadableData<D> {

	// Keep this around so that it doesn't get garbage collected
	private final IResourceLocation reloadLocation;
	private Function<IResource, D> loader;
	private D latestData;
	private D defaultData;

	/**
	 * Constructs Reloadable Data by first opening the initial resource and then reading from it.<br>
	 * Any time loading from the supplied {@link IResourceLocation} fails because of an {@link IOException}, instead the
	 * defaultData is assumed.<br>
	 * <b>This constructor will instantly fire a load. If you wish to do any setup you may supply a Runnable to the
	 * constructor which will be run shortly before the resource is loaded. This is to trick java into running your
	 * constructor code before the resource is first openened.
	 * 
	 * @param initial
	 *            the initial location
	 * @param loader
	 *            a function that loads the raw data from a specific {@link IResource}.
	 * @param defaultData
	 *            the data to use if loading fails (e.g. IOException)
	 * @param additionalArgs
	 *            these arguments will be forwarded to preInit. Use this to initialize before the resource is loaded the
	 *            first time
	 * @throws IllegalArgumentException
	 *             iff initial fails and reloadLocation returns null
	 */
	public ReloadableData(
			IResourceLocation initial,
			Function<? super IResource, D> loader,
			D defaultData,
			Object... additionalArgs) {
		Objects.requireNonNull(initial);
		Objects.requireNonNull(loader);
		Objects.requireNonNull(defaultData);
		// We do this strange lambda because of java's shitty generic capture...
		this.loader = loader::apply;
		this.reloadLocation = initial;
		this.defaultData = defaultData;
		preInit(additionalArgs);
		initial.registerReloadListener(this::reload);
	}

	// For the leisure of sub-objects.... fuck java
	protected void preInit(Object... args) {

	}

	private D getData() {
		try {
			return loader.apply(reloadLocation.open());
		} catch (IOException ioe) {
			MCAnm.logger().error("Failed loading data from " + reloadLocation, ioe);
			return defaultData;
		}
	}

	private void reload(IResourceLocation dummy) {
		assert dummy == reloadLocation;
		loadData(getData());
	}

	protected abstract void loadData(D data);

	/**
	 * Gets the {@link ResourceLocation} this model was loaded from.
	 *
	 * @return
	 */
	public IResourceLocation getResourceLocation() {
		return this.reloadLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((latestData == null) ? 0 : latestData.hashCode());
		result = prime * result + ((reloadLocation == null) ? 0 : reloadLocation.hashCode());
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
		if (!(obj instanceof ReloadableData)) {
			return false;
		}
		ReloadableData<?> other = (ReloadableData<?>) obj;
		if (latestData == null) {
			if (other.latestData != null) {
				return false;
			}
		} else if (!latestData.equals(other.latestData)) {
			return false;
		}
		if (reloadLocation == null) {
			if (other.reloadLocation != null) {
				return false;
			}
		} else if (!reloadLocation.equals(other.reloadLocation)) {
			return false;
		}
		return true;
	}
}
