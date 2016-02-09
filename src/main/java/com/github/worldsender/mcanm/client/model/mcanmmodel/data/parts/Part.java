package com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts;

import java.util.Arrays;

import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.client.model.mcanmmodel.IRenderPass;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;

public class Part {

	private final Point[] pointsList;
	private final ResourceLocation resLocation;
	private final short[] indices; // Unsigned
	private final String name;

	public Part(RawDataV1.ModelPart data, Bone[] bones) {
		Point[] points = new Point[data.points.length];
		int idx = 0;
		for (RawDataV1.TesselationPoint point : data.points) {
			points[idx++] = Point.from(point, bones);
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
			this.pointsList[idx & 0xFFFF].render(currentPass.getTesselator().getWorldRenderer());
		}
	}

	public String getName() {
		return this.name;
	}
}
