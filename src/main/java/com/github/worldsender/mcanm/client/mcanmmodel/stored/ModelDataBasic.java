package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.Part;
import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.client.renderer.Tessellator;

public class ModelDataBasic implements IModelData {
	private static class ModelVisitor implements IModelVisitor {
		List<Part> parts = new ArrayList<>();

		@Override
		public void visitModelUUID(UUID uuid) {}

		@Override
		public void visitArtist(String artist) {}

		public Part[] getParts() {
			return parts.toArray(new Part[0]);
		}
	}

	private final Part[] parts; // May have Random order
	private final ISkeleton skeleton;

	public ModelDataBasic(IModelVisitable data, ISkeleton skeleton) {
		// Now we have to restructure the bones to breadth-first order
		ModelVisitor visitor = new ModelVisitor();
		data.visitBy(visitor);
		this.parts = visitor.getParts();
		this.skeleton = skeleton;
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
