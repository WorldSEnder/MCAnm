package com.github.worldsender.mcanm.client.model.mcanmmodel.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.github.worldsender.mcanm.client.exceptions.ModelFormatException;

public class AnimatedValue {
	/** The splines making up the full curve. */
	private List<Spline> splines;
	/**
	 * Reads the animated value from the {@link DataInputStream} given using
	 * keyframes. That are time, value pairs. Each pair forms a keyframe.
	 * Between successive two keyframes is a spline that can be the graph of any
	 * real-value-function.<br>
	 *
	 * @param defaultValue
	 *            the value when no keyframe is found
	 * @param is
	 */
	private float defaultValue;
	public AnimatedValue(float defaultValue, DataInputStream dis)
			throws ModelFormatException, IOException {
		this.defaultValue = defaultValue;
		splines = new ArrayList<>();
		int nbrFrames = dis.readUnsignedShort();
		if (nbrFrames == 0) {
			Vector2f point = new Vector2f(0, defaultValue);
			splines.add(Spline.easeIn(Spline.EASE_IN_CONST, point, dis));
			splines.add(Spline.easeOut(Spline.EASE_OUT_CONST, point, dis));
		} else {
			Vector2f left = null;
			Vector2f right = Spline.readPoint(dis);
			byte easeIn = dis.readByte();
			splines.add(Spline.easeIn(easeIn, right, dis));
			for (int i = 1; i < nbrFrames; ++i) {
				left = right;
				right = Spline.readPoint(dis);
				byte interpolation = dis.readByte();
				splines.add(Spline.interpolating(interpolation, left, right,
						dis));
			}
			byte easeOut = dis.readByte();
			splines.add(Spline.easeOut(easeOut, right, dis));
		}
	}
	/**
	 * Gets the y-value that corresponds to the time (x-value) given.
	 *
	 * @param time
	 *            the time/x-value on the curve
	 * @return the corresponding y-value
	 */
	public float getValueAt(float time) {
		for (Spline spline : this.splines) {
			if (spline.isInRange(time))
				return spline.getValueAt(time);
		}
		// Should not happen....
		return this.defaultValue;
	}
}
