package com.github.worldsender.mcanm.client.model.mhfcmodel.glcontext;

import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.IAnimatedObject;
import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mhfcmodel.data.ModelDataBasic;
import com.github.worldsender.mcanm.client.model.mhfcmodel.data.RawDataV1;
import com.google.common.base.Predicate;

public class GLHelperBasic extends GLHelper {
	private ModelDataBasic modelData;

	@Override
	public void loadFrom(RawDataV1 datav1) {
		modelData = new ModelDataBasic(datav1);
	}

	@Override
	public void render(IAnimatedObject object, float subFrame) {
		if (this.modelData == null) // Not loaded correctly
			return;
		object.preRenderCallback(subFrame);
		Predicate<String> filter = object.getPartPredicate(subFrame);
		IAnimation currAnim = object.getCurrentAnimation(subFrame);
		float frame = object.getCurrentFrame(subFrame);
		if (filter != null)
			this.modelData.renderFiltered(filter, currAnim, frame);
		else
			this.modelData.renderAll(currAnim, frame);
	}
}
