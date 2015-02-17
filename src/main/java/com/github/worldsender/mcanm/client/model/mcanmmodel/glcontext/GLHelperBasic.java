package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.ModelDataBasic;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RenderPassInformation;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;
import com.google.common.base.Predicate;

public class GLHelperBasic extends GLHelper {
	private ModelDataBasic modelData;
	private RenderPassInformation passInformation = new RenderPassInformation();;

	@Override
	public void loadFrom(RawDataV1 datav1) {
		modelData = new ModelDataBasic(datav1);
	}

	@Override
	public void render(IAnimatedObject object, float subFrame) {
		if (this.modelData == null) // Not loaded correctly
			return;
		if (object == null) // Don't render
			return;
		passInformation.reset();
		RenderPassInformation currentPass = object.preRenderCallback(subFrame,
				passInformation);
		if (currentPass == null)
			this.modelData.renderAll(RenderPassInformation.BIND_POSE, 0.0F);
		else {
			Predicate<String> filter = currentPass.getPartPredicate();
			IAnimation animation = currentPass.getAnimation();
			float frame = currentPass.getFrame();
			if (filter != null)
				this.modelData.renderFiltered(filter, animation, frame);
			else
				this.modelData.renderAll(animation, frame);
		}
	}
}
