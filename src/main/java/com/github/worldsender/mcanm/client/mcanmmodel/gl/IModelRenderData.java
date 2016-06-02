package com.github.worldsender.mcanm.client.mcanmmodel.gl;

import com.github.worldsender.mcanm.client.IRenderPass;

/**
 * Interface for ModelData.
 *
 * @author WorldSEnder
 *
 */
public interface IModelRenderData {
	/**
	 * Renders the model from the given RenderPass.
	 *
	 * @param pass
	 */
	public void render(IRenderPass pass);
}
