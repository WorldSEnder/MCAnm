package com.github.worldsender.mcanm.common.skeleton;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.RawData;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitable;

@Deprecated
public class LegacyModelAsSkeleton extends AbstractSkeleton {

	private static ISkeletonVisitable load(IResource resource) {
		try {
			return RawData.retrieveFrom(resource).getLegacySkeletonData();
		} catch (ClassCastException cce) {
			throw new ModelFormatException("Expected a legacy model as skeleton", cce);
		}
	}

	public LegacyModelAsSkeleton(IResourceLocation resLoc) {
		super(resLoc, LegacyModelAsSkeleton::load);
	}

}
