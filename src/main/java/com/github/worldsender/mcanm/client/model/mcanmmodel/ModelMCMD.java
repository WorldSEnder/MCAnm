package com.github.worldsender.mcanm.client.model.mcanmmodel;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.ModelFormatException;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.Reference;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.glcontext.GLHelper;
import com.github.worldsender.mcanm.client.model.mcanmmodel.loader.VersionizedModelLoader;
import com.github.worldsender.mcanm.client.renderer.IAnimatedObject;
/**
 * Represents a model that is more abstract than boxes. The format also offers
 * animating the model through bones and a few more things.<br>
 * The model can be registered for reloading in the {@link ModelRegistry}. This
 * will reload the model whenever the user changes the active texture packs.
 * This means that mobs or whatever it is can be customized by the texture pack.
 * AWESOME.
 *
 * @author WorldSEnder
 *
 */
public class ModelMCMD implements IModelCustom {
	private static RawData loadDataChecked(ResourceLocation resource,
			IResourceManager resManager) {
		try {
			return VersionizedModelLoader.loadVersionized(resource, resManager);
		} catch (ModelFormatException mfe) {
			MCAnm.logger.error(
					String.format("Error loading model from %s.", resource),
					mfe);
		}
		return null;
	}

	private GLHelper renderHelper;
	/**
	 * Resource location of the data that will be loaded on next texturepack
	 * change.
	 */
	private ResourceLocation reloadLocation;
	private String artist;
	private UUID modelUUID;
	/**
	 * Convenience constructor. This loads the model from the given
	 * {@link ResourceLocation} but uses the default ResourceManager to actually
	 * load the resource.
	 *
	 * @param resource
	 *            the resource to load from
	 */
	public ModelMCMD(ResourceLocation resource) {
		this(resource, Minecraft.getMinecraft().getResourceManager());
	}
	/**
	 * Creates a new ModelMCMD from a given {@link ResourceLocation} using the
	 * given {@link IResourceManager}. This constructor will not throw occuring
	 * {@link ModelFormatException}s. All occuring {@link ModelFormatException}s
	 * will be logged in {@link MCAnm#logger} as an error. This will result in a
	 * model that will hold some sort of default model (could be a cube/nothing
	 * at all).
	 *
	 * @param resource
	 *            - The resource to load from. Refer to the wiki on
	 *            https://github.com/Heltrato/model_type/wiki/API-overview to
	 *            get detail about the modelformat
	 */
	public ModelMCMD(ResourceLocation resource, IResourceManager resManager) {
		// This line could throw
		this(loadDataChecked(resource, resManager));
		MCAnm.logger.trace(String.format(
				"[Model] Loading model from resource: %s", resource));
	}
	/**
	 * This constructor will not throw. INTERNAL USE<br>
	 * This constructor is thought as a possibility to load the
	 */
	protected ModelMCMD(RawData data) {
		this.renderHelper = GLHelper.getNewAppropriateHelper();
		this.putData(data);
		// Else there was an error during loading, we are supposed not to react
		// to that
	}
	/**
	 * Reloads this model from the resource-location it was previously loaded
	 * from with the {@link IResourceManager} given. If this wasn't loaded from
	 * a {@link ResourceLocation} or loading is erroneous the model keeps its
	 * previous state.
	 */
	public void reload(IResourceManager newManager) {
		if (this.reloadLocation == null)
			return;
		try {
			RawData data = VersionizedModelLoader.loadVersionized(
					this.reloadLocation, newManager);
			this.putData(data);
		} catch (ModelFormatException mfe) {
			// DO nothing if failed
		}
	}
	/** Helper function */
	private void putData(RawData data) {
		if (data != null) {
			this.renderHelper.loadInto(data);
			this.artist = data.artist;
			this.modelUUID = data.modelUUID;
			this.reloadLocation = data.srcLocation;
		} else {
			this.artist = "UNKOWN_ARTIST";
			this.modelUUID = new UUID(0, 0);
			this.reloadLocation = null;
		}
	}
	/**
	 * Gets the {@link ResourceLocation} this model was loaded from. This may be
	 * <code>null</code> if this model was loaded from a stream.
	 *
	 * @return
	 */
	public ResourceLocation getResourceLocation() {
		return this.reloadLocation;
	}
	/**
	 * Models are equal if any of the following condition:<br>
	 * <ul>
	 * <li>the resource location they have been loaded from is the same and not
	 * <code>null</code>
	 * <li>they share the same {@link UUID}
	 * </ul>
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ModelMCMD) {
			ModelMCMD other = (ModelMCMD) obj;
			// Compare loading locations
			if (this.reloadLocation == other.reloadLocation
					&& this.reloadLocation != null)
				return true;
			// Compare model ids
			if (this.modelUUID == other.modelUUID)
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
	 * @param object
	 *            the object to render (get animations from there)
	 */
	public void render(IAnimatedObject object, float parTick) {
		renderHelper.render(object, parTick);
	}

	@Override
	public String getType() {
		return Reference.model_type;
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
	// ---------------------- Never used ---------------------------
	private static void throwUsage() {
		throw new IllegalAccessError(
				"Can't use this method with this model-type. Use one of render(...)");
	}
	@Override
	public void renderAll() {
		throwUsage();
	}
	@Override
	public void renderOnly(String... groupNames) {
		throwUsage();
	}
	@Override
	public void renderPart(String partName) {
		throwUsage();
	}
	@Override
	public void renderAllExcept(String... excludedGroupNames) {
		throwUsage();
	}
}
