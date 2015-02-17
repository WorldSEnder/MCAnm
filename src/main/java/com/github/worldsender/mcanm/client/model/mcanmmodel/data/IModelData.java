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
	public void renderAll(IAnimation currAttack, float frame);
	public void renderFiltered(Predicate<String> filter, IAnimation currAttack,
			float frame);
}
