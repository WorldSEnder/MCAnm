package com.github.worldsender.mcanm.client.model.util;

import java.util.Arrays;

import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.animation.IAnimation.BoneTransformation;

public class Animations {
	/**
	 * A wrapper to chain animations together. If a position for bone &lt;bone&gt; is not given by the firstChoice, it
	 * subsequently asks the second choice and then the other choices.<br>
	 * The animations returned returns no {@link BoneTransformation} if and only if none of the animations would return
	 * a Transformation. Else the Transformation of the first animation that does return one is returned.
	 *
	 * @param choices
	 *            the choices, in the induced order. Usually two or more are given
	 * @return an animation that picks it's transforms from the animations given.
	 */
	public static IAnimation combined(final IAnimation... choices) {
		return combined(Arrays.asList(choices));
	}

	/**
	 * The same as {@link #combined(IAnimation...)}, but the animations are given as an {@link Iterable}.
	 *
	 * @param choices
	 *            the choices to choose from, in the induced order.
	 * @return an animation that picks it's transforms from the animations given
	 * @see #combined(IAnimation...)
	 */
	public static IAnimation combined(final Iterable<IAnimation> choices) {
		return new IAnimation() {
			@Override
			public boolean storeCurrentTransformation(String bone, float frame, BoneTransformation transform) {
				for (IAnimation other : choices) {
					if (other.storeCurrentTransformation(bone, frame, transform)) {
						return true;
					}
				}
				return false;
			}
		};
	}
}
