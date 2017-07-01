package com.github.worldsender.mcanm.client.mcanmmodel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.gl.IModelRenderData;
import com.github.worldsender.mcanm.client.mcanmmodel.gl.ModelRenderDataGLArray;
import com.github.worldsender.mcanm.client.mcanmmodel.stored.RawData;
import com.github.worldsender.mcanm.client.mcanmmodel.visitor.IModelVisitable;
import com.github.worldsender.mcanm.client.model.IModelStateInformation;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.common.util.ReloadableData;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;

/**
 * Represents a model that is more abstract than boxes. The format also offers animating the model through bones and a
 * few more things.<br>
 * The model can be registered for reloading in the {@link ModelLoader}. This will reload the model whenever the user
 * changes the active texture packs. This means that mobs or whatever it is can be customized by the texture pack.
 * AWESOME.<br>
 * Maybe in the future minecraft accepts non-baked/models with a geometry shader. In that case this will get a rework.
 *
 * @author WorldSEnder
 *
 */
public class ModelMCMD extends ReloadableData<IModelVisitable> implements IModel {

	private static interface DataLoader {
		IModelRenderData loadFrom(IModelVisitable data, ISkeleton skelet);
	}

	private static final DataLoader LOADING_FUNC;

	static {
		// TODO: Decide depending on OpenGL capabilities?
		LOADING_FUNC = ModelRenderDataGLArray::new;
	}

	private String artist;
	private UUID modelUUID;

	private Optional<IModelRenderData> model;
	private ISkeleton skeleton;

	public ModelMCMD(IResourceLocation initial, ISkeleton skeleton) {
		super(initial, RawData::retrieveFrom, RawData.MISSING_DATA, skeleton);
	}

	@Override
	protected void preInit(Object... args) {
		skeleton = Objects.requireNonNull(ISkeleton.class.cast(args[0]));
	}

	// For MC > 1.8
	public void preBake() {}

	@Override
	protected void loadData(IModelVisitable data) {
		this.artist = data.getArtist();
		this.modelUUID = data.getModelUUID();
		this.model = Optional.of(LOADING_FUNC.loadFrom(data, skeleton));
	}

	/**
	 * Renders the given object in place (current OpenGL-transformation)
	 *
	 * @param renderPass
	 *            a description of the current render-pass
	 */
	@Override
	public void render(IRenderPass renderPass) {
		model.ifPresent(m -> m.render(renderPass));
	}

	public List<BakedQuad> getAsBakedQuads(
			IModelStateInformation currentPass,
			Map<String, TextureAtlasSprite> slotToTex,
			VertexFormat format) {
		return model.map(m -> m.getAsBakedQuads(currentPass, slotToTex, format)).orElse(Collections.emptyList());
	}

	/**
	 * @return the artist
	 */
	public String getArtist() {
		return artist;
	}

	/**
	 * @return the modelUUID
	 */
	public UUID getModelUUID() {
		return modelUUID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((modelUUID == null) ? 0 : modelUUID.hashCode());
		result = prime * result + ((skeleton == null) ? 0 : skeleton.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof ModelMCMD)) {
			return false;
		}
		ModelMCMD other = (ModelMCMD) obj;
		if (modelUUID == null) {
			if (other.modelUUID != null) {
				return false;
			}
		} else if (!modelUUID.equals(other.modelUUID)) {
			return false;
		}
		if (skeleton == null) {
			if (other.skeleton != null) {
				return false;
			}
		} else if (!skeleton.equals(other.skeleton)) {
			return false;
		}
		return true;
	}
}
