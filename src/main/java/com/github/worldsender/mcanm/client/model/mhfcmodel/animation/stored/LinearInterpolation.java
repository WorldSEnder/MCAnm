package com.github.worldsender.mcanm.client.model.mhfcmodel.animation.stored;

import java.io.DataInputStream;

import org.lwjgl.util.vector.Vector2f;
/**
 * Describes an interpolation between two values at two frames. Let those pairs
 * be (f<SUB>1</SUB>, v<SUB>1</SUB>) and (f<SUB>2</SUB>, v<SUB>2</SUB>). The
 * value returned will be a constant interpolation between the two frames.<br>
 * Set <code>t = (frame - f<SUB>1</SUB>)/(f<SUB>2</SUB> - f<SUB>1</SUB>)</code>.
 * The returned value will be
 * <code>(1 - t) * v<SUB>1</SUB> + t * v<SUB>2</SUB></code>.
 *
 * @author WorldSEnder
 *
 */
public class LinearInterpolation extends Spline {
	public static final IInterpolationSplineFactory factory = new IInterpolationSplineFactory() {
		@Override
		public Spline newSpline(Vector2f left, Vector2f right,
				DataInputStream additionalData) {
			return new LinearInterpolation(left, right);
		}
	};

	private Vector2f left;
	private Vector2f right;

	private LinearInterpolation(Vector2f left, Vector2f right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public boolean isInRange(float frame) {
		return frame >= this.left.x && frame <= this.right.x;
	}

	@Override
	public float getValueAt(float frame) {
		float split = this.right.x - this.left.x;
		return (this.right.x - frame) / (split) * this.left.y + //
				(frame - this.left.x) / (split) * this.right.y;
	}
}
