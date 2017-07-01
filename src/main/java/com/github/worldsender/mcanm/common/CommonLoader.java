package com.github.worldsender.mcanm.common;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.ClientLoader;
import com.github.worldsender.mcanm.common.animation.StoredAnimation;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.common.skeleton.LegacyModelAsSkeleton;
import com.github.worldsender.mcanm.common.skeleton.SkeletonMCSKL;
import com.github.worldsender.mcanm.common.util.CallResolver;

import net.minecraft.util.ResourceLocation;

@SuppressWarnings("deprecation")
public class CommonLoader {
	/**
	 * Loads a .mcskl skeleton from the given {@link ResourceLocation}. Note that while the raw data from the
	 * ResourceLocation is cached, the skeleton is not (because its state is not immutable), so try to not load one per
	 * frame but save it somewhere.
	 * 
	 * @param resLoc
	 *            the resource location to load from
	 * @return
	 * @see ClientLoader#loadModel(ResourceLocation, ISkeleton)
	 * @see #loadLegacySkeleton(ResourceLocation)
	 * @see #loadAnimation(ResourceLocation)
	 */
	public static SkeletonMCSKL loadSkeleton(ResourceLocation resLoc) {
		ClassLoader context = CallResolver.INSTANCE.getCallingClass().getClassLoader();
		return new SkeletonMCSKL(MCAnm.proxy.getSidedResource(resLoc, context));
	}

	/**
	 * Loads a version one .mcmd file from the given {@link ResourceLocation} but interprets it as a skeleton. Back at
	 * version 1 skeletons were stored inside model files. This was abandoned so that not the whole model has to be
	 * present at the server.<br>
	 * Note that while the raw data from the ResourceLocation is cached, the skeleton is not (because its state is not
	 * immutable), so try to not load one per frame but save it somewhere.
	 * 
	 * @param resLoc
	 *            the resource location to load from
	 * @return
	 * @see ClientLoader#loadModel(ResourceLocation, ISkeleton)
	 * @see #loadAnimation(ResourceLocation)
	 * @see #loadAnimation(ResourceLocation)
	 */
	@Deprecated
	public static LegacyModelAsSkeleton loadLegacySkeleton(ResourceLocation resLoc) {
		ClassLoader context = CallResolver.INSTANCE.getCallingClass().getClassLoader();
		return new LegacyModelAsSkeleton(MCAnm.proxy.getSidedResource(resLoc, context));
	}

	/**
	 * Loads a .mcanm animation from the given {@link ResourceLocation}. Note that this method <b>may</b> indeed cache
	 * its results. I still consider it bad style to not cache whatever needed yourself.
	 * 
	 * @param resLoc
	 *            the resource location to load from
	 * @return
	 * @see ClientLoader#loadModel(ResourceLocation, ISkeleton)
	 * @see #loadSkeleton(ResourceLocation)
	 */
	public static StoredAnimation loadAnimation(ResourceLocation resLoc) {
		ClassLoader context = CallResolver.INSTANCE.getCallingClass().getClassLoader();
		return new StoredAnimation(MCAnm.proxy.getSidedResource(resLoc, context));
	}
}
