package com.github.worldsender.mcanm.client.mcanmmodel.loader;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.client.mcanmmodel.Utils;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawData;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1.Bone;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1.BoneBinding;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1.Header;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1.Material;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1.ModelPart;
import com.github.worldsender.mcanm.client.mcanmmodel.data.RawDataV1.TesselationPoint;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

/**
 * Loads a file with version signature 1. This implementation follows the all or nothing principle, so no corrupt data
 * will ever be accessible.<br>
 * To find a full specification of the file format please visit<br>
 *
 * @author WorldSEnder
 *
 */
public class LoaderVersion1 extends VersionizedModelLoader {
	public static final LoaderVersion1 instance = new LoaderVersion1();

	@Override
	public RawDataV1 loadFromInputStream(RawData meta, int version, DataInputStream di) throws IOException {
		// Read the header
		Header header = new Header();
		int nbrParts = di.readUnsignedByte();
		int nbrBones = di.readUnsignedByte();
		header.nbrBones = nbrBones;
		header.nbrParts = nbrParts;
		// Read parts
		ModelPart[] parts = new ModelPart[nbrParts];
		Set<String> partsNameSet = new TreeSet<>();
		for (int i = 0; i < nbrParts;) {
			ModelPart newPart = readPartFrom(di, header);
			if (!partsNameSet.add(newPart.name))
				throw new ModelFormatException("Two parts with same name " + newPart.name);
			parts[i++] = newPart;
		}
		// Read bones
		Bone[] bones = new Bone[nbrBones];
		Set<String> boneNameSet = new TreeSet<>();
		for (int i = 0; i < nbrBones;) {
			Bone newBone = readBoneFrom(di, header);
			if (!boneNameSet.add(newBone.name))
				throw new ModelFormatException("Two bones with same name " + newBone.name);
			bones[i++] = newBone;
		}
		// Read parents
		readBoneParents(di, bones); // Structure has to be tree-like
		// Apply data
		return new RawDataV1(meta, parts, bones);
	}

	/**
	 * This method has to check for a tree-like structure of the bones. No bone can be parent of his parent or his
	 * parent's parent, etc.
	 *
	 * @param di
	 *            The {@link DataInputStream} to read indices from
	 * @param bones
	 *            the bone array with read bones
	 * @throws EOFException
	 *             if the file ends unexpectedly
	 * @throws IOException
	 *             if an IOException occurs on the {@link DataInputStream}
	 */
	protected void readBoneParents(DataInputStream di, Bone[] bones) throws EOFException, IOException {
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

	protected Bone readBoneFrom(DataInputStream di, Header header) throws EOFException, IOException {
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

	protected ModelPart readPartFrom(DataInputStream di, Header header) throws EOFException, IOException {
		ModelPart mp = new ModelPart();
		// Read "header"
		int nbrPoints = di.readUnsignedShort();
		int nbrIndices = di.readUnsignedShort() * 3;
		// Read name
		String name = Utils.readString(di);
		// Read Material
		Material material = this.readMaterialFrom(di, header);
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

	protected Material readMaterialFrom(DataInputStream di, Header header) throws EOFException, IOException {
		Material tex = new Material();
		tex.resLocationRaw = Utils.readString(di);
		return tex;
	}

	protected TesselationPoint readPointFrom(DataInputStream di, Header header) throws EOFException, IOException {
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

	protected BoneBinding[] readBoneBindingsFrom(DataInputStream di, Header header) throws EOFException, IOException {
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
