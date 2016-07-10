package com.github.worldsender.mcanm.client.model.util;

import java.util.Objects;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.model.IEntityRender;
import com.github.worldsender.mcanm.client.model.IRenderPassInformation;
import com.github.worldsender.mcanm.common.animation.IAnimation;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/**
 * A collection of information neccessary to render the model.
 *
 * @author WorldSEnder
 *
 */
public class RenderPass<T extends EntityLiving> implements IRenderPass {
	private IRenderPassInformation userInfo;
	private IEntityRender<T> render;

	public RenderPass(IRenderPassInformation info, IEntityRender<T> renderer) {
		this.userInfo = Objects.requireNonNull(info);
		this.render = Objects.requireNonNull(renderer);
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
	public ResourceLocation getActualResourceLocation(String in) {
		return userInfo.getActualResourceLocation(in);
	}

	@Override
	public void bindTexture(ResourceLocation resLoc) {
		this.render.bindTextureFrom(resLoc);
	}

	public RenderPass<T> setRenderPassInformation(IRenderPassInformation info) {
		this.userInfo = Objects.requireNonNull(info);
		return this;
	}
}
