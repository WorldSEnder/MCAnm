package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import java.util.List;
import java.util.Map;

import com.github.worldsender.mcanm.client.IRenderPass;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;

public interface IPart {

	void render(IRenderPass currentPass);

	/**
	 * The totally inefficient way minecraft wants us to render item models.
	 * 
	 * @param slotToTex
	 * @param format
	 * @param out
	 */
	void getAsBakedQuads(Map<String, TextureAtlasSprite> slotToTex, VertexFormat format, List<BakedQuad> out);

	String getName();
}
