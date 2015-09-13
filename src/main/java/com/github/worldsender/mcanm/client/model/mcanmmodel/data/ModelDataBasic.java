package com.github.worldsender.mcanm.client.model.mcanmmodel.data;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector4f;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts.Bone;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts.Part;
import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.IRenderPass;
import com.google.common.base.Predicate;

public class ModelDataBasic implements IModelData {

	private final Part[] parts; // May have Random order
	private final Bone[] bones; // Breadth-first
	private static int handleBone(RawDataV1.Bone bones[], int index,
			List<List<Integer>> layers, List<Integer> handled) {
		if (index == 0xFF)
			return -1;
		// Determine parent
		int parent = bones[index].parent & 0xFF;
		// Determine layer in tree and handle parent first
		int layerNbr = handleBone(bones, parent, layers, handled) + 1;
		// If already handled
		if (handled.contains(index))
			return layerNbr;
		// Else handle
		handled.add(index);
		// Get layer
		if (layers.size() <= layerNbr)
			layers.add(new ArrayList<Integer>());
		List<Integer> layer = layers.get(layerNbr);
		// Add current index
		layer.add(index);
		return layerNbr;
	}
	/**
	 * Orders the bones in a breadth first order. This trusts in the bones
	 * having a tree-like structure.
	 *
	 * @param src
	 *            the bone
	 * @return indices in an order that is breadth first.
	 */
	private static int[] breadthFirst(RawDataV1.Bone[] src) {
		List<List<Integer>> layers = new ArrayList<List<Integer>>();
		List<Integer> handled = new ArrayList<>();
		for (int i = 0; i < src.length; ++i) {
			handleBone(src, i, layers, handled);
		}
		int[] breadthFirst = new int[src.length];
		int i = 0;
		for (List<Integer> layer : layers) {
			for (int b : layer) {
				breadthFirst[i++] = b;
			}
		}
		return breadthFirst;
	}

	public ModelDataBasic(RawDataV1 data) {
		Part[] parts = new Part[data.parts.length];
		// This is in original order
		Bone[] bones = new Bone[data.bones.length];
		int[] breadthOrder = breadthFirst(data.bones);
		// This is in breadth first order
		Bone[] breadthFirst = new Bone[data.bones.length];

		for (int i = 0; i < breadthOrder.length; ++i) {
			int idx = breadthOrder[i];
			RawDataV1.Bone bone = data.bones[idx];
			Bone newBone = Bone.fromData(bone, bones);
			bones[idx] = newBone;
			breadthFirst[i] = newBone;
		}
		for (int i = 0; i < data.parts.length; ++i) {
			parts[i] = new Part(data.parts[i], bones);
		}
		// Now we have to restructure the bones to breadth-first order
		this.bones = breadthFirst;
		this.parts = parts;
	}
	/**
	 * Sets up all bones for the following draw call. It is assumed that the
	 * bones are present in a breadth-first order so that applying a
	 * transformation to a bone can already access the transformed parent of
	 * this bone
	 *
	 * @param anim
	 *            the animation currently executed
	 * @param frame
	 *            the frame in the animation
	 * @param subFrame
	 *            the subframe in the animation
	 */
	private void setupBones(IAnimation anim, float frame) {
		for (Bone bone : this.bones) {
			bone.setTransformation(anim, frame);
		}
	}

	public void setup(IAnimation currAnimation, float frame) {
		setupBones(currAnimation, frame);
	}

	@Override
	public void render(IRenderPass pass) {
		WorldRenderer renderer = pass.getRenderer();
		Predicate<String> filter = pass.getPartPredicate();

		setup(pass.getAnimation(), pass.getFrame());
		renderer.startDrawing(GL_TRIANGLES);
		for (Part part : this.parts) {
			if (filter.apply(part.getName()))
				part.render(renderer);
		}
		renderer.draw();
		if (MCAnm.isDebug) {
			// glDisable(GL_TEXTURE_2D);
			GlStateManager.func_179090_x();
			renderer.startDrawing(GL_LINES);
			GlStateManager.color(0f, 0f, 0f, 1f);
			for (Bone bone : bones) {
				Vector4f tail = bone.getTail();
				Vector4f head = bone.getHead();
				renderer.addVertex(tail.x, tail.z, -tail.y);
				renderer.addVertex(head.x, head.z, -head.y);
			}
			renderer.draw();
			// glEnable(GL_TEXTURE_2D);
			GlStateManager.func_179090_x();
		}
	}
}
