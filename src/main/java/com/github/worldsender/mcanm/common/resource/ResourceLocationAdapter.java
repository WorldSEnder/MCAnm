package com.github.worldsender.mcanm.common.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class ResourceLocationAdapter implements IResourceLocation {
	private List<Consumer<IResourceLocation>> listeners = new ArrayList<>(3);

	/**
	 * This method should be called to notify all listeners of a reload of this resource
	 */
	protected void triggerReload() {
		Consumer<IResourceLocation>[] currentListeners;
		synchronized (listeners) {
			currentListeners = listeners.toArray(new Consumer[0]);
		}
		for (Consumer<IResourceLocation> listener : currentListeners) {
			listener.accept(this);
		}
	}

	@Override
	public boolean shouldCache() {
		return true;
	}

	@Override
	public void registerReloadListener(Consumer<IResourceLocation> reloadListener) {
		synchronized (listeners) {
			listeners.add(reloadListener);
			reloadListener.accept(this);
		}
	}

}
