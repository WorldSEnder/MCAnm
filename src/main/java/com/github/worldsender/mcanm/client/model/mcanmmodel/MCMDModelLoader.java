package com.github.worldsender.mcanm.client.model.mcanmmodel;

import java.io.DataInputStream;
import java.io.IOError;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.client.model.mcanmmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mcanmmodel.loader.VersionizedModelLoader;
import com.github.worldsender.mcanm.client.model.util.ModelLoader;

import com.google.common.base.Optional;

/**
 * This class can be used to load a model without registering it automatically
 * in the {@link ModelLoader}. This can be useful if you later want to load
 * the model with different data.
 *
 * @author WorldSEnder
 *
 */
public class MCMDModelLoader implements IResourceManagerReloadListener {
	/**
	 * This instance is registered in the {@link AdvancedModelLoader}-class
	 * provided by forge.
	 */
	public static final MCMDModelLoader instance = new MCMDModelLoader();

	private IResourceManager manager;

	private IResourceManager getManager() {
		return manager == null
				? Minecraft.getMinecraft().getResourceManager()
				: manager;
	}

	/**
	 * Used to load the models in an exception-free manner (for n0bs or just
	 * lazy guys who don't want to deal with exceptions). Static version of
	 * {@link #loadInstance(ResourceLocation)}
	 *
	 * @param resLoc
	 *            the resource location to load from
	 * @return the loaded model, never null
	 * @see #loadInstance(ResourceLocation)
	 */
	public static ModelMCMD loadMCMDModel(ResourceLocation resLoc) {
		return instance.loadInstance(resLoc);
	}
	/**
	 * Actually loads the model. Note that this method will (normally) not
	 * throw. It catches {@link IOError}s as well as
	 * {@link ModelFormatException}, etc. This will always return a new Model
	 * but that may be empty (not render).<br>
	 * Convenience method, propagates to
	 * {@link #loadInstance(ResourceLocation, IResourceManager, boolean)} with
	 * the default {@link IResourceManager} and registers the model.
	 *
	 * @param resource
	 *            the resource to load this model from
	 * @return the newly loaded {@link ModelMCMD}, never <code>null</code>
	 */
	public ModelMCMD loadInstance(ResourceLocation resource) {
		return this.loadInstance(resource, getManager(), true);
	}
	/**
	 * Actually loads the model. Note that this method will (normally) not
	 * throw. It catches {@link IOError}s as well as
	 * {@link ModelFormatException}, etc. This will always return a new Model
	 * but that may be empty (not render).
	 *
	 * @param resource
	 *            the resource to load this model from
	 * @param resManager
	 *            the resource manager to load the resource from
	 * @param register
	 *            if true the loaded model will be registered to reload on
	 *            switching resourcepack
	 * @return the newly loaded {@link ModelMCMD}, never <code>null</code>
	 */
	public ModelMCMD loadInstance(ResourceLocation resource,
			IResourceManager resManager, boolean register) {
		// The following constructor will not throw but eat our loaded data even
		// when that is illegal.
		ModelMCMD model = new ModelMCMD(resource, resManager);
		if (register) {
			ModelLoader.instance.registerModel(model);
		}
		model.preBake();
		return model;
	}
	/**
	 * ADVANCED USE<br>
	 * Use this to load a Model and receive any occuring Exceptions. The
<<<<<<< HEAD
	 * received model can later be registered in the {@link ModelLoader} to
=======
	 * received model can later be registered in the {@link ModelRegistry} to
>>>>>>> branch '1.8' of https://github.com/WorldSEnder/MCAnm
	 * cache it.<br>
	 * This does also NOT bake the model, you have to do that later on
	 *
	 * @param resLocation
	 *            the {@link ResourceLocation} to load from
	 * @param resManager
	 *            the {@link IResourceManager} to use during loading. You may
	 *            use <code>Minecraft.getMinecraft().getResourceManager()</code>
	 *            .
	 * @return the loaded model, never null, may contain no data
	 * @throws ModelFormatException
	 *             if any exception occurs during loading
	 */
	public ModelMCMD loadUnchecked(ResourceLocation resLocation,
			IResourceManager resManager) throws ModelFormatException {
		MCAnm.logger.trace(String.format(
				"[Model] Attempting to load model from resource: %s",
				resLocation));
		RawData loadedData = VersionizedModelLoader.loadVersionized(
				resLocation, resManager);
		return new ModelMCMD(Optional.of(loadedData));
	}
	/**
	 * ADVANCED USE<br>
	 * Loads the model from an already open {@link DataInputStream}. This method
	 * will - unlike {@link #loadInstance(ResourceLocation)} - throw when a
	 * {@link ModelFormatException} is encountered because it is for advanced
	 * users.<br>
	 * The model is not scheduled for reloading when the user switches his/her
	 * resourcepack.
	 *
	 * @param dis
	 *            the stream to load from
	 * @param filename
	 *            the name of the "file" (for debugging and messages)
	 * @return the loaded model, never null
	 */
	public ModelMCMD loadFromStream(DataInputStream dis, String filename)
			throws ModelFormatException {
		MCAnm.logger.trace(String.format(
				"[Model] Attempting to load model from stream: %s", filename));
		RawData loadedData = VersionizedModelLoader.loadVersionized(dis,
				filename);
		ModelMCMD model = new ModelMCMD(Optional.of(loadedData));
		model.preBake();
		return model;
	}
	@Override
	public void onResourceManagerReload(IResourceManager newManager) {
		manager = newManager;
	}
}
