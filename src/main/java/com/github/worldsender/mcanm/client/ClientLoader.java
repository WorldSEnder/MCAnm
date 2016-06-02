package com.github.worldsender.mcanm.client;

import java.util.Objects;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.mcanmmodel.ModelMCMD;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.util.ResourceLocation;

public class ClientLoader {
	/**
	 * Loads a .mcmd model from the given {@link ResourceLocation} and binds it to the skeleton
	 * 
	 * @param resLoc
	 * @param skeleton
	 * @return
	 */
	public static ModelMCMD loadModel(ResourceLocation resLoc, ISkeleton skeleton) {
		Objects.requireNonNull(skeleton);
		ModelMCMD model = new ModelMCMD(MCAnm.proxy.getSidedResource(resLoc), skeleton);
		return model;
	}
}
