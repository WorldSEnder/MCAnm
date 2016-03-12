package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import com.github.worldsender.mcanm.common.animation.IAnimation.BoneTransformation;
import com.github.worldsender.mcanm.common.animation.stored.AnimatedValue.AnimatedValueBuilder;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

/**
 * Represents the animation of one bone from the {@link StoredAnimation}.
 *
 * @author WorldSEnder
 */
public class AnimatedTransform {
	public static class AnimatedTransformBuilder {
		public static AnimatedValueBuilder builder = new AnimatedValueBuilder();

		private AnimatedTransform value = null;

		private void checkAvailable() {
			value = new AnimatedTransform();
		}

		public AnimatedTransformBuilder fromStream(DataInputStream dis) throws IOException, ModelFormatException {
			checkAvailable();
			value.loc_x = builder.setDefaultValue(0.0f).fromStream(dis).buildAndReset();
			value.loc_y = builder.setDefaultValue(0.0f).fromStream(dis).buildAndReset();
			value.loc_z = builder.setDefaultValue(0.0f).fromStream(dis).buildAndReset();
			value.quat_x = builder.setDefaultValue(0.0f).fromStream(dis).buildAndReset();
			value.quat_y = builder.setDefaultValue(0.0f).fromStream(dis).buildAndReset();
			value.quat_z = builder.setDefaultValue(0.0f).fromStream(dis).buildAndReset();
			value.quat_w = builder.setDefaultValue(1.0f).fromStream(dis).buildAndReset();
			value.scale_x = builder.setDefaultValue(1.0f).fromStream(dis).buildAndReset();
			value.scale_y = builder.setDefaultValue(1.0f).fromStream(dis).buildAndReset();
			value.scale_z = builder.setDefaultValue(1.0f).fromStream(dis).buildAndReset();
			return this;
		}

		public AnimatedTransform buildAndReset() {
			AnimatedTransform ret = value;
			return ret;
		}
	}

	private AnimatedValue loc_x;
	private AnimatedValue loc_y;
	private AnimatedValue loc_z;
	private AnimatedValue quat_x;
	private AnimatedValue quat_y;
	private AnimatedValue quat_z;
	private AnimatedValue quat_w;
	private AnimatedValue scale_x;
	private AnimatedValue scale_y;
	private AnimatedValue scale_z;

	/**
	 * Reads a {@link AnimatedTransform} from the {@link DataInputStream} given.
	 *
	 * @param dis
	 */
	private AnimatedTransform() {
		loc_x = loc_y = loc_z = quat_x = quat_y = quat_z = AnimatedValue.CONSTANT_ZERO;
		quat_w = scale_x = scale_y = scale_z = AnimatedValue.CONSTANT_ONE;
	}

	/**
	 * Gets the transformation of the bone at a specific point in the animation. This method interpolates between the
	 * nearest two key-frames using the correct interpolation mode.
	 *
	 * @param frame
	 * @param subFrame
	 * @return
	 */
	public BoneTransformation getTransformAt(float frame) {
		Vector3f translation = new Vector3f(loc_x.getValueAt(frame), loc_y.getValueAt(frame), loc_z.getValueAt(frame));
		Quat4f quaternion = new Quat4f(
				quat_x.getValueAt(frame),
				quat_y.getValueAt(frame),
				quat_z.getValueAt(frame),
				quat_w.getValueAt(frame));
		quaternion.normalize();
		Vector3f scale = new Vector3f(scale_x.getValueAt(frame), scale_y.getValueAt(frame), scale_z.getValueAt(frame));
		return new BoneTransformation(translation, quaternion, scale);
	}
}
