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
	public void renderAll();
	public void renderFiltered(Predicate<String> filter);
}
