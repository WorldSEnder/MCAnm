package com.github.worldsender.mcanm.client.renderer;

public interface ITesselator {

	void setTextureUV(double u, double v);

	void setNormal(float x, float y, float z);

	void addVertex(double x, double y, double z);
}
