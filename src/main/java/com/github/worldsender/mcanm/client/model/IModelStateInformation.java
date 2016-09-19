package com.github.worldsender.mcanm.client.model;

import com.github.worldsender.mcanm.common.animation.IAnimation;

public interface IModelStateInformation {

	IAnimation getAnimation();

	float getFrame();

	boolean shouldRenderPart(String part);

}
