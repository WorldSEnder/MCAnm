package com.github.worldsender.mcanm.client.renderer;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Preconditions;

import net.minecraft.client.renderer.GLAllocation;

/**
 * Only used to draw triangles!!! Implements a tesselator that draws a model with never-changing indices but changing
 * vertices.
 * 
 * @author WorldSEnder
 *
 */
public class DrawElementsTesselator implements ITesselator {
	// Pos, Normal, Uv
	private static final int BYTES_PER_VERTEX = 4 * (3 + 3 + 2);
	/** The byte buffer used for GL allocation. */
	private FloatBuffer floatBuffer;
	private ShortBuffer indexBuffer;

	private float texU, texV;
	private float normalX, normalY, normalZ;
	private boolean isDrawing;

	public DrawElementsTesselator(int vertexCount, short[] indices) {
		setBuffer(vertexCount, indices);
		this.isDrawing = false;
	}

	private void setBuffer(int vertexCount, short[] indices) {
		floatBuffer = GLAllocation.createDirectByteBuffer(vertexCount * BYTES_PER_VERTEX).asFloatBuffer();
		// Could check if all indices are in range, but meh
		indexBuffer = GLAllocation.createDirectByteBuffer(indices.length * 2).asShortBuffer();
		indexBuffer.put(indices);
		indexBuffer.position(0);
	}

	/**
	 * Start a new draw
	 */
	public void startDrawing() {
		Preconditions.checkState(!isDrawing, "already drawing");
		isDrawing = true;
		floatBuffer.position(0);
	}

	@Override
	public void setTextureUV(double u, double v) {
		this.texU = (float) u;
		this.texV = (float) v;
	}

	@Override
	public void setNormal(float x, float y, float z) {
		this.normalX = x;
		this.normalY = y;
		this.normalZ = z;
	}

	@Override
	public void addVertex(double x, double y, double z) {
		this.floatBuffer.put((float) x).put((float) y).put((float) z).put(normalX).put(normalY).put(normalZ).put(texU)
				.put(texV);
	}

	public void draw() {
		Preconditions.checkState(isDrawing, "not drawing");
		Preconditions.checkState(floatBuffer.remaining() == 0, "not all vertices filled");
		isDrawing = false;

		// FIXME: use GLStateManager
		this.floatBuffer.position(0);
		GL11.glVertexPointer(3, BYTES_PER_VERTEX, this.floatBuffer);
		GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);

		this.floatBuffer.position(3);
		GL11.glNormalPointer(BYTES_PER_VERTEX, this.floatBuffer);
		GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

		this.floatBuffer.position(6);
		GL11.glTexCoordPointer(2, BYTES_PER_VERTEX, this.floatBuffer);
		GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);

		GL11.glDrawElements(GL_TRIANGLES, indexBuffer);

		GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
		GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
		GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
	}

}
