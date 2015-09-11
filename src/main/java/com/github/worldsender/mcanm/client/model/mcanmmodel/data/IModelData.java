package com.github.worldsender.mcanm.client.model.mcanmmodel.data;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.GLHelper;
import com.google.common.base.Predicate;

/**
 * Interface for ModelData used by {@link GLHelper} classes.
 *
 * @author WorldSEnder
 *
 */
public interface IModelData {
	/**
	 * Setup the animation and frame given and prepares the model.
	 *
	 * @param currAnimation
	 * @param frame
	 */
	public void setup(IAnimation currAnimation, float frame);

	// public default void renderAll() {
	// renderFiltered(RenderPassInformation.RENDER_ALL);
	// }

	/**
	 * Render the parts of the model that match the given predicate
	 *
	 * @param partFilter
	 *            a filter to only render some parts of the model.
	 */
	public void render(Predicate<String> partFilter);
}
