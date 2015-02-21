package com.github.worldsender.mcanm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.client.model.mcanmmodel.ModelRegistry;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.stored.AnimationRegistry;

public class ClientProxy implements Proxy {
	@Override
	public void register() {
		IResourceManager resManager = Minecraft.getMinecraft()
				.getResourceManager();
		if (resManager instanceof IReloadableResourceManager) {
			IReloadableResourceManager registry = (IReloadableResourceManager) resManager;
			registry.registerReloadListener(AnimationRegistry.instance);
			registry.registerReloadListener(ModelRegistry.instance);
		} else {
			MCAnm.logger
					.warn("Couldn't register reload managers. Models will not be reloaded on switching texture pack");
		}
		MCAnm.logger.info("Registered Reload Managers.");
	}
}
