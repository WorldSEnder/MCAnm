package com.github.worldsender.mcanm.client.mcanmmodel.gl;

import com.github.worldsender.mcanm.client.mcanmmodel.parts.PartTesselator;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitable;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

public class ModelRenderTesselator extends ModelRenderAbstract<PartTesselator> {

	public ModelRenderTesselator(IModelVisitable data, ISkeleton skeleton) {
		super(data, skeleton, PartTesselator::new);
	}

}
