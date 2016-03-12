package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.skeleton.stored.ISkeletonVisitable;
import com.github.worldsender.mcanm.common.skeleton.stored.ISkeletonVisitor;

public class RawDataV1 implements IVersionSpecificData, ISkeletonVisitable {

	public static class BoneBinding {
		/** To be interpreted as unsigned */
		public byte boneIndex;
		public float bindingValue;
	}

	public static class Material {
		public String resLocationRaw;
	}

	public static class TesselationPoint {
		public Vector3f coords;
		public Vector3f normal;
		public Vector2f texCoords;
		public BoneBinding[] boneBindings;
	}

	public static class ModelPart {
		public String name;
		/** All available {@link TesselationPoint}s in this part of the model */
		public TesselationPoint[] points;
		/**
		 * The array to store the order of the {@link TesselationPoint}s. To be interpreted as unsigned.
		 */
		public short[] indices;
		/** The material this part of the model uses */
		public Material material;
	}

	public static class Bone {
		public String name;
		public Quat4f rotation;
		public Vector3f offset;
		/** Parent of this bone as array index. A value of 0xFF means no parent */
		public byte parent;
	}

	public static class Header {
		/**
		 * Unsigned byte
		 */
		public int nbrParts;
		/**
		 * Unsigned byte
		 */
		public int nbrBones;
	}

	// Header as is is automatically part of the model
	// public Header header;
	/**
	 * A list of all parts
	 */
	public final ModelPart[] parts;
	/**
	 * A list of all bones, in read order
	 */
	public final Bone[] bones;
	public static final int MAX_NBR_BONEBINDINGS = 4;

	public RawDataV1(ModelPart[] parts, Bone[] bones) {
		this.parts = parts;
		this.bones = bones;
	}

	@Override
	public void visitBy(IModelVisitor visitor) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visitBy(ISkeletonVisitor visitor) {
		// TODO Auto-generated method stub
	}

	public static final RawDataV1 loadFrom(DataInputStream di) throws IOException, ModelFormatException {
		// Read the header
		Header header = new Header();
		int nbrParts = di.readUnsignedByte();
		int nbrBones = di.readUnsignedByte();
		header.nbrBones = nbrBones;
		header.nbrParts = nbrParts;
		// Read parts
		ModelPart[] parts = new ModelPart[nbrParts];
		Set<String> partsNameSet = new HashSet<>();
		for (int i = 0; i < nbrParts;) {
			ModelPart newPart = readPartFrom(di, header);
			if (!partsNameSet.add(newPart.name))
				throw new ModelFormatException("Two parts with same name " + newPart.name);
			parts[i++] = newPart;
		}
		// Read bones
		Bone[] bones = new Bone[nbrBones];
		Set<String> boneNameSet = new HashSet<>();
		for (int i = 0; i < nbrBones;) {
			Bone newBone = readBoneFrom(di, header);
			if (!boneNameSet.add(newBone.name))
				throw new ModelFormatException("Two bones with same name " + newBone.name);
			bones[i++] = newBone;
		}
		// Read parents
		readBoneParents(di, bones); // Structure has to be tree-like
		// Apply data
		return new RawDataV1(parts, bones);
	}

	private static void readBoneParents(DataInputStream di, Bone[] bones) throws IOException {
		// TODO: check for treeish structure
		int nbrBones = bones.length;
		for (Bone bone : bones) {
			int parentIndex = di.readUnsignedByte();
			if (parentIndex != 255 && parentIndex >= nbrBones) {
				throw new ModelFormatException(
						String.format("ParentIndex (%d) has to be smaller than nbrBones (%d).", parentIndex, nbrBones));
			}
			bone.parent = (byte) parentIndex;
		}
	}

	private static Bone readBoneFrom(DataInputStream di, Header header) throws IOException {
		Bone bone = new Bone();
		// Read name
		String name = Utils.readString(di);
		// Read quaternion
		Quat4f quat = Utils.readQuat(di);
		// Read offset
		Vector3f offset = Utils.readVector3f(di);
		// Apply attributes
		bone.name = name;
		bone.rotation = quat;
		bone.offset = offset;
		return bone;
	}

	private static ModelPart readPartFrom(DataInputStream di, Header header) throws IOException {
		ModelPart mp = new ModelPart();
		// Read "header"
		int nbrPoints = di.readUnsignedShort();
		int nbrIndices = di.readUnsignedShort() * 3;
		// Read name
		String name = Utils.readString(di);
		// Read Material
		Material material = readMaterialFrom(di, header);
		// Read points
		TesselationPoint[] vertexArray = new TesselationPoint[nbrPoints];
		for (int i = 0; i < nbrPoints; vertexArray[i++] = readPointFrom(di, header))
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

	private static Material readMaterialFrom(DataInputStream di, Header header) throws IOException {
		Material tex = new Material();
		tex.resLocationRaw = Utils.readString(di);
		return tex;
	}

	private static TesselationPoint readPointFrom(DataInputStream di, Header header) throws IOException {
		TesselationPoint tessP = new TesselationPoint();
		// Read coords
		Vector3f coords = Utils.readVector3f(di);
		// Read normal
		Vector3f normal = Utils.readVector3f(di);
		if (normal.length() == 0)
			throw new ModelFormatException("Normal vector can't have zerolength.");
		// Read material coordinates
		Vector2f texCoords = Utils.readVector2f(di);
		// Read bindings
		BoneBinding[] bindings = readBoneBindingsFrom(di, header);
		// Apply attributes
		tessP.coords = coords;
		tessP.normal = normal;
		tessP.texCoords = texCoords;
		tessP.boneBindings = bindings;
		return tessP;
	}

	private static BoneBinding[] readBoneBindingsFrom(DataInputStream di, Header header) throws IOException {
		BoneBinding[] bindings = new BoneBinding[RawDataV1.MAX_NBR_BONEBINDINGS];
		int bindIndex;
		int i = 0;
		while (i < RawDataV1.MAX_NBR_BONEBINDINGS && (bindIndex = di.readUnsignedByte()) != 0xFF) {
			BoneBinding binding = new BoneBinding();
			if (bindIndex >= header.nbrBones)
				throw new ModelFormatException("Can't bind to non-existant bone.");
			// Read strength of binding
			float bindingValue = di.readFloat();
			if (Math.abs(bindingValue) > 100.0F || bindingValue < 0)
				throw new ModelFormatException(
						String.format(
								"Value for binding seems out of range: %f (expected to be in [0, 100]",
								bindingValue));
			// Apply attributes
			binding.boneIndex = (byte) bindIndex;
			binding.bindingValue = bindingValue;
			bindings[i++] = binding;
		}
		return Arrays.copyOf(bindings, i);
	}
}
