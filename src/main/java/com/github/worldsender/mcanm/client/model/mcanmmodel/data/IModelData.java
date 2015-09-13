package com.github.worldsender.mcanm.client.model.mcanmmodel.data;

import com.github.worldsender.mcanm.client.model.mcanmmodel.IRenderPass;
import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.GLHelper;

/**
 * Interface for ModelData used by {@link GLHelper} classes.
 *
 * @author WorldSEnder
 *
 */
public interface IModelData {
	/**
	 * Renders the model from the given RenderPass.
	 *
	 * @param pass
	 */
	public void render(IRenderPass pass);
}
