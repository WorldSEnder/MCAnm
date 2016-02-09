package com.github.worldsender.mcanm.client.model;

import java.util.Objects;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.TextureOffset;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;

import com.github.worldsender.mcanm.client.model.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.client.model.util.RenderPass;
import com.github.worldsender.mcanm.client.model.util.RenderPassInformation;

/**
 * A general purpose model that should fulfill most of your needs. It is possible to use an {@link IAnimator} to
 * determine the rendered
 *
 * @author WorldSEnder
 *
 */
public class ModelAnimated extends ModelBase {
	private static boolean checkValidPartial(float newPartialTick) {
		return newPartialTick >= 0.0F && newPartialTick <= 1.0F;
	}

	private ModelMCMD model;
	private float partialTick;
	private IRender renderer; // The renderer, used to bind textures

	private RenderPassInformation userPassCache = new RenderPassInformation();
	private RenderPass passCache = new RenderPass(userPassCache, null, null);

	/**
	 * This constructor just puts the model into itself. Nothing is checked
	 *
	 * @param model
	 *            the model to render
	 */
	public ModelAnimated(ModelMCMD model) {
		this.model = model; // No null-checks, getters could be overridden
		// Useless piece of .... sklsdalsafhkjasd
		// So we don't get problems with arrows in our entity.
		// I want to kill the programmer who thought it would be a good idea
		// not to let the entity decide where to put the arrow
		ModelRenderer argggghhhh = new ModelRenderer(this, 0, 0);
		argggghhhh.addBox(0, 0, 0, 1, 1, 1);
	}

	/**
	 * Renders the underlying model.
	 */
	@Override
	public void render(
			Entity entity,
			float uLimbSwing,
			float interpolatedSwing,
			float uRotfloat,
			float headYaw,
			float interpolatedPitch,
			float size) {
		GlStateManager.pushMatrix();

		// Get our object into place
		GlStateManager.scale(-1 * size * 16, -1 * size * 16, size * 16);
		GlStateManager.translate(0, -1.5f - 1.5f * size, 0);

		userPassCache.reset();
		IRender currentRender = getRender();
		IRenderPassInformation currentPass = currentRender.getAnimator().preRenderCallback(
				entity,
				userPassCache,
				getPartialTick(),
				uLimbSwing,
				interpolatedSwing,
				uRotfloat,
				headYaw,
				interpolatedPitch);
		passCache.setRenderPassInformation(currentPass).setTesellator(Tessellator.getInstance())
				.setRender(currentRender);

		getModel().render(passCache);

		GlStateManager.popMatrix();
	}

	// Will not use this method
	@Override
	public TextureOffset getTextureOffset(String boxName) {
		return new TextureOffset(0, 0);
	}

	public float getPartialTick() {
		return this.partialTick;
	}

	public void setPartialTick(float newPartialTick) {
		if (checkValidPartial(newPartialTick))
			this.partialTick = newPartialTick;
	}

	/**
	 * The current model is accessed via this getter. If a subclass chooses to override this, the returned model will be
	 * rendered.
	 *
	 * @return
	 */
	protected ModelMCMD getModel() {
		return this.model;
	}

	/**
	 * Returns the currently used renderer. Here to be overridden by subclasses
	 */
	protected IRender getRender() {
		return this.renderer;
	}

	/**
	 * Sets the given {@link IRender} as the current Render. Users should not use this, only if they are the coding the
	 * render.<br>
	 * The Render however should always call this at least once before rendering the model, as the render is used to
	 * bind the textures of the model.
	 *
	 * @param newRender
	 * @return this, to make this method builder-pattern-like
	 */
	public ModelAnimated setRender(IRender newRender) {
		this.renderer = Objects.requireNonNull(newRender);
		return this;
	}
}
