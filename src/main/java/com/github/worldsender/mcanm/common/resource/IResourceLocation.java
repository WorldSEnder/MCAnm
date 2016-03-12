package com.github.worldsender.mcanm.common.resource;

import java.io.IOException;

import net.minecraft.client.resources.IResourceManager;

public interface IResourceLocation {
	boolean changesWithResourceManager();

	IResource openWith(IResourceManager manager) throws IOException;

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();
}
