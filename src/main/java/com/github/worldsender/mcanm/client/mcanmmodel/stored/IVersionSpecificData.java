package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitor;

public interface IVersionSpecificData {
	void visitBy(IModelVisitor visitor);
}
