package com.github.worldsender.mcanm.common.skeleton.visitor;

public interface ISkeletonVisitor {
	IBoneVisitor visitBone(String name);

	void visitEnd();
}
