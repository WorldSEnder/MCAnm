package com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class Vertex {

	private Vector4f pos;
	private Vector3f norm;
	private Vector2f uv;

	public Vertex(Vector4f pos, Vector3f norm, Vector2f uv) {
		if (pos == null)
			pos = new Vector4f();
		if (norm == null)
			norm = new Vector3f();
		if (uv == null)
			uv = new Vector2f();
		this.pos = pos;
		this.norm = norm;
		this.uv = uv;
	}
	/**
	 * Uses the {@link Tessellator} to draw the model. Take care that the
	 * Tessellator is already drawing.
	 */
	public void render(WorldRenderer renderer) {
		renderer.func_178980_d(norm.x, norm.z, -norm.y);
		renderer.setTextureUV(uv.x, uv.y);
		renderer.addVertex(pos.x / pos.w, pos.z / pos.w, -pos.y / pos.w);
	}
	/**
	 * Offsets this Vertex by the {@link Vector4f} given.
	 *
	 * @param vector
	 *            the offset
	 */
	public void offset(Vector4f vector) {
		this.pos = Vector4f.add(this.pos, vector, this.pos);
	}
	/**
	 * Adds the normal to this Vertex basically interpolating between the
	 * current normal and the given one (by their scale). No normalization is
	 * done as this is taken care of by OpenGL during rendering.
	 *
	 * @param normal
	 *            the normal to add
	 */
	public void addNormal(Vector3f normal) {
		Vector3f.add(this.norm, normal, this.norm);
	}
	/**
	 * Sets the UV texture coordinates for this vertex
	 *
	 * @param uv
	 *            the new texture coordinates
	 */
	public void setUV(Vector2f uv) {
		this.uv.set(uv);
	}
	/**
	 * This vertex's uv coordinates
	 *
	 * @return a copy of this vertex's uv
	 */
	public Vector2f getUV() {
		return new Vector2f(this.uv);
	}
	/**
	 * This vertex's normal
	 *
	 * @return a copy of this vertex's normal
	 */
	public Vector3f getNormal() {
		return new Vector3f(this.norm);
	}
	/**
	 * This vertex's homogeneous position
	 *
	 * @return a copy of this vertex's position
	 */
	public Vector4f getPosition() {
		return new Vector4f(this.pos);
	}
}