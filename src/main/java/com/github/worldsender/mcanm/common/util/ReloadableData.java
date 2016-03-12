package com.github.worldsender.mcanm.common.util;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.util.ExceptionLessFunctions.ThrowingFunction;
import com.github.worldsender.mcanm.common.util.ExceptionLessFunctions.ThrowingSupplier;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public abstract class ReloadableData<D> {

	public static <D> Function<? super IResource, Optional<D>> convertThrowingLoader(
			final ThrowingFunction<? super IResource, ? extends D, ModelFormatException> direct) {
		return convertThrowingLoader(direct, "Error loading from %s");
	}

	public static <D> Function<? super IResource, Optional<D>> convertThrowingLoader(
			final ThrowingFunction<? super IResource, ? extends D, ModelFormatException> direct,
			String errorMessage) {
		Objects.requireNonNull(errorMessage);
		return convertThrowingLoader(direct, r -> String.format(errorMessage, r.getResourceName()));
	}

	public static <D> Function<? super IResource, Optional<D>> convertThrowingLoader(
			final ThrowingFunction<? super IResource, ? extends D, ModelFormatException> direct,
			final Function<? super IResource, ? extends String> error) {
		Objects.requireNonNull(direct);
		Objects.requireNonNull(error);
		return r -> {
			try {
				return Optional.of(direct.apply(r));
			} catch (ModelFormatException mfe) {
				MCAnm.logger().error(error.apply(r), mfe);
			}
			return Optional.empty();
		};
	}

	private final Optional<IResourceLocation> reloadLocation;
	private Function<Optional<IResource>, D> loader;
	private D lastData;

	public ReloadableData(IResource initial, Function<? super IResource, Optional<D>> loader, D defaultData) {
		this(() -> initial, () -> null, loader, defaultData);
	}

	/**
	 * Constructs Reloadable Data by first opening the initial resource and then reading from it.<br>
	 * Iff opening the resource throws an IOException, then reloadLocation must return a non-null,
	 * {@link IResourceLocation} to load the resource from on the next reload. If it does not, that is considered an
	 * {@link IllegalArgumentException} because the resource can not ever be anything else but the default data.<br>
	 * Iff opening the resource from initial succeeds, reloadLocation will not be called and the location will be
	 * 
	 * @param initial
	 *            the initial location
	 * @param reloadLocation
	 *            the reload location
	 * @param loader
	 *            a function that loads the raw data from a specific {@link IResource}.
	 * @param defaultData
	 *            the data to use if loading fails
	 * @throws IllegalArgumentException
	 *             iff initial fails and reloadLocation returns null
	 */
	public <U extends D> ReloadableData(
			ThrowingSupplier<? extends IResource, IOException> initial,
			Supplier<? extends IResourceLocation> reloadLocation,
			Function<? super IResource, Optional<U>> loader,
			D defaultData) {
		Objects.requireNonNull(initial);
		Objects.requireNonNull(reloadLocation);
		Objects.requireNonNull(loader);
		this.loader = o -> {
			Optional<U> loaded = o.flatMap(loader);
			if (loaded.isPresent()) {
				return loaded.get();
			}
			return defaultData;
		};
		Optional<IResource> resource;
		Optional<IResourceLocation> reload;
		try {
			IResource res = initial.get();
			resource = Optional.of(res);
			reload = res.getOrigin();
		} catch (IOException ioe) {
			resource = Optional.empty();
			IResourceLocation resLoc = reloadLocation.get();
			if (resLoc == null) {
				throw new IllegalArgumentException(
						"Loading the initial resource has failed, but the reloadLocation is also null",
						ioe);
			}
			reload = Optional.of(resLoc);
		}
		this.reloadLocation = reload;
		loadFrom(resource);
	}

	public void reload(IResourceManager newManager) {
		Optional<IResource> res = reloadLocation.map(l -> {
			try {
				return l.openWith(newManager);
			} catch (IOException ioe) {
				return null;
			}
		});
		loadFrom(res);
	}

	private void loadFrom(Optional<IResource> resource) {
		lastData = loader.apply(resource);
		loadData(lastData);
	}

	protected abstract void loadData(D data);

	/**
	 * Gets the {@link ResourceLocation} this model was loaded from. This may be <code>null</code> if this model was
	 * loaded from a stream.
	 *
	 * @return
	 */
	public Optional<IResourceLocation> getResourceLocation() {
		return this.reloadLocation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lastData == null) ? 0 : lastData.hashCode());
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
		if (lastData == null) {
			if (other.lastData != null) {
				return false;
			}
		} else if (!lastData.equals(other.lastData)) {
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
