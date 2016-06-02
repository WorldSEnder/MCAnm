package com.github.worldsender.mcanm.client.mcanmmodel.stored.parts;

import java.io.DataInputStream;
import java.io.IOException;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.common.Utils;

public class RawBone {
	public String name;
	public Quat4f rotation;
	public Vector3f offset;
	/** Parent of this bone as array index. A value of 0xFF means no parent */
	public byte parent;

	public static RawBone readBoneFrom(DataInputStream dis) throws IOException {
		RawBone bone = new RawBone();
		String name = Utils.readString(dis);
		Quat4f quat = Utils.readQuat(dis);
		Vector3f offset = Utils.readVector3f(dis);
		bone.name = name;
		bone.rotation = quat;
		bone.offset = offset;
		return bone;
	}

}
