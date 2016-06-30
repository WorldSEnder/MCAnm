package com.github.worldsender.mcanm.client.model.util;

import java.util.Arrays;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.animation.IAnimation.BoneTransformation;
import com.github.worldsender.mcanm.common.animation.parts.BSplineInterpolation;

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

	/**
	 * t &#8712; (0, 1]d
	 */
	private static Vector3f interpolated(Vector3f f, Vector3f s, double t) {
		float x = BSplineInterpolation.calcValue(f.x, f.x, s.x, s.x, t);
		float y = BSplineInterpolation.calcValue(f.y, f.y, s.y, s.y, t);
		float z = BSplineInterpolation.calcValue(f.z, f.z, s.z, s.z, t);
		return new Vector3f(x, y, z);
	}

	/**
	 * t &#8712; (0, 1]
	 */
	private static Quat4f interpolated(Quat4f f, Quat4f s, double t) {
		float x = BSplineInterpolation.calcValue(f.x, f.x, s.x, s.x, t);
		float y = BSplineInterpolation.calcValue(f.y, f.y, s.y, s.y, t);
		float z = BSplineInterpolation.calcValue(f.z, f.z, s.z, s.z, t);
		float w = BSplineInterpolation.calcValue(f.w, f.w, s.w, s.w, t);
		return new Quat4f(x, y, z, w);
	}
}
