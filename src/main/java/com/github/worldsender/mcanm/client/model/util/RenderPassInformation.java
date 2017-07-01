package com.github.worldsender.mcanm.client.model.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.github.worldsender.mcanm.client.model.IRenderPassInformation;
import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
public class RenderPassInformation extends ModelStateInformation implements IRenderPassInformation {
	public static final Function<String, ResourceLocation> makeCachingTransform(
			Function<String, ResourceLocation> cacheLoader) {
		LoadingCache<String, ResourceLocation> cachedResourceLoc = CacheBuilder.newBuilder().maximumSize(100)
				.build(new CacheLoader<String, ResourceLocation>() {
					@Override
					public ResourceLocation load(String key) {
						return cacheLoader.apply(key);
					}
				});
		return cachedResourceLoc::getUnchecked;
	}

	public static final Function<String, ResourceLocation> TRANSFORM_DIRECT = makeCachingTransform(
			ResourceLocation::new);
	/**
	 * This is the default texture rebinding, it just returns the given resource-location and as such does not change
	 * the bound texture.
	 */
	public static final Function<ResourceLocation, ResourceLocation> IDENTITY = Function.identity();

	private Function<String, ResourceLocation> textureRemap;

	public RenderPassInformation() {
		this.reset();
	}

	public RenderPassInformation(
			float frame,
			Optional<IAnimation> animation,
			Optional<Predicate<String>> partPredicate,
			Optional<Function<String, ResourceLocation>> resourceRemap) {
		this.setFrame(frame).setAnimation(animation).setPartPredicate(partPredicate).setTextureTransform(resourceRemap);
	}

	/**
	 * Resets this information to be reused.
	 */
	@Override
	public void reset() {
		super.reset();
		this.setTextureTransform(Optional.empty());
	}

	@Override
	public ResourceLocation getActualResourceLocation(String in) {
		return this.textureRemap.apply(in);
	}

	@Override
	public RenderPassInformation setAnimation(Optional<IAnimation> animation) {
		super.setAnimation(animation);
		return this;
	}

	@Override
	public RenderPassInformation setAnimation(IAnimation animation) {
		super.setAnimation(animation);
		return this;
	}

	/**
	 * @param frame
	 *            the frame to set
	 */
	@Override
	public RenderPassInformation setFrame(float frame) {
		super.setFrame(frame);
		return this;
	}

	/**
	 * @param partPredicate
	 *            the partPredicate to set, Optional.empty() for RENDER_ALL
	 */
	@Override
	public RenderPassInformation setPartPredicate(Optional<Predicate<String>> partPredicate) {
		super.setPartPredicate(partPredicate);
		return this;
	}

	@Override
	public RenderPassInformation setPartPredicate(Predicate<String> partPredicate) {
		super.setPartPredicate(partPredicate);
		return this;
	}

	public RenderPassInformation setTextureTransform(Optional<Function<String, ResourceLocation>> textureRemap) {
		return setTextureTransform(textureRemap.orElse(TRANSFORM_DIRECT));
	}

	public RenderPassInformation setTextureTransform(Function<String, ResourceLocation> textureRemap) {
		this.textureRemap = Objects.requireNonNull(textureRemap);
		return this;
	}
}
