package com.github.worldsender.mcanm.client.model;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.IModel;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.TextureOffset;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

/**
 * A general purpose model that should fulfill most of your needs. It is possible to use an {@link IEntityAnimator} to
 * determine the rendered
 *
 * @author WorldSEnder
 *
 */
public class ModelAnimated extends ModelBase {
	private IModel model;
	private IRenderPass renderPass;

	/**
	 * This constructor just puts the model into itself. Nothing is checked
	 *
	 * @param model
	 *            the model to render
	 */
	public ModelAnimated(IModel model) {
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

		getModel().render(renderPass);

		GlStateManager.popMatrix();
	}

	// Will not use this method
	@Override
	public TextureOffset getTextureOffset(String boxName) {
		return new TextureOffset(0, 0);
	}

	/**
	 * The current model is accessed via this getter. If a subclass chooses to override this, the returned model will be
	 * rendered.
	 *
	 * @return
	 */
	protected IModel getModel() {
		return this.model;
	}

	public void setRenderPass(IRenderPass renderPass) {
		this.renderPass = renderPass;
	}
}
