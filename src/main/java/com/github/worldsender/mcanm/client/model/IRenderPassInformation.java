package com.github.worldsender.mcanm.client.model;

import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;

public interface IRenderPassInformation {
	IAnimation getAnimation();

	float getFrame();

	boolean shouldRenderPart(String part);

	/**
	 * Offers the {@link IRenderPassInformation} a possibility to transform the
	 * {@link ResourceLocation} used to bind the texture of the model to any
	 * other location. Useful if the texture is to be decided based on some
	 * state of the entity.
	 *
	 * @return
	 */
	ResourceLocation getActualResourceLocation(ResourceLocation in);
}
