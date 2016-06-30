package com.github.worldsender.mcanm.common.animation;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.common.Utils;

/**
 * An animation to transform the model.
 *
 * @author WorldSEnder
 *
 */
public interface IAnimation {
	/**
	 * Describes a BoneTransformation, including rotation, translation and scaling. This class is opaque i.e. all
	 * getters and setters don't make defensive copies and states can be manipulated from the outside.
	 *
	 * @author WorldSEnder
	 *
	 */
	public static class BoneTransformation {
		public static final BoneTransformation identity = new BoneTransformation();

		private static Vector3f identityScale() {
			return new Vector3f(1.0F, 1.0F, 1.0F);
		}

		public final Matrix4f matrix;

		public BoneTransformation() {
			this(null, null, null);
		}

		public BoneTransformation(Vector3f translation, Quat4f quat) {
			this(translation, quat, identityScale());
		}

		public BoneTransformation(Vector3f translation, Quat4f quat, Vector3f scale) {
			if (quat == null)
				quat = new Quat4f();
			if (translation == null)
				translation = new Vector3f();
			if (scale == null)
				scale = identityScale();
			this.matrix = Utils.fromRTS(quat, translation, scale, new Matrix4f());
		}

		public Matrix4f getMatrix() {
			return matrix;
		}
	}

	/**
	 * Stores the bone's current {@link BoneTransformation} in transform(identified by name). <br>
	 * If the requested bone is not known to the animation transform is left untouched and this return
	 * <code>false</code>. Otherwise transform is set to the bone's current transform state.
	 *
	 * @param bone
	 *            the name of the bone the matrix is requested
	 * @param frame
	 *            the current frame in the animation
	 * @param transform
	 *            the transform to set
	 * @return if a transformation has been set
	 */
	public boolean storeCurrentTransformation(String bone, float frame, BoneTransformation transform);
}
