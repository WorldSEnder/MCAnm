package com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts;

import java.util.Objects;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

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
			this.parent = Objects.requireNonNull(parent, String.format("Parent of bone %s can't be null", this.name));
		}

		@Override
		protected void globalToLocal(Matrix4f src) {
			// local <- parent <- global
			this.parent.globalToLocal(src);
			super.globalToLocal(src);
		}

		@Override
		protected void localToGlobal(Matrix4f src) {
			// world <- parent <- transformedLocal <- local
			super.localToGlobal(src);
			this.parent.localToGlobal(src);
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
		this.name = name;
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
		BoneTransformation btr = anim.getCurrentTransformation(this.name, frame).or(BoneTransformation.identity);
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

	/**
	 * Transforms the normal given by the transformation currently acted out by this bone.
	 *
	 * @param position
	 *            the position to transform
	 */
	public void transform(Vector3f normal) {
		transformedGlobalToGlobalIT.transform(normal);
	}

	/**
	 * Returns a new bone from the data given. If the bone has a parent it is fetched from allBones thus it is expected
	 * that allBones is breadth-first (meaning that the parent is already handled).
	 *
	 * @param data
	 *            the raw data
	 * @param allBones
	 *            all bones, including the parent
	 * @return the new bone
	 */
	public static Bone fromData(RawDataV1.Bone data, Bone[] allBones) {
		Matrix4f localToParent = Utils.fromRTS(data.rotation, data.offset, 1.0F);
		if (data.parent != (byte) 0xFF)
			return new ParentedBone(localToParent, data.name, allBones[data.parent & 0xFF]);
		return new Bone(localToParent, data.name);
	}
}
