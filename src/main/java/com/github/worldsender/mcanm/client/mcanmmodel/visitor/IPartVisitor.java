package com.github.worldsender.mcanm.client.mcanmmodel.visitor;

import java.util.Arrays;

public interface IPartVisitor {
	void visitTesselationPoint(TesselationPoint point);

	default void visitTesselationPoints(Iterable<TesselationPoint> points) {
		for (TesselationPoint p : points) {
			visitTesselationPoint(p);
		}
	}

	default void visitTesselationPoints(TesselationPoint[] points) {
		this.visitTesselationPoints(Arrays.asList(points));
	}

	/**
	 * Visit a face. Parameters are zero-based indices in the order of visitation of the tesselation points. Tesselation
	 * points might be visited before or afterwards.
	 * 
	 * @param tess1
	 * @param tess2
	 * @param tess3
	 */
	void visitFace(short tess1, short tess2, short tess3);

	/**
	 * Visit multiple faces at once.
	 * 
	 * @param tessOrder
	 *            short[]#length must be divisible by 3
	 * @see #visitFace(short, short, short)
	 */
	default void visitFaces(short[] tessOrder) {
		if (tessOrder.length % 3 != 0) {
			throw new IllegalArgumentException();
		}
		int maxI = tessOrder.length / 3;
		for (int i = 0; i < maxI; i++) {
			visitFace(tessOrder[i], tessOrder[i + 1], tessOrder[i + 2]);
		}
	}

	IMaterialVisitor visitTexture();

	void visitEnd();
}
