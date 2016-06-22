package com.github.worldsender.mcanm.client.mcanmmodel.gl;

import com.github.worldsender.mcanm.client.mcanmmodel.parts.PartDirect;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitable;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

public class ModelRenderDataGLArray extends ModelRenderAbstract<PartDirect> {

	public ModelRenderDataGLArray(IModelVisitable data, ISkeleton skeleton) {
		super(data, skeleton, PartDirect::new);
	}

}
