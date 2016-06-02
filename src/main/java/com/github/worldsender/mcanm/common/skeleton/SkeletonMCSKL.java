package com.github.worldsender.mcanm.common.skeleton;

import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.skeleton.stored.RawData;

public class SkeletonMCSKL extends AbstractSkeleton {
	public SkeletonMCSKL(IResourceLocation resLoc) {
		super(resLoc, RawData::retrieveFrom);
	}
}
