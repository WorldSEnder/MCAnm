package com.github.worldsender.mcanm.common.skeleton;

import com.github.worldsender.mcanm.common.animation.IAnimation;

import net.minecraft.client.renderer.Tessellator;

public interface ISkeleton {

	IBone getBoneByName(String bone);

	IBone getBoneByIndex(int index);

	/**
	 * Sets up the Skeleton for the animation given
	 */
	void setup(IAnimation animation, float frame);

	/**
	 * Added for debug, don't actually use this, especially when on the server
	 * 
	 * @param tess
	 */
	void debugDraw(Tessellator tess);

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();
}
