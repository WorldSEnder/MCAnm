package com.github.worldsender.mcanm.common.resource;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * Used to collect all {@link MinecraftResourceLocation}s and trigger an update when the {@link IResourceManager}
 * changes.
 * 
 * @author WorldSEnder
 *
 */
public class MinecraftResourcePool {

	public static final MinecraftResourcePool instance = new MinecraftResourcePool();

	private List<WeakReference<MinecraftResourceLocation>> allIssuedLocations = new LinkedList<>();

	public void onResourceManagerReloaded() {
		for (Iterator<WeakReference<MinecraftResourceLocation>> it = allIssuedLocations.iterator(); it.hasNext();) {
			MinecraftResourceLocation resLoc = it.next().get();
			if (resLoc == null) { // Already garbage collected
				it.remove();
			}
			resLoc.triggerReload();
		}
	}

	public IResourceManager getCurrentResourceManager() {
		return Minecraft.getMinecraft().getResourceManager();
	}

	public MinecraftResourceLocation makeResourceLocation(ResourceLocation resLoc) {
		MinecraftResourceLocation location = new MinecraftResourceLocation(this, resLoc);
		allIssuedLocations.add(new WeakReference<MinecraftResourceLocation>(location));
		return location;
	}
}
