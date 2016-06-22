package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.Objects;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.client.renderer.ITesselator;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

public class PartTesselator implements IPart {
	private final Point[] pointsList;
	private final String textureName;
	private final short[] indices; // Unsigned
	private final String name;

	public PartTesselator(PartBuilder builder) {
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
		this.indices = indices;
	}

	@Override
	public void render(IRenderPass currentPass) {
		Tessellator tess = Tessellator.instance;

		ResourceLocation texture = currentPass.getActualResourceLocation(textureName);
		currentPass.bindTexture(texture);

		tess.startDrawing(GL_TRIANGLES);
		for (short idx : this.indices) {
			this.pointsList[idx & 0xFFFF].render(ITesselator.TESSELATOR_WRAPPER);
		}
		tess.draw();
	}

	@Override
	public String getName() {
		return this.name;
	}
}
