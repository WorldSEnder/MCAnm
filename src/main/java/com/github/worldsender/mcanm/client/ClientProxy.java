package com.github.worldsender.mcanm.client;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.client.model.mcanmmodel.MCMDModelLoader;
import com.github.worldsender.mcanm.client.model.util.AnimationLoader;
import com.github.worldsender.mcanm.client.model.util.ModelLoader;
import com.github.worldsender.mcanm.client.renderer.entity.RenderAnimatedModel;
import com.github.worldsender.mcanm.test.CubeEntity;

import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;

public class ClientProxy implements Proxy {
	@Override
	public void register() {
		AdvancedModelLoader.registerModelHandler(MCMDModelLoader.instance);
		IResourceManager resManager = Minecraft.getMinecraft().getResourceManager();
		if (resManager instanceof IReloadableResourceManager) {
			IReloadableResourceManager registry = (IReloadableResourceManager) resManager;
			registry.registerReloadListener((rm) -> AnimationLoader.instance.onResourceManagerReload(rm));
			registry.registerReloadListener((rm) -> ModelLoader.instance.onResourceManagerReload(rm));
		} else {
			MCAnm.logger
					.warn("Couldn't register reload managers. Models will not be reloaded on switching texture pack");
		}
		MCAnm.logger.info("Registered Reload Managers.");
		if (MCAnm.isDebug) {
			RenderingRegistry.registerEntityRenderingHandler(
					CubeEntity.class,
					RenderAnimatedModel.fromResLocation(new ResourceLocation("mcanm:models/Cube/Cube.mcmd"), 1.0f));
		}
	}
}
