package com.github.worldsender.mcanm.client.model.mcanmmodel.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.util.vector.Vector2f;

public class BSplineInterpolation extends Spline {
	public static final IInterpolationSplineFactory factory = new IInterpolationSplineFactory() {
		@Override
		public Spline newSpline(Vector2f left, Vector2f right, DataInputStream additionalData) throws IOException {
			Vector2f leftHandle = Spline.readPoint(additionalData);
			Vector2f rightHandle = Spline.readPoint(additionalData);
			return new BSplineInterpolation(left, leftHandle, rightHandle, right);
		}
	};

	private Vector2f left;
	private Vector2f leftHandle;
	private Vector2f rightHandle;
	private Vector2f right;

	public BSplineInterpolation(Vector2f left, Vector2f leftHandle, Vector2f rightHandle, Vector2f right) {
		this.left = left;
		this.leftHandle = leftHandle;
		this.rightHandle = rightHandle;
		this.right = right;
	}

	@Override
	public boolean isInRange(float frame) {
		return frame >= this.left.x && frame <= this.right.x;
	}

	@Override
	public float getValueAt(float frame) {
		if (frame == this.left.x)
			return this.left.y;
		if (frame == this.right.x)
			return this.right.y;
		double t = findZero(this.left.x, this.leftHandle.x, this.rightHandle.x, this.right.x, frame);
		return calcValue(this.left.y, this.leftHandle.y, this.rightHandle.y, this.right.y, t);
	}

	/**
	 * Finds a <code>t</code> for the input points in the range ]0..1[.<br>
	 * This <code>t</code> is suitable to be used for the exact value calcuation.<br>
	 * For this to work
	 * <ul>
	 * <li>x<SUB>1</SUB> <= x<SUB>2</SUB> <= x<SUB>4</SUB> and
	 * <li>x<SUB>1</SUB> <= x<SUB>3</SUB> <= x<SUB>4</SUB> and
	 * <li>x<SUB>1</SUB> <= frame <= x<SUB>4</SUB>.
	 * </ul>
	 *
	 * @param x1
	 *            x value of the left frame
	 * @param x2
	 *            x value of the right handle of the left frame
	 * @param x3
	 *            x value of the left handle of the right frame
	 * @param x4
	 *            x value of the right frame
	 * @param x
	 *            the frame to evaluate
	 * @return
	 */
	public static double findZero(float x1, float x2, float x3, float x4, float x) {
		double result, b, c, p;

		double c0 = x1 - x;
		double c1 = 3.0f * (x2 - x1);
		double c2 = 3.0f * (x1 - 2.0f * x2 + x3);
		double c3 = x4 - x1 + 3.0f * (x2 - x3);

		if (c3 != 0.0) { // degree 3 polynom
			double a, d, t, q;
			a = c2 / c3;
			b = c1 / c3;
			c = c0 / c3;
			a = a / 3;

			p = b / 3 - a * a;
			q = (2 * a * a * a - a * b + c) / 2;
			d = q * q + p * p * p;

			if (d > 0.0) {
				// One real solution
				t = Math.sqrt(d);

				result = Math.cbrt(-q + t) + Math.cbrt(-q - t) - a;
				if (rangeCheck(result))
					return result;
			} else if (d == 0.0) {
				// Two real solutions, one double
				t = Math.cbrt(-q);

				result = 2 * t - a;
				if (rangeCheck(result))
					return result;

				result = -t - a;
				if (rangeCheck(result))
					return result;
			} else {
				// 3 real solutions
				double phi = Math.acos(-q / Math.sqrt(-(p * p * p)));
				t = Math.sqrt(-p);
				p = Math.cos(phi / 3);
				q = Math.sqrt(3 - 3 * p * p);

				result = 2 * t * p - a;
				if (rangeCheck(result))
					return result;

				result = -t * (p + q) - a;
				if (rangeCheck(result))
					return result;

				result = -t * (p - q) - a;
				if (rangeCheck(result))
					return result;
			}
		} else if (c2 != 0.0) { // degree 2 polynom
			b = c1 / (2 * c2);
			c = c0 / c2;
			p = b * b - c;

			if (p > 0) {
				p = Math.sqrt(p);

				result = -b - p;
				if (rangeCheck(result))
					return result;

				result = -b + p;
				if (rangeCheck(result))
					return result;
			} else if (p == 0) {
				result = -b;
				if (rangeCheck(result))
					return result;
			}
		} else if (c1 != 0.0) { // degree 1 polynom
			double m = c1;
			double t = c0;
			result = -t / m;

			if (rangeCheck(result))
				return result;
		} else { // degree 0 polynom
			return c0;
		}
		return failedCalculation();
	}

	/**
	 * Calculates the actual value for a parameter t previously found with
	 * {@link #findZero(float, float, float, float, float)}
	 *
	 * @param y1
	 *            y value of the left frame
	 * @param y2
	 *            y value of the right handle of the left frame
	 * @param y3
	 *            y value of the left handle of the right frame
	 * @param y4
	 *            y value of the right frame
	 * @param t
	 *            t on the curve in [0..1]
	 * @return the value on the curve
	 */
	public static float calcValue(float y1, float y2, float y3, float y4, double t) {

		float c1 = 3.0f * (y2 - y1);
		float c2 = 3.0f * (y1 - 2.0f * y2 + y3);
		float c3 = y4 - y1 + 3.0f * (y2 - y3);
		float value = y1;

		double tPot = t;
		value += tPot * c1; // t * c1
		tPot *= t;
		value += tPot * c2; // t * t * c2
		tPot *= t;
		value += tPot * c3; // t * t * t * c3
		return value;
	}

	// Convenience methods -------------------------
	private static float failedCalculation() {
		return 0.0F;
	}

	/***/
	private static boolean rangeCheck(double result) {
		double SMALL = -1.0e-10;
		return (result >= SMALL) && (result <= 1.000001D);
	}
}
