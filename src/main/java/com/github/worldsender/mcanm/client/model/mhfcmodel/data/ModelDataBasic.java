package com.github.worldsender.mcanm.client.model.mhfcmodel.data;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import com.github.worldsender.mcanm.client.model.mhfcmodel.Utils;
import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.IAnimation.BoneTransformation;
import com.github.worldsender.mcanm.client.model.mhfcmodel.data.RawDataV1.TesselationPoint;
import com.google.common.base.Predicate;

public class ModelDataBasic implements IModelData {
	protected static Minecraft mc = Minecraft.getMinecraft();
	private static Tessellator tess = Tessellator.instance;

	private static class Part {
		private static class Point {
			protected static class Vertex {
				private Vector4f pos;
				private Vector3f norm;
				private Vector2f uv;
				public Vertex(Vector4f pos, Vector3f norm, Vector2f uv) {
					if (pos == null)
						pos = new Vector4f();
					if (norm == null)
						norm = new Vector3f();
					if (uv == null)
						uv = new Vector2f();
					this.pos = pos;
					this.norm = norm;
					this.uv = uv;
				}
				/**
				 * Uses the {@link Tessellator} to draw the model. Take care
				 * that the Tessellator is already drawing.
				 */
				public void render() {
					tess.setNormal(norm.x, norm.z, -norm.y);
					tess.setTextureUV(uv.x, uv.y);
					tess.addVertex(pos.x, pos.z, -pos.y);
				}
				/**
				 * Offsets this Vertex by the {@link Vector4f} given.
				 *
				 * @param vector
				 *            the offset
				 */
				public void offset(Vector4f vector) {
					Vector4f.add(this.pos, vector, this.pos);
				}
				/**
				 * Adds the normal to this Vertex basically interpolating
				 * between the current normal and the given one (by their
				 * scale). No normalization is done as this is taken care of by
				 * OpenGL during rendering.
				 *
				 * @param normal
				 *            the normal to add
				 */
				public void addNormal(Vector3f normal) {
					Vector3f.add(this.norm, normal, this.norm);
				}
				/**
				 * Sets the UV texture coordinates for this vertex
				 *
				 * @param uv
				 *            the new texture coordinates
				 */
				public void setUV(Vector2f uv) {
					this.uv = uv;
				}
			}
			protected final Vertex vert;

			protected Point(Vector3f pos, Vector3f norm, Vector2f uv) {
				this.vert = new Vertex(new Vector4f(pos.x, pos.y, pos.z, 1F),
						norm, uv);
			}
			/**
			 * Renders this point, already transformed
			 *
			 * @param bones
			 *            the models bones
			 */
			public void render() {
				this.vert.render();
			}
			/**
			 * Constructs a bone from the {@link TesselationPoint} given. This
			 * is implemented in the factory style to efficiently handle bones
			 * without Bindings
			 *
			 * @param data
			 *            the point to construct from
			 * @return the constructed point
			 */
			public static Point from(RawDataV1.TesselationPoint data,
					Bone[] bones) {
				boolean isBound = data.boneBindings.length > 0;
				if (isBound)
					return new BoundPoint(data.coords, data.normal,
							data.texCoords, data.boneBindings, bones);
				return new Point(data.coords, data.normal, data.texCoords);
			}
		}

		private static class BoundPoint extends Point {
			private static class Binding {
				// Used as a buffer, doesn't requite to always create new
				// temporaries. Prevents parallelization, though
				private static Vector4f posBuff = new Vector4f();
				private static Vector4f normBuff = new Vector4f();

