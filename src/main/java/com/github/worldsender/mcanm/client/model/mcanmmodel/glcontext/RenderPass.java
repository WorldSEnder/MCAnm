package com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext;

import net.minecraft.client.renderer.WorldRenderer;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.google.common.base.Predicate;

/**
 * A collection of information neccessary to render the model.
 *
 * @author WorldSEnder
 *
 */
public class RenderPass implements IRenderPass {
	private IRenderPassInformation userInfo;
	private WorldRenderer renderer;

	public RenderPass(IRenderPassInformation info, WorldRenderer renderer) {
		this.userInfo = info;
		this.renderer = renderer;
	}

	@Override
	public IAnimation getAnimation() {
		return userInfo.getAnimation();
	}

	@Override
	public float getFrame() {
		return userInfo.getFrame();
	}

	@Override
	public Predicate<String> getPartPredicate() {
		return userInfo.getPartPredicate();
	}

	@Override
	public WorldRenderer getRenderer() {
		return renderer;
	}

	public RenderPass setRenderer(WorldRenderer renderer) {
		this.renderer = renderer;
		return this;
	}

	public RenderPass setRenderPassInformation(IRenderPassInformation info) {
		this.userInfo = info;
		return this;
	}
}
