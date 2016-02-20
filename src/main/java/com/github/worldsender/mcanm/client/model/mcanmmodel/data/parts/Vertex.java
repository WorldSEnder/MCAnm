package com.github.worldsender.mcanm.client.model.mcanmmodel.data.parts;

import javax.vecmath.Point2f;
import javax.vecmath.Point4f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import net.minecraft.client.renderer.Tessellator;

public class Vertex {

	private Point4f pos;
	private Vector3f norm;
	private Point2f uv;

	public Vertex(Tuple4f pos, Tuple3f norm, Tuple2f uv) {
		this.pos = new Point4f(pos);
		this.norm = new Vector3f(norm);
		this.uv = new Point2f(uv);
	}

	/**
	 * Uses the {@link Tessellator} to draw the model. Take care that the Tessellator is already drawing.
	 */
	public void render(Tessellator renderer) {
		renderer.setNormal(norm.x, norm.z, -norm.y);
		renderer.setTextureUV(uv.x, uv.y);
		renderer.addVertex(pos.x / pos.w, pos.z / pos.w, -pos.y / pos.w);
	}

	/**
	 * Offsets this Vertex by the {@link Vector4f} given.
	 *
	 * @param vector
	 *            the offset
	 */
	public void offset(Point4f point) {
		this.pos.add(point);
	}

	/**
	 * Adds the normal to this Vertex basically interpolating between the current normal and the given one (by their
	 * scale). No normalization is done as this is taken care of by OpenGL during rendering.
	 *
	 * @param normal
	 *            the normal to add
	 */
	public void addNormal(Vector3f normal) {
		this.norm.add(normal);
	}

	/**
	 * Sets the UV texture coordinates for this vertex
	 *
	 * @param uv
	 *            the new texture coordinates
	 */
	public void setUV(Tuple2f uv) {
		this.uv.set(uv);
	}

	public void copyUV(Vertex src) {
		this.uv.set(src.uv);
	}

	/**
	 * Stores this vertex's uv coordinates in the target.
	 */
	public void getUV(Tuple2f trgt) {
		trgt.set(this.uv);
	}

	/**
	 * Stores this vertex's normal in the target.
	 */
	public void getNormal(Tuple3f trgt) {
		trgt.set(this.norm);
	}

	/**
	 * Stores this vertex's position in the target.
	 */
	public void getPosition(Tuple4f trgt) {
		trgt.set(this.pos);
	}
}
