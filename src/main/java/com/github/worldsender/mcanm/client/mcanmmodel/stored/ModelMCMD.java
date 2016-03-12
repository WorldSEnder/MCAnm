package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.worldsender.mcanm.client.IRenderPass;
import com.github.worldsender.mcanm.client.mcanmmodel.IModel;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.resource.MinecraftResource;
import com.github.worldsender.mcanm.common.resource.MinecraftResourceLocation;
import com.github.worldsender.mcanm.common.skeleton.ISkeleton;
import com.github.worldsender.mcanm.common.util.ExceptionLessFunctions.ThrowingSupplier;
import com.github.worldsender.mcanm.common.util.ReloadableData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

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
		IModelData loadFrom(IModelVisitable data, ISkeleton skelet);
	}

	private static final DataLoader LOADING_FUNC;

	static {
		// TODO: Decide depending on OpenGL capabilities?
		LOADING_FUNC = (data, skl) -> {
			return new ModelDataBasic(data, skl);
		};
	}

	private String artist;
	private UUID modelUUID;

	private Optional<IModelData> model;
	private ISkeleton skeleton;

	public ModelMCMD(ResourceLocation resLoc, ISkeleton skeleton) {
		this(resLoc, Minecraft.getMinecraft().getResourceManager(), skeleton);
	}

	public ModelMCMD(ResourceLocation resLoc, IResourceManager manager, ISkeleton skeleton) {
		this(() -> new MinecraftResource(resLoc, manager), () -> new MinecraftResourceLocation(resLoc), skeleton);
	}

	public ModelMCMD(
			ThrowingSupplier<IResource, IOException> initial,
			Supplier<IResourceLocation> reloadLocation,
			ISkeleton skeleton) {
		super(
				initial,
				reloadLocation,
				ReloadableData.convertThrowingLoader(RawData::retrieveFrom, "Error loading model from %s."),
				RawData.MISSING_DATA);
		skeleton = Objects.requireNonNull(skeleton);
	}

	public ModelMCMD(IResource initial, ISkeleton skeleton) {
		super(
				initial,
				ReloadableData.convertThrowingLoader(RawData::retrieveFrom, "Error loading model from %s."),
				RawData.MISSING_DATA);
		skeleton = Objects.requireNonNull(skeleton);
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
	public void render(IRenderPass renderPass) {
		model.ifPresent(m -> m.render(renderPass));
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