				private Bone bone;
				private Vertex vert;
				private float strength;
				public Binding(Bone bone, Vertex vert, float strenght) {
					this.bone = bone;
					this.vert = vert;
					this.strength = strenght;
				}
				/**
				 * Computes the current position of the vertex owned with the
				 * bone and the strenght owned and adds it position to the given
				 * vertex.
				 *
				 * @param base
				 *            the vertex to add the transformed version of the
				 *            owned vertex to
				 * @return the final vertex, a new vertex if <code>null</code>
				 *         was given
				 */
				public Vertex addTransformed(Vertex base) {
					if (base == null)
						base = new Vertex(null, null, null);
					posBuff.set(this.vert.pos.x, this.vert.pos.y,
							this.vert.pos.z, 1.0F);
					normBuff.set(this.vert.norm.x, this.vert.norm.y,
							this.vert.norm.z, 0.0F);
					// Transform matrix
					Matrix4f globalTransform = this.bone.getTransformGlobal();
					Matrix4f globalTransformIT = this.bone
							.getTransformGlobalIT();
					// Transform points with matrix
					Matrix4f.transform(globalTransform, posBuff, posBuff);
					// Transform normal
					Matrix4f.transform(globalTransformIT, normBuff, normBuff);
					// Final result
					Vector4f position = new Vector4f(posBuff);
					Vector3f normal = new Vector3f(normBuff.x, normBuff.y,
							normBuff.z);
					position.scale(this.strength);
					normal.scale(this.strength);
					base.offset(position);
					base.addNormal(normal);
					return base;
				}
				public void normalize(float sum) {
					this.strength /= sum;
				}
			}

			private List<Binding> binds;

			public BoundPoint(Vector3f pos, Vector3f norm, Vector2f uv,
					RawDataV1.BoneBinding[] readBinds, Bone[] bones) {
				super(pos, norm, uv);
				// readBinds can be assumed to at least be size 1
				this.binds = new ArrayList<>();
				float strengthSummed = 0.0F;
				for (RawDataV1.BoneBinding bind : readBinds) {
					this.binds.add(new Binding(bones[bind.boneIndex & 0xFF],
							this.vert, bind.bindingValue));
					strengthSummed += bind.bindingValue;
				}
				for (Binding bind : this.binds) {
					bind.normalize(strengthSummed);
				}
			}

			@Override
			public void render() {
				Vertex base = null;
				for (Binding bind : this.binds) {
					base = bind.addTransformed(base);
				}
				base.setUV(this.vert.uv);
				base.render();
			}
		}

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
			this.resLocation = new ResourceLocation(
					data.material.resLocationRaw);
			this.indices = data.indices;
			this.name = data.name;
		}

		public void render() {
			mc.renderEngine.bindTexture(this.resLocation);
			for (short idx : this.indices) {
				this.pointsList[idx & 0xFFFF].render();
			}
		}

