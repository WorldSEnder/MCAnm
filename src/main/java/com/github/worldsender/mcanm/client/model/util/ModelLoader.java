package com.github.worldsender.mcanm.client.model.util;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.model.mcanmmodel.MCMDModelLoader;
import com.github.worldsender.mcanm.client.model.mcanmmodel.ModelMCMD;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * This class caches and safes all models that were loaded with it. It functions at a global control point. All model
 * loading should, generally, be done through this class's interface.<br>
 * You can also register an own model. Models are registered using their {@link ResourceLocation} they have been loaded
 * from. You may only register a model that has been loaded from a valid {@link ResourceLocation}.
 *
 * @author WorldSEnder
 *
 */
public class ModelLoader {
	public static final ModelLoader instance = new ModelLoader();
	private static final Logger logger = MCAnm.logger;

	/**
	 * Loads a model from the {@link ResourceLocation}.
	 *
	 * @see #loadModelFrom(ResourceLocation)
	 */
	public static ModelMCMD loadFrom(ResourceLocation resLoc) {
		return instance.loadModelFrom(resLoc);
	}

	/**
	 * Loads a model from the {@link ResourceLocation}.
	 *
	 * @see #loadModelFrom(String)
	 */
	public static ModelMCMD loadFrom(String resLoc) {
		return instance.loadModelFrom(resLoc);
	}

	private HashMap<ResourceLocation, ModelMCMD> cachedModels = new HashMap<>();
	private IResourceManager currentManager = Minecraft.getMinecraft().getResourceManager();

	public void onResourceManagerReload(IResourceManager reloadManager) {
		if (!MCAnm.enableReload.getBoolean())
			return;
		this.currentManager = reloadManager;
		for (Entry<ResourceLocation, ModelMCMD> entry : this.cachedModels.entrySet()) {
			entry.getValue().reload(reloadManager);
		}
	}

	/**
	 * Registers a model in this registry. If a model has previously been registered this method does nothing and
	 * returns false.<br>
	 * It is an error to register <code>null</code> or a model that has not been loaded from a valid
	 * {@link ResourceLocation}. It is valid though to register a model that has been loaded incorrectly but through
	 *
	 * @param model
	 *            the model to register
	 * @return true if the model was registered, false if there was already a model registered for the given model's
	 *         resource location
	 * @throws IllegalArgumentException
	 *             when the model is <code>null</code> or has been loaded from an invalid {@link ResourceLocation}
	 */
	public boolean registerModel(ModelMCMD model) {
		if (model == null)
			throw new IllegalArgumentException("Model can't be null");
		ResourceLocation resLoc = model.getResourceLocation();
		if (resLoc == null)
			throw new IllegalArgumentException(
					"The model you are trying to register has not been loaded from a valid ResourceLocation.");
		if (this.cachedModels.containsKey(resLoc))
			return false;
		this.cachedModels.put(resLoc, model);
		return true;
	}

	/**
	 * Loads a {@link ModelMCMD} from the {@link ResourceLocation} given. If a model for the requested resource location
	 * has been previously registered or loaded with this method that model will be returned instead.<br>
	 * This method automatically registers a model if it is loaded through this method and not already registered.
	 *
	 * @param resLocation
	 *            the resource location to load the model from
	 * @return a {@link ModelMCMD} that corresponds to the requested {@link ResourceLocation}
	 */
	public ModelMCMD loadModelFrom(ResourceLocation resLocation) {
		ModelMCMD model = this.cachedModels.get(resLocation);
		if (model == null) {
			logger.trace(String.format("[ModelLoader] Loading new model from %s", resLocation));
			// Not registered
			model = MCMDModelLoader.instance.loadInstance(resLocation, this.currentManager, false);
			boolean registered = this.registerModel(model);
			assert registered == true;
		} else {
			logger.trace(String.format("[ModelLoader] Loading cached model from %s", resLocation));
		}
		return model;
	}

	/**
	 * Loads a {@link ModelMCMD} from the Resource-location given. This will simply call
	 * {@link #loadModelFrom(ResourceLocation)} with <code>new {@link ResourceLocation}(resLocation)</code>.
	 *
	 * @param resLocation
	 *            the resource location to load the model from
	 * @return a {@link ModelMCMD} that corresponds to the requested {@link ResourceLocation}
	 * @see #loadModelFrom(ResourceLocation)
	 */
	public ModelMCMD loadModelFrom(String resLocation) {
		return loadModelFrom(new ResourceLocation(resLocation));
	}
}
