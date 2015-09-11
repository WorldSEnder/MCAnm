package com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1.TesselationPoint;

public class Point {
	private static class BoundPoint extends Point {
		private static class Binding {
			// Used as a buffer, doesn't requite to always create new
			// temporaries. Prevents parallelization, though
			private static Vector4f posBuff = new Vector4f();
			private static Vector4f normBuff = new Vector4f();

			private Bone bone;
			private float strength;
			public Binding(Bone bone, float strenght) {
				this.bone = bone;
				this.strength = strenght;
			}
			/**
			 * Computes the transformed and weighted position of the given
			 * vertex. Adds that to the target vertex.
			 *
			 * @param base
			 *            the vertex to transform
			 * @param trgt
			 *            the vertex to add to. If null is given, it is just
			 *            assigned to
			 * @return the final vertex, a new vertex if <code>null</code> was
			 *         given
			 */
			public Vertex addTransformed(Vertex base, Vertex trgt) {
				Objects.requireNonNull(base);
				Vector4f pos = base.getPosition();
				Vector3f norm = base.getNormal();

				posBuff.set(pos.x, pos.y, pos.z, 1.0F);
				normBuff.set(norm.x, norm.y, norm.z, 0.0F);
				// Transform matrix
				Matrix4f globalTransform = this.bone.getTransformGlobal();
				Matrix4f globalTransformIT = this.bone.getTransformGlobalIT();
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
				if (trgt == null) {
					trgt = new Vertex(position, normal, null);
				} else {
					trgt.offset(position);
					trgt.addNormal(normal);
				}
				return trgt;
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
				if (bind.bindingValue <= 0.0f)
					continue;
				this.binds.add(new Binding(bones[bind.boneIndex & 0xFF],
						bind.bindingValue));
				strengthSummed += bind.bindingValue;
			}
			for (Binding bind : this.binds) {
				bind.normalize(strengthSummed);
			}
		}

		@Override
		public void render() {
			Vertex base = this.vert;
			Vertex transformed = null;
			for (Binding bind : this.binds) {
				transformed = bind.addTransformed(base, transformed);
			}
			transformed.setUV(this.vert.getUV());
			transformed.render();
		}
	}

	protected final Vertex vert;

	protected Point(Vector3f pos, Vector3f norm, Vector2f uv) {
		this.vert = new Vertex(new Vector4f(pos.x, pos.y, pos.z, 1F), norm, uv);
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
	 * Constructs a bone from the {@link TesselationPoint} given. This is
	 * implemented in the factory style to efficiently handle bones without
	 * Bindings
	 *
	 * @param data
	 *            the point to construct from
	 * @return the constructed point
	 */
	public static Point from(RawDataV1.TesselationPoint data, Bone[] bones) {
		boolean isBound = data.boneBindings.length > 0;
		if (isBound)
			return new BoundPoint(data.coords, data.normal, data.texCoords,
					data.boneBindings, bones);
		return new Point(data.coords, data.normal, data.texCoords);
	}
}