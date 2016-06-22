package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import com.github.worldsender.mcanm.client.IRenderPass;

public interface IPart {

	void render(IRenderPass currentPass);

	String getName();
}
