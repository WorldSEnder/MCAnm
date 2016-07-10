package com.github.worldsender.mcanm.client.renderer;

import net.minecraft.client.renderer.Tessellator;

public interface ITesselator {
	public static final ITesselator TESSELATOR_WRAPPER = new ITesselator() {
		private final Tessellator instance = Tessellator.getInstance();

		@Override
		public void setTextureUV(double u, double v) {
			instance.getWorldRenderer().tex(u, v);
		}

		@Override
		public void setNormal(float x, float y, float z) {
			instance.getWorldRenderer().normal(x, y, z);
		}

		@Override
		public void addVertex(double x, double y, double z) {
			instance.getWorldRenderer().pos(x, y, z);
		}
	};

	void setTextureUV(double u, double v);

	void setNormal(float x, float y, float z);

	void addVertex(double x, double y, double z);
}
