package com.github.worldsender.mcanm.client.renderer;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
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
	public IAnimation getCurrentAnimation(float subFrame) {
		return null;
	}

	@Override
	public float getCurrentFrame(float subFrame) {
		return 0.0f;
	}

	@Override
	public void preRenderCallback(float subFrame) {}

	@Override
	public Predicate<String> getPartPredicate(float subFrame) {
		return RENDER_ALL;
	}

}
