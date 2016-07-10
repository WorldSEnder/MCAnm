package com.github.worldsender.mcanm.client.renderer.entity;

import com.github.worldsender.mcanm.client.ClientLoader;
import com.github.worldsender.mcanm.client.mcanmmodel.IModel;
import com.github.worldsender.mcanm.client.model.IEntityAnimator;
import com.github.worldsender.mcanm.client.model.IEntityRender;
import com.github.worldsender.mcanm.client.model.IRenderPassInformation;
import com.github.worldsender.mcanm.client.model.ModelAnimated;
import com.github.worldsender.mcanm.client.model.util.RenderPass;
import com.github.worldsender.mcanm.client.model.util.RenderPassInformation;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class RenderAnimatedModel<T extends EntityLiving> extends RenderLiving<T> implements IEntityRender<T> {
	private static final ResourceLocation ignored = TextureMap.LOCATION_MISSING_TEXTURE;

	protected ModelAnimated model;
	private IEntityAnimator<T> animator;

	private RenderPassInformation userPassCache = new RenderPassInformation();
	private RenderPass<T> passCache = new RenderPass<>(userPassCache, this);
	private float partialTick;

	public RenderAnimatedModel(
			RenderManager manager,
			ModelAnimated model,
			IEntityAnimator<T> animator,
			float shadowSize) {
		super(manager, model, shadowSize);
		this.model = model;
		model.setRenderPass(passCache);
		this.animator = animator;
	}

	@Override
	protected void renderModel(
			T entity,
			float uLimbSwing,
			float interpolatedSwing,
			float uRotfloat,
			float headYaw,
			float interpolatedPitch,
			float scaleFactor) {
		userPassCache.reset();
		IRenderPassInformation currentPass = getAnimator().preRenderCallback(
				entity,
				userPassCache,
				partialTick,
				uLimbSwing,
				interpolatedSwing,
				uRotfloat,
				headYaw,
				interpolatedPitch);
		passCache.setRenderPassInformation(currentPass);
		super.renderModel(entity, uLimbSwing, interpolatedSwing, uRotfloat, headYaw, interpolatedPitch, scaleFactor);
	}

	@Override
	protected ResourceLocation getEntityTexture(T entity) {
		return ignored;
	}

	@Override
	protected void preRenderCallback(T entity, float partialTick) {
		this.partialTick = partialTick;
	}

	@Override
	public IEntityAnimator<T> getAnimator() {
		return this.animator;
	}

	@Override
	public void bindTextureFrom(ResourceLocation resLoc) {
		this.bindTexture(resLoc);
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
	public static <T extends EntityLiving & IAnimatedObject> IRenderFactory<T> fromModel(
			IModel model,
			float shadowSize) {
		return fromModel(IAnimatedObject.ANIMATOR_ADAPTER(), model, shadowSize);
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
	public static <T extends EntityLiving> IRenderFactory<T> fromModel(
			IEntityAnimator<T> animator,
			IModel model,
			float shadowSize) {
		ModelAnimated mcmodel = new ModelAnimated(model);

		return manager -> new RenderAnimatedModel<>(manager, mcmodel, animator, shadowSize);
	}
}
