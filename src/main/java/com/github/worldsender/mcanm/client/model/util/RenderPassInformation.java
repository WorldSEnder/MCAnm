package com.github.worldsender.mcanm.client.model.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.worldsender.mcanm.client.model.IRenderPassInformation;
import com.github.worldsender.mcanm.common.animation.IAnimation;

import net.minecraft.util.ResourceLocation;

/**
 * Used to simplify the pre-render callback process. This class is an aggregate of all needed information for one render
 * pass (aka. rendering one object). You may use the setter methods or simply extend the class and override the getter
 * methods to return what you need.<br>
 * You should not call new RenderPassInformation(...) every time one is needed but reuse instances you used before. The
 * internal API doesn't keep custom instances around after the render-pass is over, only its own.
 *
 * @author WorldSEnder
 *
 */
public class RenderPassInformation implements IRenderPassInformation {
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
		public Optional<BoneTransformation> getCurrentTransformation(String bone, float frame) {
			return Optional.empty();
		};
	};
	/**
	 * This is the default texture rebinding, it just returns the given resource-location and as such does not change
	 * the bound texture.
	 */
	public static final Function<ResourceLocation, ResourceLocation> IDENTITY = Function.identity();

	private float frame;
	private IAnimation animation;
	private Predicate<String> partPredicate;
	private Function<ResourceLocation, ResourceLocation> textureRemap;

	public RenderPassInformation() {
		this.reset();
	}

	public RenderPassInformation(
			float frame,
			Optional<IAnimation> animation,
			Optional<Predicate<String>> partPredicate,
			Optional<Function<ResourceLocation, ResourceLocation>> resourceRemap) {
		this.setFrame(frame).setAnimation(animation).setPartPredicate(partPredicate).setTextureTransform(resourceRemap);
	}

	/**
	 * Resets this information to be reused.
	 */
	public void reset() {
		this.setFrame(0F).setAnimation(Optional.of(BIND_POSE)).setPartPredicate(Optional.of(RENDER_ALL))
				.setTextureTransform(Optional.of(IDENTITY));
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

	@Override
	public boolean shouldRenderPart(String part) {
		return this.partPredicate.test(part);
	}

	@Override
	public ResourceLocation getActualResourceLocation(ResourceLocation in) {
		return this.textureRemap.apply(in);
	}

	/**
	 * @param animation
	 *            the animation to set, never null
	 */
	public RenderPassInformation setAnimation(Optional<IAnimation> animation) {
		this.animation = animation.orElse(BIND_POSE);
		return this;
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
	 * @param partPredicate
	 *            the partPredicate to set, never null
	 */
	public RenderPassInformation setPartPredicate(Optional<Predicate<String>> partPredicate) {
		this.partPredicate = partPredicate.orElse(RENDER_ALL);
		return this;
	}

	public RenderPassInformation setTextureTransform(
			Optional<Function<ResourceLocation, ResourceLocation>> textureRemap) {
		this.textureRemap = textureRemap.orElse(IDENTITY);
		return this;
	}
}
