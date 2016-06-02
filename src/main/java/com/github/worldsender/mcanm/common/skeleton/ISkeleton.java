package com.github.worldsender.mcanm.common.skeleton;

import com.github.worldsender.mcanm.common.animation.IAnimation;

import net.minecraft.client.renderer.Tessellator;

public interface ISkeleton {
	public static final ISkeleton EMPTY = new ISkeleton() {

		@Override
		public void setup(IAnimation animation, float frame) {}

		@Override
		public IBone getBoneByName(String bone) {
			return IBone.STATIC_BONE;
		}

		@Override
		public IBone getBoneByIndex(int index) {
			return IBone.STATIC_BONE;
		}

		@Override
		public void debugDraw(Tessellator tess) {}
	};

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
