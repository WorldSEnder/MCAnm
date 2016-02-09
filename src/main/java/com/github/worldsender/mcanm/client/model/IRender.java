package com.github.worldsender.mcanm.client.model;

import net.minecraft.util.ResourceLocation;

public interface IRender {
	/**
	 * Retrieves the current animator
	 *
	 * @return
	 */
	IAnimator getAnimator();

	/**
	 * Binds a texture
	 */
	void bindTexture(ResourceLocation resLoc);
}
