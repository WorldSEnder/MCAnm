package com.github.worldsender.mcanm.client.mcanmmodel.visitor;

import java.util.UUID;

public interface IModelVisitor {
	void visitModelUUID(UUID uuid);

	void visitArtist(String artist);

	IPartVisitor visitPart(String name);

	void visitEnd();
}
