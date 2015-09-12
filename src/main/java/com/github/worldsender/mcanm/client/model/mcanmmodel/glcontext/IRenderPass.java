package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
/**
 * An extension of only the user supplied information. This adds engine-based
 * properties such as the used {@link RenderManager}.
 *
 * @author WorldSEnder
 *
 */
public interface IRenderPass extends IRenderPassInformation {
	WorldRenderer getRenderer();
}
