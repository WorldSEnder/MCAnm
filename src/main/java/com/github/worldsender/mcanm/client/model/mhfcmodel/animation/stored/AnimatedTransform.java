package com.github.worldsender.mcanm.client.model.mhfcmodel.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraftforge.client.model.ModelFormatException;

import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import com.github.worldsender.mcanm.client.model.mhfcmodel.animation.IAnimation.BoneTransformation;
/**
 * Represents the animation of one bone from the {@link StoredAnimation}.
 *
 * @author WorldSEnder
 */
public class AnimatedTransform {
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
	public AnimatedTransform(DataInputStream dis) throws ModelFormatException,
			IOException {
		this.loc_x = new AnimatedValue(0.0F, dis);
		this.loc_y = new AnimatedValue(0.0F, dis);
		this.loc_z = new AnimatedValue(0.0F, dis);
		this.quat_x = new AnimatedValue(0.0F, dis);
		this.quat_y = new AnimatedValue(0.0F, dis);
		this.quat_z = new AnimatedValue(0.0F, dis);
		this.quat_w = new AnimatedValue(1.0F, dis);
		this.scale_x = new AnimatedValue(1.0F, dis);
		this.scale_y = new AnimatedValue(1.0F, dis);
		this.scale_z = new AnimatedValue(1.0F, dis);
	}
	/**
	 * Gets the transformation of the bone at a specific point in the animation.
	 * This method interpolates between the nearest two key-frames using the
	 * correct interpolation mode.
	 *
	 * @param frame
	 * @param subFrame
	 * @return
	 */
	public BoneTransformation getTransformAt(float frame) {
		Vector3f translation = new Vector3f(loc_x.getValueAt(frame),
				loc_y.getValueAt(frame), loc_z.getValueAt(frame));
		Quaternion quaternion = new Quaternion(quat_x.getValueAt(frame),
				quat_y.getValueAt(frame), quat_z.getValueAt(frame),
				quat_w.getValueAt(frame));
		Vector3f scale = new Vector3f(scale_x.getValueAt(frame),
				scale_y.getValueAt(frame), scale_z.getValueAt(frame));
		return new BoneTransformation(translation, quaternion, scale);
	}
}
