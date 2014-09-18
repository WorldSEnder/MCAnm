package com.github.worldsender.mcanm.client.model.mhfcmodel;

import java.io.DataInputStream;
import java.io.IOError;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustomLoader;
import net.minecraftforge.client.model.ModelFormatException;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.model.mhfcmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mhfcmodel.loader.VersionizedModelLoader;

/**
 * This class can be used to load a model without registering it automatically
 * in the {@link ModelRegistry}. This can be useful if you later want to load
 * the model with different data.
 *
 * @author WorldSEnder
 *
 */
public class MCMDModelLoader implements IModelCustomLoader {
	private static String[] suffixList = {".mhmd"};

	/**
	 * This instance is registered in the {@link AdvancedModelLoader}-class
	 * provided by forge.
	 */
	public static final MCMDModelLoader instance = new MCMDModelLoader();

	@Override
	public String getType() {
		return "MHFC";
	}

	@Override
	public String[] getSuffixes() {
		return suffixList;
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
	public static ModelMCMD loadModel(ResourceLocation resLoc) {
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
	@Override
	public ModelMCMD loadInstance(ResourceLocation resource) {
		return this.loadInstance(resource, Minecraft.getMinecraft()
				.getResourceManager(), true);
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
		// when that is null.
		ModelMCMD model = new ModelMCMD(resource, resManager);
		if (register) {
			ModelRegistry.instance.registerModel(model);
		}
		return model;
	}
	/**
	 * ADVANCED USE<br>
	 * Use this to load a Model and receive any occuring Exceptions. The
	 * received model can later be registered in the {@link ModelRegistry} to
	 * cache it.
	 *
	 * @param resLocation
	 *            the {@link ResourceLocation} to load from
	 * @param resManager
	 *            the {@link IResourceManager} to use during loading. You may
	 *            use <code>Minecraft.getMinecraft().getResourceManager()</code>
	 *            .
	 * @return the loaded model
	 * @throws ModelFormatException
	 *             if any exception occurs during loading
	 */
	public ModelMCMD loadChecked(ResourceLocation resLocation,
			IResourceManager resManager) throws ModelFormatException {
		MCAnm.logger.trace(String.format(
				"[Model] Attempting to load model from resource: %s",
				resLocation));
		RawData loadedData = VersionizedModelLoader.loadVersionized(
				resLocation, resManager);
		return new ModelMCMD(loadedData);
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
		return new ModelMCMD(loadedData);
	}
}
