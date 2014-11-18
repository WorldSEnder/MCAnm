package com.github.worldsender.mcanm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.client.model.AdvancedModelLoader;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.client.model.mhfcmodel.MCMDModelLoader;
import com.github.worldsender.mcanm.client.model.mhfcmodel.ModelRegistry;
import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.stored.AnimationRegistry;

public class ClientProxy implements Proxy {
	@Override
	public void register() {
		AdvancedModelLoader.registerModelHandler(MCMDModelLoader.instance);
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
		MCAnm.logger
				.info("Registered Reload Managers. Models will be realoaded on when changing the texture pack.");
	}
}
