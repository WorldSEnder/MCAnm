package com.github.worldsender.mcanm.client.mcanmmodel.gl;

import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitable;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

public class ModelRenderDataArray implements IModelRenderData {

	private final ISkeleton skeleton;

	public ModelRenderDataArray(IModelVisitable data, ISkeleton skeleton) {
		this.skeleton = skeleton;
		ModelVisitor visitor = new ModelVisitor();
		data.visitBy(visitor);
	}

}
