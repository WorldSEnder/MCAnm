package com.github.worldsender.mcanm.server;

import com.github.worldsender.mcanm.Proxy;
import com.github.worldsender.mcanm.common.resource.EmbeddedResourceLocation;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.util.CallResolver;

import net.minecraft.util.ResourceLocation;

public class ServerProxy implements Proxy {
	@Override
	public void preInit() {}

	@Override
	public void init() {}

	@Override
	public IResourceLocation getSidedResource(ResourceLocation resLoc) {
		ClassLoader loader = CallResolver.INSTANCE.getCallingClass().getClassLoader();
		String path = resLoc.getResourceDomain() + "/" + resLoc.getResourcePath();
		return new EmbeddedResourceLocation(path, loader);
	}
}
