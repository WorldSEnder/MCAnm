package com.github.worldsender.mcanm.client.model;

import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslatef;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.TextureOffset;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.client.model.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.client.model.util.ModelLoader;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;

/**
 * A general purpose model that should fulfill most of your needs. It uses a
 * {@link ModelMHMD} internally to render thus the registered entity class HAS
 * TO IMPLEMENT {@link IAnimatedObject}. This will throw an exception during
 * rendering otherwise.
 *
 * @author WorldSEnder
 *
 */
public class ModelAnimated extends ModelBase {
	private static boolean checkValidPartial(float newPartialTick) {
		return newPartialTick >= 0.0F && newPartialTick <= 1.0F;
	}

	protected ModelMCMD model;
	protected float partialTick;
	/**
	 * Loads the model from the given ResourceLocation using the
	 * {@link ModelLoader} thus this constructor is exception-free and will
	 * load the models from the Registry's chache if possible. You can give/load
	 * your own model with {@link #ModelAnimated(ModelMHMD)} to receive
	 * exceptions.
	 *
	 * @param resLoc
	 *            the {@link ResourceLocation} to load the model from
	 */
	public ModelAnimated(ResourceLocation resLoc) {
		this(ModelLoader.loadFrom(resLoc));
		// Useless piece of .... sklsdalsafhkjasd
		// So we don't get problems with arrows in our entity.
		// I want to kill the programmer who thought it would be a good idea
		// not to let the entity decide where to put the arrow
		ModelRenderer argggghhhh = new ModelRenderer(this, 0, 0);
		argggghhhh.addBox(0, 0, 0, 1, 1, 1);
	}
	/**
	 * This constructor just puts the model into itself. Nothing is checked
	 *
	 * @param model
	 *            the model to render
	 */
	public ModelAnimated(ModelMCMD model) {
		this.model = model;
	}
	/**
	 * Renders the underlying model.
	 */
	@Override
	public void render(Entity entity, float uLimbSwing,
			float interpolatedSwing, float uRotfloat, float headYaw,
			float interpolatedPitch, float size) {
		if (!(entity instanceof IAnimatedObject))
			throw new IllegalArgumentException(String.format(
					"Entity rendered must be an IAnimatedObject. EntityId %d",
					entity.getEntityId()));
		glPushMatrix();

		// Get our object into place
		glTranslatef(0, 1 / 128F, 0);
		glScalef(-size * 16, -size * 16, size * 16);
		glTranslatef(0, -1.5F, 0);
		// Actually render it
		IAnimatedObject animatedEntity = (IAnimatedObject) entity;
		this.model.render(animatedEntity, this.getPartialTick());

		glPopMatrix();
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
}
