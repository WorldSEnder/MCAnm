package com.github.worldsender.mcanm.client.model.mcanmmodel.data;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class RawDataV1 extends RawData {

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

	public RawDataV1(RawData metaInfo, ModelPart[] parts, Bone[] bones) {
		super(metaInfo);
		this.parts = parts;
		this.bones = bones;
	}
}
