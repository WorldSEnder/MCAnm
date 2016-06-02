package com.github.worldsender.mcanm.client;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.client.mcanmmodel.IModel;
import com.github.worldsender.mcanm.client.model.IEntityAnimator;
import com.github.worldsender.mcanm.client.renderer.entity.RenderAnimatedModel;
import com.github.worldsender.mcanm.common.CommonLoader;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.resource.MinecraftResourcePool;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.test.CubeEntity;
import com.github.worldsender.mcanm.test.CubeEntityV2;

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
			registry.registerReloadListener(this::reload);
		} else {
			MCAnm.logger()
					.warn("Couldn't register reload managers. Models will not be reloaded on switching resource pack");
		}
	}

	@Override
	public void init() {
		if (MCAnm.isDebug) {
			ResourceLocation modelSrc = new ResourceLocation("mcanm:models/Cube/Cube.mcmd");
			ISkeleton skeleton = CommonLoader.loadLegacySkeleton(modelSrc);
			IModel model = ClientLoader.loadModel(modelSrc, skeleton);
			RenderAnimatedModel renderer = RenderAnimatedModel.fromModel(model, 1.0f);

			ResourceLocation model2Src = new ResourceLocation("mcanm:models/CubeV2/Cube.mcmd");
			IModel model2 = ClientLoader.loadModel(model2Src, ISkeleton.EMPTY);
			RenderAnimatedModel renderer2 = RenderAnimatedModel.fromModel(IEntityAnimator.STATIC_ENTITY, model2, 1.0f);

			RenderingRegistry.registerEntityRenderingHandler(CubeEntity.class, renderer);
			RenderingRegistry.registerEntityRenderingHandler(CubeEntityV2.class, renderer2);
		}
	}

	private void reload(IResourceManager newManager) {
		if (!MCAnm.configuration().isReloadEnabled()) {
			return;
		}
		MinecraftResourcePool.instance.onResourceManagerReloaded();
	}

	@Override
	public IResourceLocation getSidedResource(ResourceLocation resLoc) {
		return MinecraftResourcePool.instance.makeResourceLocation(resLoc);
	}
}
