package com.github.worldsender.mcanm.client.mcanmmodel.gl;

import java.util.List;
import java.util.Map;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.model.IModelStateInformation;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;

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

	/**
	 * The totally inefficient method of minecraft to get block data
	 * 
	 * @param currentPass
	 * @param slotToTex
	 * @param format
	 * @return
	 */
	List<BakedQuad> getAsBakedQuads(
			IModelStateInformation currentPass,
			Map<String, TextureAtlasSprite> slotToTex,
			VertexFormat format);
}
