package com.github.worldsender.mcanm.client.model.util;

import java.util.Arrays;
import java.util.Objects;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.tuple.Triple;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation.BoneTransformation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.stored.BSplineInterpolation;
import com.google.common.base.Optional;

public class Animations {
	/**
	 * A wrapper to chain animations together. If a position for bone
	 * &lt;bone&gt; is not given by the firstChoice, it subsequently asks the
	 * second choice and then the other choices.<br>
	 * The animations returned returns no {@link BoneTransformation} if and only
	 * if none of the animations would return a Transformation. Else the
	 * Transformation of the first animation that does return one is returned.
	 *
	 * @param choices
	 *            the choices, in the induced order. Usually two or more are
	 *            given
	 * @return an animation that picks it's transforms from the animations
	 *         given.
	 */
	public static IAnimation combined(final IAnimation... choices) {
		return combined(Arrays.asList(choices));
	}
	/**
	 * The same as {@link #combined(IAnimation...)}, but the animations are
	 * given as an {@link Iterable}.
	 *
	 * @param choices
	 *            the choices to choose from, in the induced order.
	 * @return an animation that picks it's transforms from the animations given
	 * @see #combined(IAnimation...)
	 */
	public static IAnimation combined(final Iterable<IAnimation> choices) {
		return new IAnimation() {
			@Override
			public Optional<BoneTransformation> getCurrentTransformation(
					String bone, float frame) {
				Optional<BoneTransformation> value = Optional.absent();
				for (IAnimation other : choices) {
					value = other.getCurrentTransformation(bone, frame);
					if (!value.isPresent())
						continue;
				}
				return value;
			}
		};
	}
	/**
	 * Concenates two animations together with a smooth transition between them.
	 * The resulting animation has
	 * <ul>
	 * <li>the value of the first animation if <code>frame < endFirst </code>
	 * <li>the value of the second animation if
	 * <code>frame > endFirst + transitionLength</code>
	 * <li>interpolates smoothly if
	 * <code> frame &#x2208; [endFrist, endFirst + transitionLength]</code>
	 *
	 * </ul>
	 *
	 * @param first
	 *            the first animation
	 * @param endFirst
	 *            the end frame of the first animation
	 * @param transitionLength
	 *            the transition length
	 * @param second
	 *            the second animation
	 * @return an animation that concatenates the two animation smoothly
	 */
	public static IAnimation concat(IAnimation first, final float endFirst,
			final float transitionLength, IAnimation second) {
		final IAnimation f = Objects.requireNonNull(first);
		final IAnimation s = Objects.requireNonNull(second);

		return new IAnimation() {
			@Override
			public Optional<BoneTransformation> getCurrentTransformation(
					String bone, float frame) {
				if (frame < endFirst) {
					return f.getCurrentTransformation(bone, frame);
				} else if (frame > endFirst + transitionLength) {
					return s.getCurrentTransformation(bone, frame - endFirst
							- transitionLength);
				}
				Optional<BoneTransformation> currentF = f
						.getCurrentTransformation(bone, frame);
				Optional<BoneTransformation> currentS = s
						.getCurrentTransformation(bone, frame - endFirst
								- transitionLength);
				if (!currentF.isPresent()) {
					return currentS;
				} else if (!currentS.isPresent()) {
					return currentF; // Always present at this point
				}
				Triple<Vector3f, Quat4f, Vector3f> trsF = currentF.get()
						.asTRS();
				Triple<Vector3f, Quat4f, Vector3f> trsS = currentS.get()
						.asTRS();
				float tr = (frame - endFirst) / transitionLength; // \ele (0, 1]
				// TODO: t is mostly constant, might also pre-evaluate that
				double t = BSplineInterpolation.findZero(0, 1, 0, 1, tr);
				return Optional.of(new BoneTransformation(interpolated(
						trsF.getLeft(), trsS.getLeft(), t), interpolated(
						trsF.getMiddle(), trsS.getMiddle(), t), interpolated(
						trsF.getRight(), trsS.getRight(), t)));
			}
		};
	}

	/**
	 * tr \ele (0, 1]
	 */
	private static Vector3f interpolated(Vector3f f, Vector3f s, double t) {
		float x = BSplineInterpolation.calcValue(f.x, f.x, s.x, s.x, t);
		float y = BSplineInterpolation.calcValue(f.y, f.y, s.y, s.y, t);
		float z = BSplineInterpolation.calcValue(f.z, f.z, s.z, s.z, t);
		return new Vector3f(x, y, z);
	}

	/**
	 * tr \ele (0, 1]
	 */
	private static Quat4f interpolated(Quat4f f, Quat4f s, double t) {
		float x = BSplineInterpolation.calcValue(f.x, f.x, s.x, s.x, t);
		float y = BSplineInterpolation.calcValue(f.y, f.y, s.y, s.y, t);
		float z = BSplineInterpolation.calcValue(f.z, f.z, s.z, s.z, t);
		float w = BSplineInterpolation.calcValue(f.w, f.w, s.w, s.w, t);
		return new Quat4f(x, y, z, w);
	}
}
