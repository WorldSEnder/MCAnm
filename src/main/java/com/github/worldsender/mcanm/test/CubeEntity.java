package com.github.worldsender.mcanm.test;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.world.World;

import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RenderPassInformation;
import com.github.worldsender.mcanm.client.model.util.AnimationLoader;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;

public class CubeEntity extends EntityPig implements IAnimatedObject {
	public static final IAnimation animation = AnimationLoader
			.loadAnimation("mcanm:models/Cube/idle.mcanm");

	public CubeEntity(World w) {
		super(w);
	}

	@Override
	public RenderPassInformation preRenderCallback(float subFrame,
			RenderPassInformation callback) {
		return callback.setAnimation(animation).setFrame(
				this.ticksExisted % 90 + subFrame);
	}
}
