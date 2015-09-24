package com.github.worldsender.mcanm.client.model.mcanmmodel.animation;

import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3i;
import net.minecraftforge.client.model.ITransformation;

import org.apache.commons.lang3.tuple.Triple;

import com.github.worldsender.mcanm.client.model.mcanmmodel.Utils;
import com.google.common.base.Optional;
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
	public static class BoneTransformation implements ITransformation {
		public static final BoneTransformation identity = new BoneTransformation();

		private static Vector3f identityScale() {
			return new Vector3f(1.0F, 1.0F, 1.0F);
		}

		private final Matrix4f matrix;
		private final Vector3f t, s;
		private final Quat4f r;

		public BoneTransformation() {
			this(null, null, null);
		}

		public BoneTransformation(Vector3f translation, Quat4f quat) {
			this(translation, quat, identityScale());
		}

		public BoneTransformation(Vector3f translation, Quat4f quat,
				Vector3f scale) {
			if (quat == null)
				quat = new Quat4f();
			if (translation == null)
				translation = new Vector3f();
			if (scale == null)
				scale = identityScale();
			this.t = translation;
			this.r = quat;
			this.s = scale;
			this.matrix = Utils.fromRTS(quat, translation, scale);
		}

		@Override
		public Matrix4f getMatrix() {
			return (Matrix4f) matrix.clone();
		}

		@Override
		public EnumFacing rotate(EnumFacing facing) {
			Vec3i dir = facing.getDirectionVec();
			Vector4f vec = new Vector4f(dir.getX(), dir.getY(), dir.getZ(), 0);
			matrix.transform(vec);
			return EnumFacing.func_176737_a(vec.x, vec.y, vec.z);
		}

		public Triple<Vector3f, Quat4f, Vector3f> asTRS() {
			return Triple.of(t, r, s);
		}

		@Override
		public int rotate(EnumFacing facing, int vertexIndex) {
			// @see TRSRTransformation#rotate(EnumFacing, int)
			return vertexIndex;
		}
	}

/**
	 * Returns the bone's current {@link BoneTransformation} (identified by
	 * name). <br>
	 * If the requested bone is not known to the animation it should return
	 * an {@link Optional#absent()). This means (for the model) that the bone
	 * is placed at it's identity position and not transformed at all, but
	 * it also gives other animations the chance to supply their Transformation
	 *
	 * @param bone
	 *            the name of the bone the matrix is requested
	 * @param frame
	 *            the current frame in the animation
	 * @return the present position of the bone.
	 */
	public Optional<BoneTransformation> getCurrentTransformation(String bone,
			float frame);
}
