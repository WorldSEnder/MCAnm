package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;

import com.github.worldsender.mcanm.client.model.mcanmmodel.ModelMCMD.MCMDState;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.IModelData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;
import com.github.worldsender.mcanm.client.model.mcanmmodel.loader.VersionizedModelLoader;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;
import com.google.common.base.Optional;

/**
 * Represents an GLHelper. That is a glHelper for the correct OpenGL- version
 * present on this computer.
 *
 * @author WorldSEnder
 *
 */
public abstract class GLHelper {
	protected Optional<IModelData> currentData;

	protected RenderPassInformation userPassCache = new RenderPassInformation();
	protected RenderPass passCache = new RenderPass(userPassCache, null);
	/**
	 * This method is used to translate the {@link RawDataV1} that was
	 * previously read by the appropriate {@link VersionizedModelLoader}.
	 *
	 * @param amd
	 *            the data loaded by the {@link VersionizedModelLoader}
	 * @return data that can be understood by this {@link GLHelper}
	 */
	public final Optional<IModelData> preBake(MCMDState state,
			VertexFormat format) {
		RawData amd = state.getData().orNull();
		if (amd == null)
			return currentData = Optional.<IModelData> absent();
		if (amd instanceof RawDataV1)
			return currentData = this.preBakeV1((RawDataV1) amd, format);
		throw new IllegalArgumentException("Unrecognized data format");

	}
	/**
	 * Loads data of version 1. This method should return an absent Optional
	 * when the data couldn't be processed, never null. Should also log the fail
	 * somewhere.
	 *
	 * @param datav1
	 *            the data to be loaded into this handler
	 */
	public abstract Optional<IModelData> preBakeV1(RawDataV1 datav1,
			VertexFormat format);
	/**
	 * Actually renders the model that MAY have been previously loaded. If no
	 * data has been loaded yet, this method is expected to instantly return. It
	 * may log but it is not required and/or expected to do so.
	 *
	 * @param entity
	 *            the entity to render
	 * @param subFrame
	 *            the current subFrame, always 0.0 <= subFrame <= 1.0
	 **/
	public void render(IAnimatedObject object, float subFrame) {
		if (object == null || !this.currentData.isPresent()) // Model is absent
			return;
		IModelData data = this.currentData.get();

		userPassCache.reset();

		RenderPassInformation currentPass = object.preRenderCallback(subFrame,
				userPassCache);

		data.render(passCache.setRenderPassInformation(currentPass)
				.setRenderer(Tessellator.getInstance().getWorldRenderer()));
	}
	/**
	 * Selects an appropriate {@link GLHelper} from the known types.
	 */
	public static GLHelper getAppropriateHelper() {
		// TODO: enable advanced rendering, write when you feel like you have to
		// optimize
		return new GLHelperBasic();
	}
}
