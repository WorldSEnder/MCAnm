package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import java.util.Objects;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.client.renderer.DrawElementsTesselator;

import net.minecraft.util.ResourceLocation;

public class PartDirect implements IPart {
	private final String textureName;
	private final String name;
	private final Point[] pointsList;
	private final DrawElementsTesselator directTesselator;

	public PartDirect(PartBuilder builder) {
		Point[] points = new Point[builder.pointList.size()];
		int idx = 0;
		for (TesselationPoint point : builder.pointList) {
			points[idx++] = Point.from(point, builder.skeleton);
		}
		short[] indices = new short[builder.indexBuf.readableBytes() / 2];
		builder.indexBuf.nioBuffer().asShortBuffer().get(indices);
		for (short i : indices) {
			if (i < 0 || i >= points.length) {
				throw new IllegalArgumentException(
						"face index " + i + " too big. Only " + points.length + " points available");
			}
		}
		this.pointsList = points;
		this.name = Objects.requireNonNull(builder.name, "A name is required");
		this.textureName = Objects.requireNonNull(builder.textureName, "texture name required");
		directTesselator = new DrawElementsTesselator(pointsList.length, indices);
	}

	@Override
	public void render(IRenderPass currentPass) {
		ResourceLocation texture = currentPass.getActualResourceLocation(textureName);
		currentPass.bindTexture(texture);

		directTesselator.startDrawing();
		for (Point p : this.pointsList) {
			p.render(directTesselator);
		}
		directTesselator.draw();
	}

	@Override
	public String getName() {
		return this.name;
	}
}
