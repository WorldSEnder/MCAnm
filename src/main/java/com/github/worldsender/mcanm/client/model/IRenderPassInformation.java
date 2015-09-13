package com.github.worldsender.mcanm.client.model;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.google.common.base.Predicate;

public interface IRenderPassInformation {
	IAnimation getAnimation();

	float getFrame();

	Predicate<String> getPartPredicate();
}
