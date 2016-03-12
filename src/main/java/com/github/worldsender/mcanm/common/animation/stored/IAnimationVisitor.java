package com.github.worldsender.mcanm.common.animation.stored;

public interface IAnimationVisitor {

	void visitArtist(String artist);

	void visitBone(String name, AnimatedTransform transform);

	void visitEnd();
}
