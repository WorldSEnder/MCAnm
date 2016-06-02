package com.github.worldsender.mcanm.client.mcanmmodel.stored.parts;

import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;

public class ModelPartV1 {
	public String name;
	/** All available {@link TesselationPoint}s in this part of the model */
	public TesselationPoint[] points;
	/**
	 * The array to store the order of the {@link TesselationPoint}s. To be interpreted as unsigned.
	 */
	public short[] indices;
	/** The materialIndex this part of the model uses */
	public Material material;
}
