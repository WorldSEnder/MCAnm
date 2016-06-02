package com.github.worldsender.mcanm.client.mcanmmodel.visitor;

public interface IMaterialVisitor {
	void visitTexture(String textureName);

	void visitEnd();
}
