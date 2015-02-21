package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import net.minecraft.client.renderer.vertex.VertexFormat;

import com.github.worldsender.mcanm.client.model.mcanmmodel.data.IModelData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.ModelDataBasic;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;

public class GLHelperBasic extends GLHelper {
	
	public IModelData preBakeV1(RawDataV1 datav1, VertexFormat format) {
		return new ModelDataBasic(datav1);
	}
	
}
