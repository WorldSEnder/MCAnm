package com.github.worldsender.mcanm.common.skeleton.visitor;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

public interface IBoneVisitor {
	void visitParent(byte parentIndex);

	void visitLocalOffset(Vector3f headPosition);

	void visitLocalRotation(Quat4f rotation);

	void visitEnd();
}
