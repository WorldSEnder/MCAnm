package com.github.worldsender.mcanm.common;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.common.animation.StoredAnimation;
import com.github.worldsender.mcanm.common.skeleton.LegacyModelAsSkeleton;
import com.github.worldsender.mcanm.common.skeleton.SkeletonMCSKL;

import net.minecraft.util.ResourceLocation;

public class CommonLoader {
	public static SkeletonMCSKL loadSkeleton(ResourceLocation resLoc) {
		return new SkeletonMCSKL(MCAnm.proxy.getSidedResource(resLoc));
	}

	@Deprecated
	public static LegacyModelAsSkeleton loadLegacySkeleton(ResourceLocation resLoc) {
		return new LegacyModelAsSkeleton(MCAnm.proxy.getSidedResource(resLoc));
	}

	public static StoredAnimation loadAnimation(ResourceLocation resLoc) {
		return new StoredAnimation(MCAnm.proxy.getSidedResource(resLoc));
	}
}
