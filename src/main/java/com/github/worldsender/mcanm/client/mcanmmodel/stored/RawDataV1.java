package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.HeaderV1;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.Material;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.ModelPartV1;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.RawBone;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IMaterialVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IPartVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.skeleton.visitor.IBoneVisitor;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitable;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitor;

public class RawDataV1 implements IVersionSpecificData, ISkeletonVisitable {
	/**
	 * A list of all parts
	 */
	public final ModelPartV1[] parts;
	/**
	 * A list of all bones, in read order
	 */
	public final RawBone[] bones;
	public static final int MAX_NBR_BONEBINDINGS = 4;

	public RawDataV1(ModelPartV1[] parts, RawBone[] bones) {
		this.parts = parts;
		this.bones = bones;
	}

	@Override
	public void visitBy(IModelVisitor visitor) {
		for (ModelPartV1 part : parts) {
			IPartVisitor partVisitor = visitor.visitPart(part.name);
			partVisitor.visitTesselationPoints(part.points);
			partVisitor.visitFaces(part.indices);
			{
				IMaterialVisitor matVisitor = partVisitor.visitTexture();
				matVisitor.visitTexture(part.material.resLocationRaw);
				matVisitor.visitEnd();
			}
			partVisitor.visitEnd();
		}
	}

	@Override
	public void visitBy(ISkeletonVisitor visitor) {
		IBoneVisitor[] boneVisitors = new IBoneVisitor[bones.length];
		for (int i = 0; i < bones.length; i++) {
			boneVisitors[i] = visitor.visitBone(bones[i].name);
		}
		for (int i = 0; i < bones.length; i++) {
			IBoneVisitor boneVisitor = boneVisitors[i];
			RawBone bone = bones[i];
			if (bone.parent != 0xFF) {
				boneVisitor.visitParent(bone.parent);
			}
			boneVisitor.visitLocalOffset(bone.offset);
			boneVisitor.visitLocalRotation(bone.rotation);
			boneVisitor.visitEnd();
		}
		visitor.visitEnd();
	}

	public static final RawDataV1 loadFrom(DataInputStream di) throws IOException, ModelFormatException {
		// Read the header
		HeaderV1 header = new HeaderV1();
		int nbrParts = di.readUnsignedByte();
		int nbrBones = di.readUnsignedByte();
		header.nbrBones = nbrBones;
		header.nbrParts = nbrParts;
		// Read parts
		ModelPartV1[] parts = new ModelPartV1[nbrParts];
		Set<String> partsNameSet = new HashSet<>();
		for (int i = 0; i < nbrParts;) {
			ModelPartV1 newPart = readPartFrom(di, header);
			if (!partsNameSet.add(newPart.name))
				throw new ModelFormatException("Two parts with same name " + newPart.name);
			parts[i++] = newPart;
		}
		// Read bones
		RawBone[] bones = new RawBone[nbrBones];
		Set<String> boneNameSet = new HashSet<>();
		for (int i = 0; i < nbrBones; i++) {
			RawBone newBone = RawBone.readBoneFrom(di);
			if (!boneNameSet.add(newBone.name))
				throw new ModelFormatException("Two bones with same name " + newBone.name);
			bones[i] = newBone;
		}
		// Read parents
		readBoneParents(di, bones); // Structure has to be tree-like
		// Apply data
		return new RawDataV1(parts, bones);
	}

	private static void readBoneParents(DataInputStream di, RawBone[] bones) throws IOException {
		int nbrBones = bones.length;
		for (RawBone bone : bones) {
			int parentIndex = di.readUnsignedByte();
			if (parentIndex != 255 && parentIndex >= nbrBones) {
				throw new ModelFormatException(
						String.format("ParentIndex (%d) has to be smaller than nbrBones (%d).", parentIndex, nbrBones));
			}
			bone.parent = (byte) parentIndex;
		}
	}

	private static ModelPartV1 readPartFrom(DataInputStream di, HeaderV1 header) throws IOException {
		ModelPartV1 mp = new ModelPartV1();
		// Read "header"
		int nbrPoints = di.readUnsignedShort();
		int nbrIndices = di.readUnsignedShort() * 3;
		// Read name
		String name = Utils.readString(di);
		// Read Material
		Material material = Material.readFrom(di);
		// Read points
		TesselationPoint[] vertexArray = new TesselationPoint[nbrPoints];
		for (int i = 0; i < nbrPoints; vertexArray[i++] = TesselationPoint.readFrom(di))
			;
		// Read indices
		short[] indexArray = new short[nbrIndices];
		for (int i = 0; i < nbrIndices; ++i) {
			int candidate = di.readUnsignedShort();
			if (candidate >= nbrPoints)
				throw new ModelFormatException(
						String.format("Vertexindex (%d) has to be smaller than nbrPoints (%d).", candidate, nbrPoints));
			indexArray[i] = (short) candidate;
		}
		// Apply attributes
		mp.name = name;
		mp.material = material;
		mp.points = vertexArray;
		mp.indices = indexArray;
		return mp;
	}

}
