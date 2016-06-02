package com.github.worldsender.mcanm.common.skeleton.parts;

import java.util.Objects;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.animation.IAnimation.BoneTransformation;
import com.github.worldsender.mcanm.common.skeleton.IBone;

public class Bone implements IBone {
	private static final Matrix4f identity = new Matrix4f();

	static {
		identity.setIdentity();
	}

	public static class BoneBuilder {
		private Quat4f rotation;
		private Vector3f offset;
		private IBone parent;
		private String name;

		public BoneBuilder() {
			this.rotation = new Quat4f();
			this.offset = new Vector3f();
			reset();
		}

		public BoneBuilder(String name) {
			this();
			setName(name);
		}

		public BoneBuilder setName(String name) {
			this.name = Objects.requireNonNull(name);
			return this;
		}

		public BoneBuilder setOffset(Vector3f offset) {
			this.offset.set(offset);
			return this;
		}

		public BoneBuilder setRotation(Quat4f rotation) {
			this.rotation.set(rotation);
			return this;
		}

		public BoneBuilder setParent(IBone parent) {
			this.parent = parent;
			return this;
		}

		private void reset() {
			this.parent = null;
			this.name = null;
			this.offset.set(0, 0, 0);
			this.rotation.set(0, 0, 0, 0);
		}

		public Bone build() {
			Matrix4f localToParent = Utils.fromRTS(rotation, offset, 1.0F);
			if (parent != null)
				return new ParentedBone(localToParent, name, parent);
			return new Bone(localToParent, name);
		}
	}

	private static class ParentedBone extends Bone {
		private IBone parent;

		protected ParentedBone(Matrix4f localToParent, String name, IBone parent) {
			super(localToParent, name);
			this.parent = Objects.requireNonNull(parent, String.format("Parent of bone %s can't be null", this.name));
		}

		@Override
		protected void globalToLocal(Matrix4f src) {
			// local <- parent <- global
			this.parent.transformToLocal(src);
			super.globalToLocal(src);
		}

		@Override
		protected void localToGlobal(Matrix4f src) {
			// world <- parent <- transformedLocal <- local
			super.localToGlobal(src);
			this.parent.transformFromLocal(src);
		}
	}

	protected final Matrix4f localToParent;
	protected final Matrix4f parentToLocal;
	public final String name;

	protected Matrix4f transformed = new Matrix4f(); // For childs
	protected Matrix4f transformedGlobalToGlobal = new Matrix4f(); // vertices
	protected Matrix4f transformedGlobalToGlobalIT = new Matrix4f(); // normals

	protected Bone(Matrix4f localMatrix, String name) {
		this.localToParent = new Matrix4f(localMatrix);
		this.parentToLocal = new Matrix4f(localMatrix);
		this.parentToLocal.invert();
		this.name = Objects.requireNonNull(name);
	}

	/**
	 * Debug!
	 *
	 * @return the head of the bone
	 */
	public Vector4f getHead() {
		Matrix4f localToGlobal = new Matrix4f(identity);
		this.localToGlobal(localToGlobal);

		Vector4f head = new Vector4f();
		head.y = 1.0f;
		head.w = 1.0f;
		localToGlobal.transform(head);
		return head;
	}

	/**
	 * Debug!
	 *
	 * @return the tail of the bone
	 */
	public Vector4f getTail() {
		Matrix4f localToGlobal = new Matrix4f(identity);
		this.localToGlobal(localToGlobal);

		Vector4f tail = new Vector4f();
		tail.w = 1.0f;
		localToGlobal.transform(tail);
		return tail;
	}

	public void resetTransform() {
		transformed.set(identity);
		transformedGlobalToGlobal.set(identity);
		transformedGlobalToGlobalIT.set(identity);
	}

	/**
	 * Sets up this bone for the following calls to {@link #getLocalToWorld()}, {@link #getTransformGlobal()} and
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
		BoneTransformation btr = anim.getCurrentTransformation(this.name, frame).orElse(BoneTransformation.identity);
		transformed.set(btr.getMatrix());

		transformedGlobalToGlobal.setIdentity();
		this.globalToLocal(transformedGlobalToGlobal);
		this.localToGlobal(transformedGlobalToGlobal);

		transformedGlobalToGlobalIT.set(transformedGlobalToGlobal);
		transformedGlobalToGlobalIT.invert();
		transformedGlobalToGlobalIT.transpose();
	}

	/**
	 * Transforms the source matrix into local space and stores the resulting matrix back in the source.<br>
	 *
	 * @param src
	 *            the matrix to transform
	 */
	protected void globalToLocal(Matrix4f src) {
		src.mul(parentToLocal, src);
	}

	/**
	 * Transforms the source matrix from local into global space and stores the resulting matrix back in the source.<br>
	 * This method is - contrary to {@link #globalToLocal(Matrix4f, Matrix4f)} sensitive to this bone's current
	 * transformation.<br>
	 *
	 * @param src
	 *            the matrix to transform
	 */
	protected void localToGlobal(Matrix4f src) {
		src.mul(transformed, src);
		src.mul(localToParent, src);
	}

	/**
	 * Transforms the position given by the transformation currently acted out by this bone.
	 *
	 * @param position
	 *            the position to transform
	 */
	public void transform(Point4f position) {
		transformedGlobalToGlobal.transform(position);
	}

	@Override
	public void transformFromLocal(Matrix4f matrix) {
		localToGlobal(matrix);
	}

	@Override
	public void transformToLocal(Matrix4f matrix) {
		globalToLocal(matrix);
	}

	@Override
	public void transform(Matrix4f matrix) {
		transformedGlobalToGlobal.mul(matrix, matrix);
	}

	/**
	 * Transforms the normal given by the transformation currently acted out by this bone.
	 *
	 * @param position
	 *            the position to transform
	 */
	public void transformNormal(Vector3f normal) {
		transformedGlobalToGlobalIT.transform(normal);
	}
}
