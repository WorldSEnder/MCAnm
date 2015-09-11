package com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts;

import java.util.Objects;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector4f;

import com.github.worldsender.mcanm.client.model.mcanmmodel.Utils;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation.BoneTransformation;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawDataV1;

public class Bone {
	private static final Matrix4f identity = new Matrix4f();

	private static class ParentedBone extends Bone {
		private Bone parent;

		protected ParentedBone(Matrix4f localToParent, String name, Bone parent) {
			super(localToParent, name);
			this.parent = Objects
					.requireNonNull(parent, String.format(
							"Parent of bone %s can't be null", this.name));
		}

		@Override
		protected Matrix4f globalToLocal(Matrix4f src, Matrix4f dest) {
			// local <- parent <- global
			dest = this.parent.globalToLocal(src, dest);
			return super.globalToLocal(dest, dest);
		}

		@Override
		protected Matrix4f localToGlobal(Matrix4f src, Matrix4f dest) {
			// world <- parent <- transformedLocal <- local
			dest = super.localToGlobal(src, dest);
			return this.parent.localToGlobal(dest, dest);
		}
	}

	protected final Matrix4f localToParent;
	protected final Matrix4f parentToLocal;
	public final String name;

	protected Matrix4f transformed = new Matrix4f(); // For childs
	protected Matrix4f transformedGlobalToGlobal = new Matrix4f(); // vertices
	protected Matrix4f transformedGlobalToGlobalIT = new Matrix4f(); // normals

	protected Bone(Matrix4f localMatrix, String name) {
		this.localToParent = localMatrix;
		this.parentToLocal = Matrix4f.invert(localMatrix, null);
		this.name = name;
	}
	/**
	 * Debug!
	 *
	 * @return the head of the bone
	 */
	public Vector4f getHead() {
		Matrix4f localToGlobal = this.localToGlobal(identity, null);
		Vector4f head = new Vector4f();
		head.y = 1.0f;
		head.w = 1.0f;
		Matrix4f.transform(localToGlobal, head, head);
		return head;
	}

	public Vector4f getTail() {
		Matrix4f localToGlobal = this.localToGlobal(identity, null);
		Vector4f head = new Vector4f();
		head.w = 1.0f;
		Matrix4f.transform(localToGlobal, head, head);
		return head;
	}

	private void resetTransform() {
		Matrix4f.load(identity, transformed);
		Matrix4f.load(identity, transformedGlobalToGlobal);
		Matrix4f.load(identity, transformedGlobalToGlobalIT);
	}
	/**
	 * Sets up this bone for the following calls to {@link #getLocalToWorld()},
	 * {@link #getTransformGlobal()} and {@link #getTransformITGlobal()}.
	 *
	 * @param anim
	 *            the animation being executed
	 * @param frame
	 *            the frame in the animation
	 * @param subFrame
	 *            the subframe
	 */
	public void setTransformation(IAnimation anim, float frame) {
		BoneTransformation btr = anim
				.getCurrentTransformation(this.name, frame);
		if (btr.equals(identity)) {
			resetTransform();
			return;
		}
		Matrix4f worldToLocal = this.globalToLocal(identity, null);
		Matrix4f.load(btr.asMatrix(), transformed);
		Matrix4f transformedLocalToWorld = this.localToGlobal(identity, null);
		Matrix4f.mul(transformedLocalToWorld, worldToLocal,
				transformedGlobalToGlobal);
		Matrix4f.load(transformedGlobalToGlobal, transformedGlobalToGlobalIT);
		transformedGlobalToGlobalIT.invert().transpose();
	}
	/**
	 * Transforms the source matrix into local space and stores the resulting
	 * matrix in destination.<br>
	 * If dest is <code>null</code> a new matrix with the result is returned
	 *
	 * @param src
	 *            the matrix to transform
	 * @param dest
	 *            the matrix to store the result in
	 * @return dest if dest is not <code>null</code>, a new matrix otherwise
	 */
	protected Matrix4f globalToLocal(Matrix4f src, Matrix4f dest) {
		return Matrix4f.mul(parentToLocal, src, dest);
	}
	/**
	 * Transforms the source matrix from local into global space and stores the
	 * resulting matrix in destination.<br>
	 * This method is - contrary to {@link #globalToLocal(Matrix4f, Matrix4f)}
	 * sensitive to this bone's current transformation.<br>
	 * If dest is <code>null</code> a new matrix with the result is returned.
	 *
	 * @param src
	 *            the matrix to transform
	 * @param dest
	 *            the matrix to store the result in
	 * @return dest if dest is not <code>null</code>, a new matrix otherwise
	 */
	protected Matrix4f localToGlobal(Matrix4f src, Matrix4f dest) {
		dest = Matrix4f.mul(transformed, src, dest);
		return Matrix4f.mul(localToParent, dest, dest);
	}
	/**
	 * Gets the transform previously applied with
	 * {@link #setTransformation(IAnimation, int, float)} already transformed to
	 * the global matrix.
	 *
	 * @return world to transformed world
	 */
	public Matrix4f getTransformGlobal() {
		return transformedGlobalToGlobal;
	}
	/**
	 * Gets the inverse transposed transform previously applied with
	 * {@link #setTransformation(IAnimation, int, float)} already transformed to
	 * the global matrix. This matrix is suitable for usage with normals.
	 *
	 * @return world to transformed world
	 */
	public Matrix4f getTransformGlobalIT() {
		return transformedGlobalToGlobalIT;
	}
	/**
	 * Returns a new bone from the data given. If the bone has a parent it is
	 * fetched from allBones thus it is expected that allBones is breadth-first
	 * (meaning that the parent is already handled).
	 *
	 * @param data
	 *            the raw data
	 * @param allBones
	 *            all bones, including the parent
	 * @return the new bone
	 */
	public static Bone fromData(RawDataV1.Bone data, Bone[] allBones) {
		Matrix4f localToParent = Utils.fromRTS(data.rotation, data.offset,
				1.0F);
		if (data.parent != (byte) 0xFF)
			return new ParentedBone(localToParent, data.name,
					allBones[data.parent & 0xFF]);
		return new Bone(localToParent, data.name);
	}
}