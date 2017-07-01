package com.github.worldsender.mcanm;

import com.github.worldsender.mcanm.common.resource.EmbeddedResourceLocation;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.resource.MinecraftResourceLocation;

import net.minecraft.util.ResourceLocation;

public interface Proxy {
	void preInit();

	void init();

	/**
	 * On a client, this will return a {@link MinecraftResourceLocation} that will get reloaded when a new texture pack
	 * is switched on. On the server, this returns a {@link EmbeddedResourceLocation} using the {@link ClassLoader} of
	 * the calling class.
	 * 
	 * @param resLoc
	 *            the resource location to load from. On the server the resource must be packed with the .jar
	 * @param context
	 *            when the resource is loaded on the server, it is attempted to load it from this class loader
	 * @return
	 */
	IResourceLocation getSidedResource(ResourceLocation resLoc, ClassLoader context);
}
