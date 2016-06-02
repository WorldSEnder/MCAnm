package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.google.common.collect.Iterables;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.ResourceLocation;

public class Part {
	public static class PartBuilder {
		private ByteBuf indexBuf;
		private String name;
		private String textureName;
		private List<TesselationPoint> pointList;
		private ISkeleton skeleton;

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

		public Part build() {
			Point[] points = new Point[pointList.size()];
			int idx = 0;
			for (TesselationPoint point : pointList) {
				points[idx++] = Point.from(point, skeleton);
			}
			short[] indices = new short[indexBuf.readableBytes() / 2];
			indexBuf.nioBuffer().asShortBuffer().get(indices);
			return new Part(points, name, textureName, indices);
		}
	}

	private final Point[] pointsList;
	private final String textureName;
	private final short[] indices; // Unsigned
	private final String name;

	private Part(Point[] points, String name, String texName, short[] indices) {
		for (short i : indices) {
			if (i < 0 || i >= points.length) {
				throw new IllegalArgumentException(
						"face index " + i + " too big. Only " + points.length + " points available");
			}
		}
		this.pointsList = points;
		this.name = Objects.requireNonNull(name, "A name is required");
		this.textureName = Objects.requireNonNull(texName, "texture name required");
		this.indices = indices;
	}

	public void render(IRenderPass currentPass) {
		ResourceLocation texture = currentPass.getActualResourceLocation(textureName);
		currentPass.bindTexture(texture);
		for (short idx : this.indices) {
			this.pointsList[idx & 0xFFFF].render(currentPass.getTesselator());
		}
	}

	public String getName() {
		return this.name;
	}
}
