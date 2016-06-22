package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.google.common.collect.Iterables;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PartBuilder {
	/*package */ ByteBuf indexBuf;
	/*package */ String name;
	/*package */ String textureName;
	/*package */ List<TesselationPoint> pointList;
	/*package */ ISkeleton skeleton;

	public PartBuilder() {
		reset();
	}

	private void reset() {
		this.indexBuf = Unpooled.buffer();
		this.name = null;
		this.textureName = null;
		this.skeleton = null;
		this.pointList = new ArrayList<>();
	}

	public PartBuilder setName(String name) {
		this.name = Objects.requireNonNull(name);
		return this;
	}

	public PartBuilder setTexture(String texName) {
		this.textureName = Objects.requireNonNull(texName);
		return this;
	}

	public PartBuilder setSkeleton(ISkeleton skeleton) {
		this.skeleton = Objects.requireNonNull(skeleton);
		return this;
	}

	public PartBuilder addPoint(TesselationPoint point) {
		this.pointList.add(point);
		return this;
	}

	public PartBuilder addPoints(Iterable<TesselationPoint> points) {
		Iterables.addAll(pointList, points);
		return this;
	}

	public PartBuilder addFace(short index1, short index2, short index3) {
		this.indexBuf.writeShort(index1);
		this.indexBuf.writeShort(index2);
		this.indexBuf.writeShort(index3);
		return this;
	}

	public PartBuilder addIndices(short[] indices) {
		this.indexBuf.ensureWritable(indices.length * 2);
		for (int i = 0; i < indices.length; i++) {
			this.indexBuf.writeShort(indices[i]);
		}
		return this;
	}
}
