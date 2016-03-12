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
		Optional<IResourceLocation> location = resource.getOrigin();
		if (location.isPresent()) {
			IResourceLocation replayableLocation = location.get();
			return cache.computeIfAbsent(replayableLocation, a -> loadFunc.apply(resource));
		}
		return loadFunc.apply(resource);
	}

	public Optional<D> get(IResourceLocation location) {
		return Optional.ofNullable(cache.get(location));
	}

	public void onResourceManagerReloaded() {
		cache.entrySet().removeIf(e -> e.getKey().changesWithResourceManager());
	}
}
