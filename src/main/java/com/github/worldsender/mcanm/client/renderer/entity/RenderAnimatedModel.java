package com.github.worldsender.mcanm.client.renderer.entity;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.ModelMCMD;
import com.github.worldsender.mcanm.client.model.IEntityAnimator;
import com.github.worldsender.mcanm.client.model.IEntityRender;
import com.github.worldsender.mcanm.client.model.ModelAnimated;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class RenderAnimatedModel extends RenderLiving implements IEntityRender {
	private static final ResourceLocation ignored = TextureMap.locationBlocksTexture;

	protected ModelAnimated model;
	private IEntityAnimator animator;

	public RenderAnimatedModel(ModelAnimated model, IEntityAnimator animator, float shadowSize) {
		super(model, shadowSize);
		this.model = model.setRender(this);
		this.animator = animator;
	}

	@Override
	public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
		super.doRender(entity, x, y, z, yaw, partialTicks);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		// Ignored anyway, there is not only one texture, probably
		return ignored;
	}

	@Override
	protected void preRenderCallback(EntityLivingBase entity, float partialRick) {
		this.model.setPartialTick(partialRick);
	}

	@Override
	public IEntityAnimator getAnimator() {
		return this.animator;
	}

	@Override
	public void bindTexture(ResourceLocation texLocation) {
		super.bindTexture(texLocation);
	}

	/**
	 * Convenience alternative to the constructor. A new {@link ModelAnimated} is instanciated from the
	 * {@link ResourceLocation} given (using the normal constructor).
	 *
	 * @param animator
	 *            the animator for the entity
	 * @param resLoc
	 *            the resource location to load the entity from
	 * @param shadowSize
	 *            the shadow size...
	 * @return the constructed {@link RenderAnimatedModel}
	 * @see IEntityAnimator
	 */
	public static RenderAnimatedModel fromResLocation(
			IEntityAnimator animator,
			ResourceLocation resLoc,
			ISkeleton skeleton,
			float shadowSize) {
		ModelMCMD rawmodel = new ModelMCMD(resLoc, skeleton);
		ModelAnimated mcmodel = new ModelAnimated(rawmodel);

		return new RenderAnimatedModel(mcmodel, animator, shadowSize);
	}
}
