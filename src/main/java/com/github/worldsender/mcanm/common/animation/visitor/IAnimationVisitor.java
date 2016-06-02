package com.github.worldsender.mcanm.common.animation.visitor;

import com.github.worldsender.mcanm.common.animation.parts.AnimatedTransform;

public interface IAnimationVisitor {

	void visitArtist(String artist);

	void visitBone(String name, AnimatedTransform transform);

	void visitEnd();
}
