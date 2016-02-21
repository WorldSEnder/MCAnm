package com.github.worldsender.mcanm.client.mcanmmodel.glcontext;

import java.util.Optional;

import com.github.worldsender.mcanm.client.mcanmmodel.data.IModelData;
import com.github.worldsender.mcanm.client.mcanmmodel.data.ModelDataBasic;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1;

public class GLHelperBasic extends GLHelper {
	@Override
	public Optional<IModelData> preBakeV1(RawDataV1 datav1) {
		return Optional.<IModelData>of(new ModelDataBasic(datav1));
	}

}
