package com.github.worldsender.mcanm.client.model.util;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation.BoneTransformation;
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
	public static IAnimation gathered(final IAnimation... choices) {
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
	 * The same as {@link #gathered(IAnimation...)}, but the animations are given
	 * as an {@link Iterable}.
	 *
	 * @param choices
	 *            the choices to choose from, in the induced order.
	 * @return an animation that picks it's transforms from the animations given
	 * @see #gathered(IAnimation...)
	 */
	public static IAnimation gathered(final Iterable<IAnimation> choices) {
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
}
