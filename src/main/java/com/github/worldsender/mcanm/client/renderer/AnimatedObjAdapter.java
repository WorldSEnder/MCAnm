package com.github.worldsender.mcanm.client.renderer;

import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.RenderPassInformation;
/**
 * Kind of a default implementation for {@link IAnimatedObject} which means that
 * the user isn't bothered as much.
 *
 * @author WorldSEnder
 *
 */
public abstract class AnimatedObjAdapter implements IAnimatedObject {
	@Override
	public RenderPassInformation preRenderCallback(float subFrame,
			RenderPassInformation callback) {
		return callback; // Passthrough
	}
}
