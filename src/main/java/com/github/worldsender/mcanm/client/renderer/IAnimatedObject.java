package com.github.worldsender.mcanm.client.renderer;

import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.RenderPassInformation;
import com.github.worldsender.mcanm.client.renderer.entity.RenderAnimatedModel;
/**
 * An animatable type that is typically rendered with a
 * {@link RenderAnimatedModel} or a //TODO: RenderMHFCModelTile
 *
 * @author WorldSEnder
 *
 */
public interface IAnimatedObject {
	/**
	 * A callback to the animated object just before it is rendered. The method
	 * is always called BEFORE. The object should setup and return the
	 * {@link RenderPassInformation} given as callback (never null).
	 *
	 * @param subFrame
	 *            the current subFrame (subTick)
	 * @param callback
	 *            a {@link RenderPassInformation} to prepare
	 * @return A {@link RenderPassInformation} to use in the current pass, not
	 *         null
	 */
	public RenderPassInformation preRenderCallback(float subFrame,
			RenderPassInformation callback);
}
