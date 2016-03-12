package com.github.worldsender.mcanm.common.skeleton.stored;

import java.io.IOException;

import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;

public class RawData implements ISkeletonVisitable {

	private RawData() {}

	@Override
	public void visitBy(ISkeletonVisitor visitor) {
		// TODO Auto-generated method stub

	}

	public static RawData loadFrom(IResource resource) throws IOException, ModelFormatException {
		RawData data = new RawData();
		// FIXME: actually load the data from stream....
		return data;
	}

}
