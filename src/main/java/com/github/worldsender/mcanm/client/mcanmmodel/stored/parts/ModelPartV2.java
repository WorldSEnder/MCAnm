package com.github.worldsender.mcanm.client.mcanmmodel.stored.parts;

import java.io.DataInputStream;
import java.io.IOException;

import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.common.Utils;

public class ModelPartV2 {
	public String name;
	/** All available {@link TesselationPoint}s in this part of the model */
	public TesselationPoint[] points;
	/**
	 * The array to store the order of the {@link TesselationPoint}s. To be interpreted as unsigned.
	 */
	public short[] indices;
	/** The materialIndex index part of the model uses (unsigned byte) */
	public int materialIndex;

	public static ModelPartV2 readFrom(DataInputStream dis) throws IOException {
		ModelPartV2 data = new ModelPartV2();
		int nbrPoints = dis.readUnsignedShort();
		int nbrIndices = dis.readUnsignedShort() * 3;
		data.points = new TesselationPoint[nbrPoints];
		data.indices = new short[nbrIndices];
		data.name = Utils.readString(dis);
		data.materialIndex = dis.readUnsignedByte();
		for (int i = 0; i < nbrPoints; i++) {
			data.points[i] = TesselationPoint.readFrom(dis);
		}
		for (int i = 0; i < nbrIndices; i++) {
			data.indices[i] = dis.readShort();
		}
		return data;
	}
}
