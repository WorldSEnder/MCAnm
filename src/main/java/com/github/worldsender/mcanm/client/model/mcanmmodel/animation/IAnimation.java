package com.github.worldsender.mcanm.client.model.mcanmmodel.animation;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import com.github.worldsender.mcanm.client.model.mcanmmodel.Utils;
/**
 * An animation to transform the model.
 *
 * @author WorldSEnder
 *
 */
public interface IAnimation {
	/**
	 * Describes a BoneTransformation, including rotation, translation and
	 * scaling.
	 *
	 * @author WorldSEnder
	 *
	 */
	public static class BoneTransformation {
		private static Vector3f identityScale() {
			return new Vector3f(1.0F, 1.0F, 1.0F);
		}

		public static final BoneTransformation identity = new BoneTransformation();

		private Quaternion rotationQuat;
		private Vector3f translation;
		private Vector3f scale;

		public BoneTransformation() {
			this(new Vector3f(), new Quaternion(), identityScale());
		}

		public BoneTransformation(Vector3f translation, Quaternion quat) {
			this(translation, quat, identityScale());
		}

		public BoneTransformation(Vector3f translation, Quaternion quat,
				Vector3f scale) {
			if (quat == null)
				quat = new Quaternion();
			if (translation == null)
				translation = new Vector3f();
			if (scale == null)
				scale = identityScale();
			this.rotationQuat = quat;
			this.translation = translation;
			this.scale = scale;
		}

		public Matrix4f asMatrix() {
			Matrix4f mat = Utils.fromRTS(this.rotationQuat,
					this.translation, 1.0F);
			mat.scale(scale);
			return mat;
		}
	}
	/**
	 * Returns the bone's current {@link BoneTransformation} (identified by
	 * name). <br>
	 * If the requested bone is not known to the animation it may throw or
	 * simply return {@link BoneTransformation#identity}. It may never return
	 * <code>null</code>.
	 *
	 * @param bone
	 *            the name of the bone the matrix is requested
	 * @param frame
	 *            the current frame in the animation
	 * @return the actual, present state of the requested bone, never
	 *         <code>null</code>
	 */
	public BoneTransformation getCurrentTransformation(String bone, float frame);
}
