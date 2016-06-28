package com.github.worldsender.mcanm.client.model;

import net.minecraft.util.ResourceLocation;

public interface IEntityRender {
	/**
	 * Retrieves the current animator
	 *
	 * @return
	 */
	IEntityAnimator getAnimator();

	/**
	 * Binds a texture
	 */
	void bindTextureFrom(ResourceLocation resLoc);
}
