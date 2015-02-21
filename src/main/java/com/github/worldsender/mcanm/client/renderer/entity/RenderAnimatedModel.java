package com.github.worldsender.mcanm.client.renderer.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.client.model.ModelAnimated;

public class RenderAnimatedModel extends RenderLiving {
	private static final ResourceLocation ignored = TextureMap.locationBlocksTexture;

	protected ModelAnimated model;

	public RenderAnimatedModel(RenderManager manager, ModelAnimated model,
			float shadowSize) {
		super(manager, model, shadowSize);
		this.model = model;
	}

	@Override
	public void doRender(EntityLiving entity, double uk1, double uk2,
			double uk3, float uk4, float size) {
		super.doRender(entity, uk1, uk2, uk3, uk4, size);
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
	 * @param resLoc
	 * @param shadowSize
	 * @return
	 */
	public static RenderAnimatedModel fromResLocation(ResourceLocation resLoc,
			float shadowSize) {
		return new RenderAnimatedModel(Minecraft.getMinecraft()
				.getRenderManager(), new ModelAnimated(resLoc), shadowSize);
	}
}
