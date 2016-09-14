package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.client.model.ModelLoader;
import com.github.worldsender.mcanm.client.renderer.DrawElementsTesselator;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

public class PartDirect implements IPart {
	private final String textureSlot;
	private final String name;
	private final String textureSlotWithOct;
	private final Point[] pointsList;
	private final int[] indices;
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
		this.textureSlot = Objects.requireNonNull(builder.textureName, "texture name required");
		this.textureSlotWithOct = "#" + textureSlot;
		directTesselator = new DrawElementsTesselator(pointsList.length, indices);
		// Required for the stupid item rendering...
		this.indices = IntStream.range(0, indices.length).map(i -> indices[i] & 0xFFFF).toArray();
	}

	@Override
	public void render(IRenderPass currentPass) {
		ResourceLocation texture = currentPass.getActualResourceLocation(textureSlot);
		currentPass.bindTexture(texture);

		directTesselator.startDrawing();
		for (Point p : this.pointsList) {
			p.render(directTesselator);
		}
		directTesselator.draw();
	}

	public void getAsBakedQuads(Map<String, TextureAtlasSprite> slotToTex, VertexFormat format, List<BakedQuad> out) {
		TextureAtlasSprite tex = retrieveSprite(slotToTex);
		for (int i = 0; i < indices.length; i += 3) {
			Point point1 = pointsList[indices[i]];
			Point point2 = pointsList[indices[i + 1]];
			Point point3 = pointsList[indices[i + 2]];
			UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
			point1.putIntoBakedQuadBuilder(builder, tex);
			point2.putIntoBakedQuadBuilder(builder, tex);
			point3.putIntoBakedQuadBuilder(builder, tex);
			point1.putIntoBakedQuadBuilder(builder, tex);
			builder.setTexture(tex);
			out.add(builder.build());
		}
	}

	private TextureAtlasSprite retrieveSprite(Map<String, TextureAtlasSprite> slotToTex) {
		TextureAtlasSprite tex = slotToTex.get(textureSlotWithOct);
		if (tex != null) {
			return tex;
		}
		return slotToTex.get(ModelLoader.MISSING_SLOT_NAME);
	}

	@Override
	public String getName() {
		return this.name;
	}
}
