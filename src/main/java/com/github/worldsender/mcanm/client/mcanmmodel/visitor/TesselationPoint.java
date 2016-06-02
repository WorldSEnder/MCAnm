package com.github.worldsender.mcanm.client.mcanmmodel.visitor;

import java.io.DataInputStream;
import java.io.IOException;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

public class TesselationPoint {
	public Vector3f coords;
	public Vector3f normal;
	public Vector2f texCoords;
	public BoneBinding[] boneBindings;

	public static TesselationPoint readFrom(DataInputStream di) throws IOException {
		TesselationPoint tessP = new TesselationPoint();
		// Read coords
		Vector3f coords = Utils.readVector3f(di);
		// Read normal
		Vector3f normal = Utils.readVector3f(di);
		if (normal.length() == 0)
			throw new ModelFormatException("Normal vector can't have zerolength.");
		// Read materialIndex coordinates
		Vector2f texCoords = Utils.readVector2f(di);
		// Read bindings
		BoneBinding[] bindings = BoneBinding.readMultipleFrom(di);
		// Apply attributes
		tessP.coords = coords;
		tessP.normal = normal;
		tessP.texCoords = texCoords;
		tessP.boneBindings = bindings;
		return tessP;
	}
}
