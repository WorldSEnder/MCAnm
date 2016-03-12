package com.github.worldsender.mcanm.common.skeleton;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point4f;
import javax.vecmath.Vector3f;

public interface IBone {
	/**
	 * Transforms the matrix given by the transformation currently acted out by the specified bone. Assumes that the
	 * matrix describes a transformation relative to the bone's origin.
	 *
	 * @param position
	 *            the position to transform
	 */
	void transformFromLocal(Matrix4f matrix);

	/**
	 * Transforms the matrix given by the transformation currently acted out by the specified bone. The matrix describes
	 * a transformation relative to the skeleton's origin.
	 *
	 * @param position
	 *            the position to transform
	 */
	void transform(Matrix4f matrix);

	/**
	 * Transforms the position given by the transformation currently acted out by the specified bone.
	 *
	 * @param position
	 *            the position to transform
	 */
	void transform(Point4f position);

	/**
	 * Transforms the normal given by the transformation currently acted out by the specified bone.
	 *
	 * @param position
	 *            the position to transform
	 */
	void transformNormal(Vector3f normal);
}
