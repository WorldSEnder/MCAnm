package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import com.github.worldsender.mcanm.client.IRenderPass;

/**
 * Interface for ModelData.
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
