package com.github.worldsender.mcanm.client.model.mhfcmodel.animation;

import com.google.common.base.Predicate;
/**
 * Kind of a default implementation for {@link IAnimatedObject} which means that
 * the user isn't bothered as much.
 * 
 * @author WorldSEnder
 *
 */
public abstract class AnimatedObjAdapter implements IAnimatedObject {

	@Override
	public IAnimation getCurrentAnimation() {
		return null;
	}

	@Override
	public int getCurrentFrame() {
		return 0;
	}

	@Override
	public Scale getScale() {
		return NO_SCALE;
	}

	@Override
	public Predicate<String> getPartPredicate(float subFrame) {
		return RENDER_ALL;
	}

}
