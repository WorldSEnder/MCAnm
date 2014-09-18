package com.github.worldsender.mcanm.client.model.mhfcmodel.data;

import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mhfcmodel.glcontext.GLHelper;
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
