package com.github.worldsender.mcanm.common.skeleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import org.apache.commons.lang3.ArrayUtils;

import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.skeleton.parts.Bone;
import com.github.worldsender.mcanm.common.skeleton.parts.Bone.BoneBuilder;
import com.github.worldsender.mcanm.common.skeleton.stored.RawData;
import com.github.worldsender.mcanm.common.skeleton.visitor.IBoneVisitor;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitable;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitor;
import com.github.worldsender.mcanm.common.util.ReloadableData;

import net.minecraft.client.renderer.Tessellator;

public abstract class AbstractSkeleton extends ReloadableData<ISkeletonVisitable> implements ISkeleton {

	private static int doBFSSingleBone(byte[] parents, int index, List<List<Integer>> layers, int[] layerNumbers) {
		if (index == 0xFF)
			return -1;
		if (layerNumbers[index] != -1)
			return layerNumbers[index];
		// Determine parent
		int parent = parents[index] & 0xFF;
		// Determine layer in tree and handle parent first
		int layerNbr = doBFSSingleBone(parents, parent, layers, layerNumbers) + 1;
		// Else handle
		layerNumbers[index] = layerNbr;
		// Get layer
		if (layers.size() <= layerNbr)
			layers.add(new ArrayList<Integer>());
		List<Integer> layer = layers.get(layerNbr);
		// Add current index
		layer.add(index);
		return layerNbr;
	}

	/**
	 * Orders the bonesBreadthFirst in a breadth first order. This trusts in the bonesBreadthFirst having a tree-like
	 * structure. Parent bones will always be ordered before their children
	 * 
	 * @param src
	 *            the bone
	 * @return indices in an order that is breadth first.
	 */
	private static int[] doBFSBoneOrdering(byte[] parents) {
		List<List<Integer>> layers = new ArrayList<List<Integer>>();
		int[] layerNumber = new int[parents.length];
		Arrays.fill(layerNumber, -1);
		for (int i = 0; i < parents.length; ++i) {
			doBFSSingleBone(parents, i, layers, layerNumber);
		}
		int[] breadthFirst = new int[parents.length];
		int i = 0;
		for (List<Integer> layer : layers) {
			for (int b : layer) {
				breadthFirst[i++] = b;
			}
		}
		return breadthFirst;
	}

	private class SkeletonVisitor implements ISkeletonVisitor {
		private List<Byte> parentIndices = new ArrayList<>();
		private List<Supplier<Bone>> boneSuppliers = new ArrayList<>();
		private Bone[] bones = null;

		@Override
		public IBoneVisitor visitBone(String name) {
			assert parentIndices.size() == boneSuppliers.size();
			final int boneIndex = parentIndices.size();
			parentIndices.add(boneIndex, (byte) 0xFF);
			boneSuppliers.add(boneIndex, null);

			return new IBoneVisitor() {
				private byte parentIndex = -1;
				private BoneBuilder builder = new BoneBuilder(name);

				@Override
				public void visitParent(byte parentIndex) {
					parentIndices.set(boneIndex, parentIndex);
					this.parentIndex = parentIndex;
				}

				@Override
				public void visitLocalRotation(Quat4f rotation) {
					builder.setRotation(rotation);
				}

				@Override
				public void visitLocalOffset(Vector3f headPosition) {
					builder.setOffset(headPosition);
				}

				@Override
				public void visitEnd() {
					if (parentIndex != -1) {
						boneSuppliers.set(boneIndex, () -> builder.setParent(bones[parentIndex]).build());
					} else {
						boneSuppliers.set(boneIndex, builder::build);
					}
				}
			};
		}

		@Override
		public void visitEnd() {
			assert parentIndices.size() == boneSuppliers.size();
			int size = boneSuppliers.size();
			byte[] parentList = ArrayUtils.toPrimitive(parentIndices.toArray(ArrayUtils.EMPTY_BYTE_OBJECT_ARRAY));
			int[] breadthFirstOrdering = doBFSBoneOrdering(parentList);

			AbstractSkeleton.this.bonesByIndex = bones = new Bone[size];
			AbstractSkeleton.this.bonesBreadthFirst = new Bone[size];
			AbstractSkeleton.this.bonesByName.clear();

			for (int i = 0; i < size; i++) {
				// We have to make the bone breadth first because the supplier accesses its parent bones
				int index = breadthFirstOrdering[i];
				Bone b = Objects.requireNonNull(boneSuppliers.get(index).get());
				bonesBreadthFirst[i] = bonesByIndex[index] = b;
				bonesByName.put(b.name, b);
			}
		}
	}

	private Bone[] bonesBreadthFirst;
	private Bone[] bonesByIndex;
	private Map<String, Bone> bonesByName;

	public AbstractSkeleton(IResourceLocation resLoc, Function<IResource, ISkeletonVisitable> readFunc) {
		super(resLoc, readFunc, RawData.MISSING_DATA);
	}

	@Override
	protected void preInit(Object... args) {
		bonesByName = new HashMap<>();
	}

	@Override
	protected void loadData(ISkeletonVisitable data) {
		data.visitBy(this.new SkeletonVisitor());
	}

	@Override
	public IBone getBoneByName(String boneName) {
		IBone bone = bonesByName.get(boneName);
		return bone == null ? IBone.STATIC_BONE : bone;
	}

	@Override
	public IBone getBoneByIndex(int index) {
		return index < 0 || index >= bonesByIndex.length ? IBone.STATIC_BONE : bonesByIndex[index];
	}

	@Override
	public void setup(IAnimation animation, float frame) {
		for (Bone bone : bonesBreadthFirst) {
			bone.setTransformation(animation, frame);
		}
	}

	@Override
	public void debugDraw(Tessellator tess) {
		return;
		/*
		glDisable(GL_TEXTURE_2D);
		glDisable(GL_DEPTH_TEST);
		tess.getWorldRenderer().begin(GL_LINES, new VertexFormat().addElement(VertexFormatElement.EnumUsage.UV));
		startDrawing(GL_LINES);
		glColor4f(0f, 0f, 0f, 1f);
		for (Bone bone : bonesBreadthFirst) {
			Vector4f tail = bone.getTail();
			Vector4f head = bone.getHead();
			tess.addVertex(tail.x, tail.z, -tail.y);
			tess.addVertex(head.x, head.z, -head.y);
		}
		tess.draw();
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_TEXTURE_2D);*/
	}

}
