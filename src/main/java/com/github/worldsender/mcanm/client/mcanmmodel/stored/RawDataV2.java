package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import java.io.DataInputStream;
import java.io.IOException;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.Material;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.ModelPartV2;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IMaterialVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IPartVisitor;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

public class RawDataV2 implements IVersionSpecificData {

	private ModelPartV2[] parts;
	private Material[] mats;

	@Override
	public void visitBy(IModelVisitor visitor) {
		for (ModelPartV2 part : parts) {
			IPartVisitor partVisitor = visitor.visitPart(part.name);
			partVisitor.visitTesselationPoints(part.points);
			partVisitor.visitFaces(part.indices);
			{
				IMaterialVisitor matVisitor = partVisitor.visitTexture();
				matVisitor.visitTexture(mats[part.materialIndex].resLocationRaw);
				matVisitor.visitEnd();
			}
			partVisitor.visitEnd();
		}
	}

	public static final RawDataV2 loadFrom(DataInputStream dis) throws IOException, ModelFormatException {
		RawDataV2 data = new RawDataV2();
		int nbrParts = dis.readUnsignedByte();
		int nbrMaterials = dis.readUnsignedByte();

		data.parts = new ModelPartV2[nbrParts];
		data.mats = new Material[nbrMaterials];
		for (int i = 0; i < nbrParts; i++) {
			data.parts[i] = ModelPartV2.readFrom(dis);
		}
		for (int i = 0; i < nbrMaterials; i++) {
			data.mats[i] = Material.readFrom(dis);
		}
		return data;
	}
}
