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

		IAnimation animation = currentPass == null
				|| currentPass.getAnimation() == null
				? RenderPassInformation.BIND_POSE
				: currentPass.getAnimation();
		Predicate<String> filter = currentPass == null
				|| currentPass.getPartPredicate() == null
				? RenderPassInformation.RENDER_ALL
				: currentPass.getPartPredicate();
		float frame = currentPass == null ? 0f : currentPass.getFrame();

		this.modelData.renderFiltered(filter, animation, frame);
	}
}
