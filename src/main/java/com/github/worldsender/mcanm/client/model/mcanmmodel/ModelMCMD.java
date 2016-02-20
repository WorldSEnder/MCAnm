package com.github.worldsender.mcanm.client.model.mcanmmodel;

import java.util.UUID;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.GLHelper;
import com.github.worldsender.mcanm.client.model.mcanmmodel.loader.VersionizedModelLoader;
import com.github.worldsender.mcanm.client.model.util.ModelLoader;
import com.google.common.base.Optional;

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
public class ModelMCMD {
	public static class MCMDState {
		private Optional<RawData> rawData;

		public MCMDState(Optional<RawData> data) {
			rawData = data;
		}

		public Optional<RawData> getData() {
			return rawData;
		}
	}

	private static Optional<RawData> loadDataChecked(ResourceLocation resource, IResourceManager resManager) {
		try {
			return Optional.of(VersionizedModelLoader.loadVersionized(resource, resManager));
		} catch (ModelFormatException mfe) {
			MCAnm.logger().error(String.format("Error loading model from %s.", resource), mfe);
		}
		return Optional.absent();
	}

	private GLHelper renderHelper;
	/**
	 * Resource location of the data that will be loaded on next texturepack change.
	 */
	private Optional<ResourceLocation> reloadLocation;
	private MCMDState stateCache;

	private String artist;
	private UUID modelUUID;

	/**
	 * Convenience constructor. This loads the model from the given {@link ResourceLocation} but uses the default
	 * ResourceManager to actually load the resource.
	 *
	 * @param resource
	 *            the resource to load from
	 */
	public ModelMCMD(ResourceLocation resource) {
		this(resource, Minecraft.getMinecraft().getResourceManager());
	}

	/**
	 * Creates a new ModelMCMD from a given {@link ResourceLocation} using the given {@link IResourceManager}. This
	 * constructor will not throw occuring {@link ModelFormatException}s. All occuring {@link ModelFormatException}s
	 * will be logged in {@link MCAnm#logger} as an error. This will result in a model that will hold some sort of
	 * default model (could be a cube/nothing at all).
	 *
	 * @param resource
	 *            - The resource to load from. Refer to the wiki on
	 *            https://github.com/Heltrato/model_type/wiki/API-overview to get detail about the modelformat
	 */
	public ModelMCMD(ResourceLocation resource, IResourceManager resManager) {
		// This line could throw
		this(loadDataChecked(resource, resManager));
		MCAnm.logger().trace(String.format("[Model] Loading model from resource: %s", resource));
	}

	/**
	 * This constructor will not throw. INTERNAL USE<br>
	 * This constructor is thought as a possibility to load the
	 */
	protected ModelMCMD(Optional<RawData> data) {
		this.renderHelper = GLHelper.getAppropriateHelper();
		this.putData(data);
		// Else there was an error during loading, we are supposed not to react
		// to that
	}

	/**
	 * Reloads this model from the resource-location it was previously loaded from with the {@link IResourceManager}
	 * given. If this wasn't loaded from a {@link ResourceLocation} or loading is erroneous the model keeps its previous
	 * state.
	 */
	public void reload(IResourceManager newManager) {
		if (this.reloadLocation == null)
			return;
		try {
			RawData data = VersionizedModelLoader.loadVersionized(this.reloadLocation.get(), newManager);
			this.putData(Optional.of(data));
		} catch (ModelFormatException mfe) {
			this.putData(Optional.<RawData>absent());
		}
	}

	/**
	 * "Bakes" the model, resolves as much as possible
	 *
	 * @param format
	 *            the supported format, //FIXME: format currently UNUSED
	 */
	public void preBake() {
		renderHelper.preBake(stateCache);
	}

	/** Helper function */
	private void putData(Optional<RawData> loaded) {
		this.stateCache = new MCMDState(loaded);
		if (loaded.isPresent()) {
			RawData data = loaded.get();
			this.artist = data.artist;
			this.modelUUID = data.modelUUID;
			this.reloadLocation = Optional.fromNullable(data.srcLocation);
		} else {
			this.artist = "UNKOWN_ARTIST";
			this.modelUUID = new UUID(0, 0);
			this.reloadLocation = null;
		}
	}

	/**
	 * Gets the {@link ResourceLocation} this model was loaded from. This may be <code>null</code> if this model was
	 * loaded from a stream.
	 *
	 * @return
	 */
	public Optional<ResourceLocation> getResourceLocation() {
		return this.reloadLocation;
	}

	/**
	 * Models are equal if any of the following condition:<br>
	 * <ul>
	 * <li>the resource location they have been loaded from is the same and not <code>null</code>
	 * <li>they share the same {@link UUID}
	 * </ul>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ModelMCMD)) {
			return false;
		}
		ModelMCMD other = (ModelMCMD) obj;
		// Compare loading locations
		if (this.reloadLocation != null && this.reloadLocation.equals(other.reloadLocation)) {
			return true;
		}
		// Compare model ids
		if (this.modelUUID == other.modelUUID) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.modelUUID.hashCode() ^ this.reloadLocation.hashCode();
	}

	/**
	 * Renders the given object in place (current OpenGL-transformation)
	 *
	 * @param renderPass
	 *            a description of the current render-pass
	 */
	public void render(IRenderPass renderPass) {
		renderHelper.render(renderPass);
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
}
