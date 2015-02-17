package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;
import com.github.worldsender.mcanm.client.model.mcanmmodel.loader.VersionizedModelLoader;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;

/**
 * Represents an GLHelper. That is a render-glHelper for the correct OpenGL-
 * version present on this computer.
 *
 * @author WorldSEnder
 *
 * @param <T>
 *            the ModelDataType this GLHelper can handle
 */
public abstract class GLHelper {
	/**
	 * This method is used to translate the {@link RawDataV1} that was
	 * previously read by the appropriate {@link VersionizedModelLoader}.
	 *
	 * @param amd
	 *            the data loaded by the {@link VersionizedModelLoader}
	 * @return data that can be understood by this {@link GLHelper}
	 */
	public final void loadInto(RawData amd) {
		if (amd instanceof RawDataV1)
			this.loadFrom((RawDataV1) amd);
		else
			throw new IllegalArgumentException("Unrecognized data format");
	}
	/**
	 * Loads data of version 1.
	 *
	 * @param datav1
	 *            the data to be loaded into this handler
	 */
	public abstract void loadFrom(RawDataV1 datav1);
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
	public abstract void render(IAnimatedObject entity, float subFrame);
	/**
	 * Selects an appropriate {@link GLHelper} from the known types.
	 */
	public static GLHelper getNewAppropriateHelper() {
		// TODO: enable advanced rendering, write when you feel like you have to
		// optimize
		return new GLHelperBasic();
	}
}
