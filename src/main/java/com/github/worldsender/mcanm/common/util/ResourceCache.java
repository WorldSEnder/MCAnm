package com.github.worldsender.mcanm.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;

public class ResourceCache<D> {
	private Map<IResourceLocation, D> cache;

	public ResourceCache() {
		cache = new HashMap<>();
	}

	public D getOrCompute(IResource resource, Function<IResource, D> loadFunc) {
		IResourceLocation location = resource.getOrigin();
		if (location.shouldCache()) {
			location.registerReloadListener(this::changedCallback);
			return cache.computeIfAbsent(location, a -> loadFunc.apply(resource));
		}
		return loadFunc.apply(resource);
	}

	private void changedCallback(IResourceLocation location) {
		this.cache.remove(location);
	}

	public Optional<D> get(IResourceLocation location) {
		return Optional.ofNullable(cache.get(location));
	}
}
