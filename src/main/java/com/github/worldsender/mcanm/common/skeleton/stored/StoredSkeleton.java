package com.github.worldsender.mcanm.common.skeleton.stored;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector4f;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.RawDataV1;
import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.skeleton.IBone;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.client.renderer.Tessellator;

public class StoredSkeleton implements ISkeleton {

	private Bone[] bones; // Breadth-first

	private static int handleBone(
			RawDataV1.Bone bones[],
			int index,
			List<List<Integer>> layers,
			List<Integer> handled) {
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
	 * Orders the bones in a breadth first order. This trusts in the bones having a tree-like structure.
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

	@Override
	public IBone getBoneByName(String bone) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IBone getBoneByIndex(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setup(IAnimation animation, float frame) {
		for (Bone bone : bones) {
			bone.setTransformation(animation, frame);
		}
	}

	@Override
	public void debugDraw(Tessellator tess) {
		glDisable(GL_TEXTURE_2D);
		tess.startDrawing(GL_LINES);
		glColor4f(0f, 0f, 0f, 1f);
		for (Bone bone : bones) {
			Vector4f tail = bone.getTail();
			Vector4f head = bone.getHead();
			tess.addVertex(tail.x, tail.z, -tail.y);
			tess.addVertex(head.x, head.z, -head.y);
		}
		tess.draw();
		glEnable(GL_TEXTURE_2D);
	}

}
