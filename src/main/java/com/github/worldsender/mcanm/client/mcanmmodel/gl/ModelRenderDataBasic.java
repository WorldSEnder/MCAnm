package com.github.worldsender.mcanm.client.mcanmmodel.gl;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.parts.Part;
import com.github.worldsender.mcanm.client.mcanmmodel.parts.Part.PartBuilder;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IMaterialVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitable;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IPartVisitor;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.client.renderer.Tessellator;

public class ModelRenderDataBasic implements IModelRenderData {
	private class ModelVisitor implements IModelVisitor {
		List<Part> parts = new ArrayList<>();

		@Override
		public void visitModelUUID(UUID uuid) {}

		@Override
		public void visitArtist(String artist) {}

		@Override
		public IPartVisitor visitPart(String name) {
			final int partIndex = parts.size();
			parts.add(null);
			return new IPartVisitor() {
				private PartBuilder builder = new PartBuilder().setSkeleton(skeleton).setName(name);

				@Override
				public void visitEnd() {
					parts.set(partIndex, builder.build());
				}

				@Override
				public void visitTesselationPoint(TesselationPoint point) {
					builder.addPoint(point);
				}

				@Override
				public void visitTesselationPoints(Iterable<TesselationPoint> points) {
					builder.addPoints(points);
				}

				@Override
				public void visitFace(short tess1, short tess2, short tess3) {
					builder.addFace(tess1, tess2, tess3);
				}

				@Override
				public void visitFaces(short[] tessOrder) {
					builder.addIndices(tessOrder);
				}

				@Override
				public IMaterialVisitor visitTexture() {
					return new IMaterialVisitor() {
						@Override
						public void visitTexture(String textureName) {
							builder.setTexture(textureName);
						}

						@Override
						public void visitEnd() {}
					};
				}
			};
		}

		@Override
		public void visitEnd() {
			ModelRenderDataBasic.this.parts = this.parts.toArray(new Part[0]);
		}
	}

	private Part[] parts; // May have Random order
	private final ISkeleton skeleton;

	public ModelRenderDataBasic(IModelVisitable data, ISkeleton skeleton) {
		this.skeleton = skeleton;
		ModelVisitor visitor = new ModelVisitor();
		data.visitBy(visitor);
	}

	/**
	 * Sets up all bones for the following draw call. It is assumed that the bones are present in a breadth-first order
	 * so that applying a transformation to a bone can already access the transformed parent of this bone
	 *
	 * @param anim
	 *            the animation currently executed
	 * @param frame
	 *            the frame in the animation
	 * @param subFrame
	 *            the subframe in the animation
	 */
	private void setupBones(IAnimation anim, float frame) {
		skeleton.setup(anim, frame);
	}

	public void setup(IAnimation currAnimation, float frame) {
		setupBones(currAnimation, frame);
	}

	@Override
	public void render(IRenderPass currentPass) {
		Tessellator tess = currentPass.getTesselator();

		setup(currentPass.getAnimation(), currentPass.getFrame());
		tess.startDrawing(GL_TRIANGLES);
		for (Part part : this.parts) {
			if (currentPass.shouldRenderPart(part.getName()))
				part.render(currentPass);
		}
		tess.draw();
		if (MCAnm.isDebug) {
			this.skeleton.debugDraw(tess);
		}
	}
}
