package com.github.worldsender.mcanm.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.client.model.IAnimator;
import com.github.worldsender.mcanm.client.model.ModelAnimated;
import com.github.worldsender.mcanm.client.model.ModelLoader;
import com.github.worldsender.mcanm.client.model.mcanmmodel.ModelMCMD;

public class RenderAnimatedModel extends RenderLiving {
	private static final ResourceLocation ignored = TextureMap.locationBlocksTexture;

	protected ModelAnimated model;

	public RenderAnimatedModel(RenderManager manager, ModelAnimated model,
			float shadowSize) {
		super(manager, model, shadowSize);
		this.model = model;
	}

	@Override
	public void doRender(EntityLiving entity, double x, double y, double z,
			float yaw, float partialTicks) {
		super.doRender(entity, x, y, z, yaw, partialTicks);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		// Ignored anyway, so we load any resource location
		return ignored;
	}

	@Override
	protected void preRenderCallback(EntityLivingBase entity, float partialRick) {
		this.model.setPartialTick(partialRick);
	}
	/**
	 * Convenience alternative to the constructor. A new {@link ModelAnimated}
	 * is instanciated from the {@link ResourceLocation} given (using the normal
	 * constructor).
	 *
	 * @param animator
	 *            the animator for the entity
	 * @param resLoc
	 *            the resource location to load the entity from
	 * @param shadowSize
	 *            the shadow size...
	 * @return the constructed {@link RenderAnimatedModel}
	 * @see IAnimator
	 */
	public static RenderAnimatedModel fromResLocation(IAnimator animator,
			ResourceLocation resLoc, float shadowSize) {
		ModelMCMD rawmodel = ModelLoader.loadFrom(resLoc);
		RenderManager manager = Minecraft.getMinecraft().getRenderManager();
		ModelAnimated mcmodel = new ModelAnimated(rawmodel, animator);

		return new RenderAnimatedModel(manager, mcmodel, shadowSize);
	}
}
