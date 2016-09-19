package com.github.worldsender.mcanm.client.model;

import net.minecraft.util.ResourceLocation;

public interface IRenderPassInformation extends IModelStateInformation {
	/**
	 * Retrieves the texture that should go into the texture slot slot
	 * 
	 * @param slot
	 *            the name of the texture slot getting polled
	 * @return the resource location of the texture to stick
	 */
	ResourceLocation getActualResourceLocation(String slot);
}
