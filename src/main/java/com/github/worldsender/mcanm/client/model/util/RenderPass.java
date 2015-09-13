package com.github.worldsender.mcanm.client.model.util;

import java.util.Objects;

import net.minecraft.client.renderer.Tessellator;

import com.github.worldsender.mcanm.client.model.IRenderPassInformation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.IRenderPass;
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
	private Tessellator tesselator;

	public RenderPass(IRenderPassInformation info, Tessellator tesselator) {
		this.userInfo = Objects.requireNonNull(info);
		this.tesselator = Objects.requireNonNull(tesselator);
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
	public Tessellator getTesselator() {
		return tesselator;
	}

	public RenderPass setTesellator(Tessellator tesselator) {
		this.tesselator = Objects.requireNonNull(tesselator);
		return this;
	}

	public RenderPass setRenderPassInformation(IRenderPassInformation info) {
		this.userInfo = Objects.requireNonNull(info);
		return this;
	}
}
