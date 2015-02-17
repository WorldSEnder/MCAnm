package com.github.worldsender.mcanm.client.model.mcanmmodel.data;

import java.util.UUID;

import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.GLHelper;

import net.minecraft.util.ResourceLocation;

/**
 * Represents the model data right after being loaded. No optimizations or
 * offset has happened yet. This is the raw data return from the appropriate
 * loader. <br>
 * This data will then be translated into an {@link IModelData} by the
 * appropriate {@link GLHelper} to be used to render the model. This is a marker
 * interface.
 *
 * @author WorldSEnder
 *
 */
public class RawData {
	public final UUID modelUUID;
	public final String artist;
	public final ResourceLocation srcLocation;
	/**
	 * This constructor is used as an initiator and called by the loader. This
	 * loads meta-data and hands this {@link RawData} object over to the correct
	 * version loader. This version loader then instantiates a new RawData
	 * through {@link #RawData(RawData)} to copy the meta information.
	 *
	 * @param uuid
	 * @param artist
	 * @param srcLocation
	 */
	public RawData(UUID uuid, String artist, ResourceLocation srcLocation) {
		this.modelUUID = uuid;
		this.artist = artist;
		this.srcLocation = srcLocation;
	}
	/**
	 * Copy constructor.
	 *
	 * @param src
	 */
	public RawData(RawData src) {
		this.modelUUID = src.modelUUID;
		this.artist = src.artist;
		this.srcLocation = src.srcLocation;
	}
}
