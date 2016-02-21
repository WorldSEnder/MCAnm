package com.github.worldsender.mcanm.client.model.util;

import java.util.Objects;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.client.mcanmmodel.IRenderPass;
import com.github.worldsender.mcanm.client.model.IEntityRender;
import com.github.worldsender.mcanm.client.model.IRenderPassInformation;
import com.github.worldsender.mcanm.common.animation.IAnimation;

/**
 * A collection of information neccessary to render the model.
 *
 * @author WorldSEnder
 *
 */
public class RenderPass implements IRenderPass {
	private IRenderPassInformation userInfo;
	private Tessellator tesselator;
	private IEntityRender render;

	public RenderPass(IRenderPassInformation info, Tessellator tesselator, IEntityRender renderer) {
		this.userInfo = Objects.requireNonNull(info);
		this.tesselator = tesselator;
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
	public boolean shouldRenderPart(String part) {
		return userInfo.shouldRenderPart(part);
	}

	@Override
	public ResourceLocation getActualResourceLocation(ResourceLocation in) {
		return userInfo.getActualResourceLocation(in);
	}

	@Override
	public void bindTexture(ResourceLocation resLoc) {
		this.render.bindTexture(resLoc);
	}

	@Override
	public Tessellator getTesselator() {
		return Objects.requireNonNull(tesselator);
	}

	public RenderPass setTesellator(Tessellator tesselator) {
		this.tesselator = Objects.requireNonNull(tesselator);
		return this;
	}

	public RenderPass setRenderPassInformation(IRenderPassInformation info) {
		this.userInfo = Objects.requireNonNull(info);
		return this;
	}

	public RenderPass setRender(IEntityRender render) {
		this.render = Objects.requireNonNull(render);
		return this;
	}
}
