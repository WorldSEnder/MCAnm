package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import java.util.UUID;

public interface IModelVisitor {
	void visitModelUUID(UUID uuid);

	void visitArtist(String artist);
}