		public String getName() {
			return this.name;
		}
	}

	private static class Bone {
		private static final Matrix4f identity = new Matrix4f();

		private static class ParentedBone extends Bone {
			private Bone parent;

			protected ParentedBone(Matrix4f lTp, String name, Bone parent) {
				super(lTp, name);
				if (parent == null)
					throw new IllegalArgumentException(String.format(
							"Parent of bone %s can't be null", this.name));
				this.parent = parent;
			}

			@Override
			protected Matrix4f worldToLocal(Matrix4f dst) {
				// = parentToLocal * worldToParent
				dst = this.parent.worldToLocal(dst);
				return Matrix4f.mul(parentToLocal, dst, dst);
			}

			@Override
			protected Matrix4f localToWorld(Matrix4f dst) {
				// = parentToWorld * localToParent * transform
				dst = this.parent.localToWorld(dst);
				Matrix4f.mul(dst, this.localToParent, dst);
				return Matrix4f.mul(dst, TMP_transform, dst);
			}
		}

		protected Matrix4f localToParent;
		protected Matrix4f parentToLocal;
		public final String name;

		protected Matrix4f TMP_transform = new Matrix4f();
		protected Matrix4f TMP_globalMatrix = new Matrix4f();
		protected Matrix4f TMP_globalMatrixIT = new Matrix4f();

		protected Bone(Matrix4f localMatrix, String name) {
			this.localToParent = localMatrix;
			this.parentToLocal = Matrix4f.invert(localMatrix, null);
			this.name = name;
		}

		private void resetTransform() {
			Matrix4f.load(TMP_transform, identity);
			Matrix4f.load(TMP_globalMatrix, identity);
			Matrix4f.load(TMP_globalMatrixIT, identity);
		}
		/**
		 * Sets up this bone for the following calls to
		 * {@link #getLocalToWorld()}, {@link #getTransformGlobal()} and
		 * {@link #getTransformITGlobal()}.
		 *
		 * @param anim
		 *            the animation being executed
		 * @param frame
		 *            the frame in the animation
		 * @param subFrame
		 *            the subframe
		 */
		public void setTransformation(IAnimation anim, float frame) {
			if (anim == null) {
				resetTransform();
				return;
			}
			BoneTransformation btr = anim.getCurrentTransformation(this.name,
					frame);
			if (btr == null) {
				resetTransform();
				return;
			}
			Matrix4f.load(btr.asMatrix(), TMP_transform);
			Matrix4f worldToLocal = this.worldToLocal(null);
			Matrix4f localToWorld = this.localToWorld(null);
			Matrix4f.mul(localToWorld, worldToLocal, this.TMP_globalMatrix);
			Matrix4f.load(TMP_globalMatrix, TMP_globalMatrixIT);
			this.TMP_globalMatrixIT.invert().transpose();
		}
		/**
		 * Gets a a matrix transformation from world to local coordinates
		 *
		 * @param dst
		 *            the matrix to place the result in
		 * @return
		 */
		protected Matrix4f worldToLocal(Matrix4f dst) {
			if (dst == null)
				dst = new Matrix4f();
			return dst.load(this.parentToLocal);
		}
		/**
		 * Gets a matrix that transforms from local to world respecting the
		 * current transformation.
		 *
		 * @param dst
		 *            the matrix to place the result in
		 * @return a new matrix if dst is <code>null</code>
		 */
		protected Matrix4f localToWorld(Matrix4f dst) {
			return Matrix4f.mul(this.localToParent, TMP_transform, dst);
		}
		/**
		 * Gets the transform previously applied with
		 * {@link #setTransformation(IAnimation, int, float)} already
		 * transformed to the global matrix.
		 *
		 * @return world to transformed world
		 */
		public Matrix4f getTransformGlobal() {
			return TMP_globalMatrix;
		}
		/**
		 * Gets the inverse transposed transform previously applied with
		 * {@link #setTransformation(IAnimation, int, float)} already
		 * transformed to the global matrix. This matrix is suitable for usage
		 * with normals
		 *
		 * @return world to transformed world
		 */
		public Matrix4f getTransformGlobalIT() {
			return TMP_globalMatrixIT;
		}
		/**
		 * Returns a new bone from the data given. If the bone has a parent it
		 * is fetched from allBones thus it is expected that allBones is
		 * breadth-first (meaning that the parent is already handled).
		 *
		 * @param data
		 *            the raw data
		 * @param allBones
		 *            all bones, including the parent
		 * @return the new bone
		 */
		public static Bone fromData(RawDataV1.Bone data, Bone[] allBones) {
			Matrix4f localToParent = Utils.fromRotTrans(data.rotation,
					data.offset, 1.0F);
			if (data.parent != -1)
				return new ParentedBone(localToParent, data.name,
						allBones[data.parent & 0xFF]);
			return new Bone(localToParent, data.name);
		}
	}
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

	@Override
	public void renderAll(IAnimation currAnimation, float frame) {
		setupBones(currAnimation, frame);
		tess.startDrawing(GL_TRIANGLES);
		for (Part part : this.parts) {
			part.render();
		}
		tess.draw();
	}

	@Override
	public void renderFiltered(Predicate<String> filter,
			IAnimation currAnimation, float frame) {
		setupBones(currAnimation, frame);
		tess.startDrawing(GL_TRIANGLES);
		for (Part part : this.parts) {
			if (filter.apply(part.getName()))
				part.render();
		}
		tess.draw();
	}
}
