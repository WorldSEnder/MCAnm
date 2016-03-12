package com.github.worldsender.mcanm.common.resource;

import java.io.IOException;
import java.util.Optional;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class MinecraftResource extends ResourceAdapter {
	private final ResourceLocation resLoc;

	public MinecraftResource(ResourceLocation resLoc, IResourceManager manager) throws IOException {
		super(manager.getResource(resLoc).getInputStream());
		this.resLoc = resLoc;
	}

	@Override
	public Optional<IResourceLocation> getOrigin() {
		return Optional.of(new MinecraftResourceLocation(resLoc));
	}

	@Override
	public String getResourceName() {
		return this.resLoc.toString();
	}
}
