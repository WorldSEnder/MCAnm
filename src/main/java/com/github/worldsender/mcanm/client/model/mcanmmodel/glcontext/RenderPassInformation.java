package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
/**
 * Used to simplify the pre-render callback process. This class is an aggregate
 * of all needed information for one render pass (aka. rendering one object).
 * You may use the setter methods or simply extend the class and override the
 * getter methods to return what you need.<br>
 * You should not call new RenderPassInformation(...) every time one is needed
 * but reuse instances you used before. The internal API doesn't keep custom
 * instances around after the render-pass is over, only its own.
 *
 * @author WorldSEnder
 *
 */
public class RenderPassInformation implements IRenderPassInformation {
	/**
	 * A suitable {@link Predicate} to return in
	 * {@link #getPartPredicate(float)} to render all parts without exception.
	 * Note that this will not test if the currently executed animation wants to
	 * display the part.
	 */
	public static final Predicate<String> RENDER_ALL = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return true;
		}
	};
	/**
	 * A suitable {@link Predicate} to return in
	 * {@link #getPartPredicate(float)} to render no parts without exception.
	 * Note that this will not test if the currently executed animation wants to
	 * display the part.
	 */
	public static final Predicate<String> RENDER_NONE = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return false;
		}
	};
	/**
	 * This is a default animation that always returns the binding pose for each
	 * bone it is asked for. Actually it returns <code>null</code> which is to
	 * be interpreted as the binding-pose.
	 */
	public static final IAnimation BIND_POSE = new IAnimation() {
		@Override
		public BoneTransformation getCurrentTransformation(String bone,
				float frame) {
			return BoneTransformation.identity;
		};
	};

	private float frame;
	private IAnimation animation;
	private Predicate<String> partPredicate;

	public RenderPassInformation() {
		this.reset();
	}

	public RenderPassInformation(float frame, Optional<IAnimation> animation,
			Optional<Predicate<String>> partPredicate) {
		this.setFrame(frame).setAnimation(animation)
				.setPartPredicate(partPredicate);
	}
	/**
	 * Resets this information to be reused.
	 */
	public void reset() {
		this.setFrame(0F).setAnimation(Optional.of(BIND_POSE))
				.setPartPredicate(Optional.of(RENDER_ALL));
	}
	/**
	 * Returns the current Frame in the animation. This is not inside the
	 * {@link IAnimation} so that each object/entity can decide on its own.
	 * Should include the current subframe for smoother transitions.
	 *
	 * @return the current frame
	 */
	@Override
	public float getFrame() {
		return frame;
	}
	/**
	 * @param frame
	 *            the frame to set
	 */
	public RenderPassInformation setFrame(float frame) {
		this.frame = frame;
		return this;
	}
	/**
	 * Gets the animation to play. If you return null here the model will be
	 * displayed in bind pose.
	 *
	 * @return the current animation
	 */
	@Override
	public IAnimation getAnimation() {
		return animation;
	}
	/**
	 * @param animation
	 *            the animation to set, never null
	 */
	public RenderPassInformation setAnimation(Optional<IAnimation> animation) {
		this.animation = animation.or(BIND_POSE);
		return this;
	}
	/**
	 * Returns for the current animation a predicate that get applied all
	 * model-parts to test if they should be rendered.
	 *
	 * @return a predicate to match parts against that may be rendered
	 */
	@Override
	public Predicate<String> getPartPredicate() {
		return partPredicate;
	}
	/**
	 * @param partPredicate
	 *            the partPredicate to set, never null
	 */
	public RenderPassInformation setPartPredicate(
			Optional<Predicate<String>> partPredicate) {
		this.partPredicate = partPredicate.or(RENDER_ALL);
		return this;
	}

}
