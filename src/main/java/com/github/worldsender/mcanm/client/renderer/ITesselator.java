package com.github.worldsender.mcanm.client.renderer;

import net.minecraft.client.renderer.Tessellator;

public interface ITesselator {
	public static final ITesselator TESSELATOR_WRAPPER = new ITesselator() {
		private final Tessellator instance = Tessellator.instance;

		@Override
		public void setTextureUV(double u, double v) {
			instance.setTextureUV(u, v);
		}

		@Override
		public void setNormal(float x, float y, float z) {
			instance.setNormal(x, y, z);
		}

		@Override
		public void addVertex(double x, double y, double z) {
			instance.addVertex(x, y, z);
		}
	};

	void setTextureUV(double u, double v);

	void setNormal(float x, float y, float z);

	void addVertex(double x, double y, double z);
}
