package com.github.worldsender.mcanm.client.model;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

public interface IEntityRender<T extends EntityLiving> {
	/**
	 * Retrieves the current animator
	 *
	 * @return
	 */
	IEntityAnimator<T> getAnimator();

	/**
	 * Binds a texture
	 */
	void bindTextureFrom(ResourceLocation resLoc);
}
