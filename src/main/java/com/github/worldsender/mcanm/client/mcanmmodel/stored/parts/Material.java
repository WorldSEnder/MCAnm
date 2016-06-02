package com.github.worldsender.mcanm.client.mcanmmodel.stored.parts;

import java.io.DataInputStream;
import java.io.IOException;

import com.github.worldsender.mcanm.common.Utils;

public class Material {
	public String resLocationRaw;

	public static Material readFrom(DataInputStream di) throws IOException {
		Material tex = new Material();
		tex.resLocationRaw = Utils.readString(di);
		return tex;
	}
}
