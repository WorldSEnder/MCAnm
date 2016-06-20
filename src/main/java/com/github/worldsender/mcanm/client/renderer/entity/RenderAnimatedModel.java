package com.github.worldsender.mcanm.client.renderer.entity;

import com.github.worldsender.mcanm.client.ClientLoader;
import com.github.worldsender.mcanm.client.mcanmmodel.IModel;
import com.github.worldsender.mcanm.client.model.IEntityAnimator;
import com.github.worldsender.mcanm.client.model.IEntityRender;
import com.github.worldsender.mcanm.client.model.ModelAnimated;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;
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

	@Override
	protected void rotateCorpse(EntityLivingBase entity, float rotation, float yaw, float partialFrame) {
		// No-op. We do not want to do anything here
	}

	/**
	 * Convenience alternative to the constructor. The entity this render is used for has to extend
	 * {@link IAnimatedObject}.
	 * 
	 * @param model
	 *            the model to use
	 * @param shadowSize
	 *            the shadow size...
	 * @return
	 */
	public static RenderAnimatedModel fromModel(IModel model, float shadowSize) {
		return fromModel(IAnimatedObject.ANIMATOR_ADAPTER, model, shadowSize);
	}

	/**
	 * Convenience alternative to the constructor. A new {@link ModelAnimated} is instantiated from the
	 * {@link ResourceLocation} given (using the normal constructor).
	 *
	 * @param animator
	 *            the animator for the entity
	 * @param model
	 *            the model to use
	 * @param shadowSize
	 *            the shadow size...
	 * @return the constructed {@link RenderAnimatedModel}
	 * @see IEntityAnimator
	 * @see IEntityAnimator#STATIC_ENTITY
	 * @see IAnimatedObject#ANIMATOR_ADAPTER
	 * @see ClientLoader#loadModel(ResourceLocation, ISkeleton)
	 */
	public static RenderAnimatedModel fromModel(IEntityAnimator animator, IModel model, float shadowSize) {
		ModelAnimated mcmodel = new ModelAnimated(model);

		return new RenderAnimatedModel(mcmodel, animator, shadowSize);
	}
}
