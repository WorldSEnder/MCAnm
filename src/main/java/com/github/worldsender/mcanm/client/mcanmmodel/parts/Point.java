package com.github.worldsender.mcanm.client.mcanmmodel.parts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.vecmath.Point4f;
import javax.vecmath.Tuple2f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Tuple4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import com.github.worldsender.mcanm.client.mcanmmodel.visitor.BoneBinding;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.TesselationPoint;
import com.github.worldsender.mcanm.client.renderer.ITesselator;
import com.github.worldsender.mcanm.common.skeleton.IBone;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

public class Point {
	private static class BoundPoint extends Point {
		private static class Binding {
			// Used as a buffer, doesn't requite to always create new
			// temporaries. Prevents parallelization, though
			private static Point4f posBuff = new Point4f();
			private static Vector3f normBuff = new Vector3f();

			private IBone bone;
			private float strength;

			public Binding(IBone bone, float strenght) {
				this.bone = Objects.requireNonNull(bone);
				this.strength = strenght;
			}

			/**
			 * Computes the transformed and weighted position of the given vertex. Adds that to the target vertex.
			 *
			 * @param base
			 *            the vertex to transform
			 * @param trgt
			 *            the vertex to add to. If null is given, it is just assigned to
			 * @return the final vertex, a new vertex if <code>null</code> was given
			 */
			public void addTransformed(Vertex base, Vertex trgt) {
				Objects.requireNonNull(base);
				Objects.requireNonNull(trgt);

				base.getPosition(posBuff);
				base.getNormal(normBuff);
				// Transform points with matrix
				this.bone.transform(posBuff);
				this.bone.transformNormal(normBuff);

				posBuff.scale(this.strength);
				normBuff.scale(this.strength);

				trgt.offset(posBuff);
				trgt.addNormal(normBuff);
			}

			public void normalize(float sum) {
				this.strength /= sum;
			}
		}

		private List<Binding> binds;
		private Vertex transformed;

		public BoundPoint(Vector3f pos, Vector3f norm, Vector2f uv, BoneBinding[] readBinds, ISkeleton skelet) {
			super(pos, norm, uv);
			// readBinds can be assumed to at least be size 1
			this.binds = new ArrayList<>();
			this.transformed = new Vertex(this.vert);
			float strengthSummed = 0.0F;
			for (BoneBinding bind : readBinds) {
				if (bind.bindingValue <= 0.0f)
					continue;
				Binding binding = new Binding(
						skelet.getBoneByIndex(Byte.toUnsignedInt(bind.boneIndex)),
						bind.bindingValue);
				this.binds.add(binding);
				strengthSummed += bind.bindingValue;
			}
			for (Binding bind : this.binds) {
				bind.normalize(strengthSummed);
			}
		}

		protected Vertex getTransformedVertex() {
			Vertex base = this.vert;
			Vertex transformed = setupTransformed();
			for (Binding bind : this.binds) {
				bind.addTransformed(base, transformed);
			}
			return transformed;
		}

		private Vertex setupTransformed() {
			this.transformed.retainUVOnly();
			return this.transformed;
		}
	}

	protected final Vertex vert;

	protected Point(Vector3f pos, Vector3f norm, Vector2f uv) {
		this.vert = new Vertex(new Vector4f(pos.x, pos.y, pos.z, 1F), norm, uv);
	}

	/**
	 * Renders this point, already transformed
	 *
	 * @param bones
	 *            the models bones
	 */
	public void render(ITesselator renderer) {
		getTransformedVertex().render(renderer);
	}

	protected Vertex getTransformedVertex() {
		return vert;
	}

	public void putIntoBakedQuadBuilder(UnpackedBakedQuad.Builder builder, TextureAtlasSprite sprite) {
		Vertex transformed = getTransformedVertex();
		Tuple4f positionBuffer = new Vector4f();
		transformed.getPosition(positionBuffer);
		Tuple3f normalBuffer = new Vector3f();
		transformed.getNormal(normalBuffer);
		Tuple2f uvBuffer = new Vector2f();
		transformed.getUV(uvBuffer);

		VertexFormat vertexFormat = builder.getVertexFormat();
		int elementCount = vertexFormat.getElementCount();
		for (int e = 0; e < elementCount; e++) {
			VertexFormatElement element = vertexFormat.getElement(e);
			switch (element.getUsage()) {
			case POSITION:
				builder.put(e, positionBuffer.x, positionBuffer.z, -positionBuffer.y, positionBuffer.w);
				break;
			case NORMAL:
				builder.put(e, normalBuffer.x, normalBuffer.z, -normalBuffer.y, 0);
				break;
			case UV:
				if (element.getIndex() != 0)
					break;
				builder.put(
						e,
						sprite.getInterpolatedU(uvBuffer.x * 16),
						sprite.getInterpolatedV(uvBuffer.y * 16),
						0,
						1);
				break;
			case COLOR:
				builder.put(e, 1, 1, 1, 1);
				break;
			default:
				builder.put(e);
			}
		}
	}

	/**
	 * Constructs a bone from the {@link TesselationPoint} given. This is implemented in the factory style to
	 * efficiently handle bones without Bindings
	 *
	 * @param data
	 *            the point to construct from
	 * @return the constructed point
	 */
	public static Point from(TesselationPoint data, ISkeleton skelet) {
		boolean isBound = data.boneBindings.length > 0;
		if (isBound)
			return new BoundPoint(data.coords, data.normal, data.texCoords, data.boneBindings, skelet);
		return new Point(data.coords, data.normal, data.texCoords);
	}
}
