package com.github.worldsender.mcanm.client;

import com.github.worldsender.mcanm.client.model.IRenderPassInformation;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

/**
 * An extension of only the user supplied information. This adds engine-based properties such as the used
 * {@link Tessellator}.
 *
 * @author WorldSEnder
 *
 */
public interface IRenderPass extends IRenderPassInformation {
	/**
	 * Binds the resource-location given.<br>
	 * <b>WARNING</b> This does not transform the resourc-location via
	 * {@link #getActualResourceLocation(ResourceLocation)}. This has the be done before calling this method.
	 *
	 * @param resLoc
	 *            the resource to bind
	 */
	void bindTexture(ResourceLocation resLoc);
}
