package com.github.worldsender.mcanm.client.mcanmmodel.stored.parts;

import java.util.Arrays;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.RawDataV1;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.util.ResourceLocation;

public class Part {

	private final Point[] pointsList;
	private final ResourceLocation resLocation;
	private final short[] indices; // Unsigned
	private final String name;

	public Part(RawDataV1.ModelPart data, ISkeleton skelet) {
		Point[] points = new Point[data.points.length];
		int idx = 0;
		for (RawDataV1.TesselationPoint point : data.points) {
			points[idx++] = Point.from(point, skelet);
		}
		this.pointsList = points;
		this.resLocation = new ResourceLocation(data.material.resLocationRaw);
		this.indices = Arrays.copyOf(data.indices, data.indices.length);
		this.name = data.name;
	}

	public void render(IRenderPass currentPass) {
		ResourceLocation texture = currentPass.getActualResourceLocation(resLocation);
		currentPass.bindTexture(texture);
		for (short idx : this.indices) {
			this.pointsList[idx & 0xFFFF].render(currentPass.getTesselator());
		}
	}

	public String getName() {
		return this.name;
	}
}
