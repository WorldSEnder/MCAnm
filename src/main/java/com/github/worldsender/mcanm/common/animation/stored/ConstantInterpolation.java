package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;

import org.lwjgl.util.vector.Vector2f;

/**
 * Describes an interpolation between two values at two frames. Let those pairs be (f<SUB>1</SUB>, v<SUB>1</SUB>) and
 * (f<SUB>2</SUB>, v<SUB>2</SUB>). The value returned will be v<SUB>1</SUB> if <code>frame < f<SUB>2</SUB> </code> and
 * v<SUB>2</SUB> otherwise.
 *
 * @author WorldSEnder
 *
 */
public class ConstantInterpolation extends Spline {
	public static final IInterpolationSplineFactory factory = new IInterpolationSplineFactory() {
		@Override
		public Spline newSpline(Vector2f left, Vector2f right, DataInputStream additionalData) {
			return new ConstantInterpolation(left, right);
		}
	};

	private Vector2f left;
	private Vector2f right;

	private ConstantInterpolation(Vector2f left, Vector2f right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public boolean isInRange(float frame) {
		return frame >= this.left.x && frame <= this.right.x;
	}

	@Override
	public float getValueAt(float frame) {
		return frame < this.right.x ? this.left.y : this.right.y;
	}
}
