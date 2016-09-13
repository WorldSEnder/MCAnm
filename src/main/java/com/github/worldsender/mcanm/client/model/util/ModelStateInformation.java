package com.github.worldsender.mcanm.client.model.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.github.worldsender.mcanm.client.model.IModelStateInformation;
import com.github.worldsender.mcanm.common.animation.IAnimation;

public class ModelStateInformation implements IModelStateInformation {
	/**
	 * A suitable {@link Predicate} to return in {@link #getPartPredicate(float)} to render all parts without exception.
	 * Note that this will not test if the currently executed animation wants to display the part.
	 */
	public static final Predicate<String> RENDER_ALL = g -> true;
	/**
	 * A suitable {@link Predicate} to return in {@link #getPartPredicate(float)} to render no parts without exception.
	 * Note that this will not test if the currently executed animation wants to display the part.
	 */
	public static final Predicate<String> RENDER_NONE = g -> false;

	/**
	 * This is a default animation that never returns a pose for any bone it is asked for.
	 */
	public static final IAnimation BIND_POSE = new IAnimation() {
		@Override
		public boolean storeCurrentTransformation(String bone, float frame, BoneTransformation transform) {
			return false;
		};
	};

	private Predicate<String> partPredicate;
	private float frame;
	private IAnimation animation;

	public ModelStateInformation() {
		this.reset();
	}

	public void reset() {
		this.setFrame(0F).setAnimation(Optional.empty());
	}

	@Override
	public boolean shouldRenderPart(String part) {
		return this.partPredicate.test(part);
	}

	/**
	 * Returns the current Frame in the animation. This is not inside the {@link IAnimation} so that each object/entity
	 * can decide on its own. Should include the current subframe for smoother transitions.
	 *
	 * @return the current frame
	 */
	@Override
	public float getFrame() {
		return frame;
	}

	/**
	 * Gets the animation to play. If you return null here the model will be displayed in bind pose.
	 *
	 * @return the current animation
	 */
	@Override
	public IAnimation getAnimation() {
		return animation;
	}

	/**
	 * @param animation
	 *            the animation to set, Optional.empty() for bind pose
	 */
	public ModelStateInformation setAnimation(Optional<IAnimation> animation) {
		return setAnimation(animation.orElse(BIND_POSE));
	}

	public ModelStateInformation setAnimation(IAnimation animation) {
		this.animation = Objects.requireNonNull(animation);
		return this;
	}

	/**
	 * @param frame
	 *            the frame to set
	 */
	public ModelStateInformation setFrame(float frame) {
		this.frame = frame;
		return this;
	}

	/**
	 * @param partPredicate
	 *            the partPredicate to set, Optional.empty() for RENDER_ALL
	 */
	public ModelStateInformation setPartPredicate(Optional<Predicate<String>> partPredicate) {
		return setPartPredicate(partPredicate.orElse(RENDER_ALL));
	}

	public ModelStateInformation setPartPredicate(Predicate<String> partPredicate) {
		this.partPredicate = Objects.requireNonNull(partPredicate);
		return this;
	}

}
