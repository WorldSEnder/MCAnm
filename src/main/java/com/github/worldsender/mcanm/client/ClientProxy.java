package com.github.worldsender.mcanm.client;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.client.model.util.AnimationLoader;
import com.github.worldsender.mcanm.client.model.util.ModelLoader;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;
import com.github.worldsender.mcanm.client.renderer.entity.RenderAnimatedModel;
import com.github.worldsender.mcanm.test.CubeEntity;

import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class ClientProxy implements Proxy {
	@Override
	public void preInit() {
		IResourceManager resManager = Minecraft.getMinecraft().getResourceManager();
		if (resManager instanceof IReloadableResourceManager) {
			IReloadableResourceManager registry = (IReloadableResourceManager) resManager;
			registry.registerReloadListener((rm) -> AnimationLoader.instance.onResourceManagerReload(rm));
			registry.registerReloadListener((rm) -> ModelLoader.instance.onResourceManagerReload(rm));
		} else {
			MCAnm.logger()
					.warn("Couldn't register reload managers. Models will not be reloaded on switching resource pack");
		}
	}

	@Override
	public void init() {
		if (MCAnm.isDebug) {
			RenderAnimatedModel renderer = RenderAnimatedModel.fromResLocation(
					IAnimatedObject.ANIMATOR_ADAPTER,
					new ResourceLocation("mcanm:models/Cube/Cube.mcmd"),
					1.0f);
			RenderingRegistry.registerEntityRenderingHandler(CubeEntity.class, renderer);
		}
	}
}
