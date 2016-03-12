package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.util.vector.Vector2f;

import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

public class AnimatedValue {
	public static class AnimatedValueBuilder {
		private AnimatedValue value = null;

		private void checkAvailable() {
			value = new AnimatedValue(0.0f);
		}

		public AnimatedValueBuilder addSpline(Spline spline) {
			value.splines.add(spline);
			return this;
		}

		public AnimatedValueBuilder fromStream(DataInputStream dis) throws IOException, ModelFormatException {
			if (value == null) {
				throw new IllegalStateException("Can only red from stream with after a default value has been set");
			}
			int nbrFrames = dis.readUnsignedShort();
			if (nbrFrames == 0) {
				Vector2f point = new Vector2f(0, value.defaultValue);
				value.splines.add(Spline.easeIn(Spline.EASE_IN_CONST, point, dis));
				value.splines.add(Spline.easeOut(Spline.EASE_OUT_CONST, point, dis));
			} else {
				Vector2f left = null;
				Vector2f right = Spline.readPoint(dis);
				byte easeIn = dis.readByte();
				value.splines.add(Spline.easeIn(easeIn, right, dis));
				for (int i = 1; i < nbrFrames; ++i) {
					left = right;
					right = Spline.readPoint(dis);
					byte interpolation = dis.readByte();
					value.splines.add(Spline.interpolating(interpolation, left, right, dis));
				}
				byte easeOut = dis.readByte();
				value.splines.add(Spline.easeOut(easeOut, right, dis));
			}
			return this;
		}

		public AnimatedValueBuilder setDefaultValue(float defaultValue) {
			checkAvailable();
			value.defaultValue = defaultValue;
			return this;
		}

		public AnimatedValue buildAndReset() {
			AnimatedValue ret = value;
			value = null;
			ret.splines = Collections.unmodifiableList(ret.splines);
			return ret;
		}
	}

	public static final AnimatedValue CONSTANT_ZERO = new AnimatedValue(0f);
	public static final AnimatedValue CONSTANT_ONE = new AnimatedValue(1f);

	/** The splines making up the full curve. */
	private List<Spline> splines;
	/**
	 * Reads the animated value from the {@link DataInputStream} given using keyframes. That are time, value pairs. Each
	 * pair forms a keyframe. Between successive two keyframes is a spline that can be the graph of any
	 * real-value-function.<br>
	 *
	 * @param defaultValue
	 *            the value when no keyframe is found
	 * @param is
	 */
	private float defaultValue;

	private AnimatedValue(float defaultValue) {
		this.splines = new ArrayList<>();
		this.defaultValue = defaultValue;
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
