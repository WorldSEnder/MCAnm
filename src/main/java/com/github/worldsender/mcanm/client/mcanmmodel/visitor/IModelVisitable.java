package com.github.worldsender.mcanm.client.mcanmmodel.visitor;

import java.util.UUID;

public interface IModelVisitable {
	void visitBy(IModelVisitor visitor);

	String getArtist();

	UUID getModelUUID();
}
