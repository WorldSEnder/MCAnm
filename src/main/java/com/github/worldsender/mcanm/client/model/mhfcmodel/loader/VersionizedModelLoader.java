package com.github.worldsender.mcanm.client.model.mhfcmodel.loader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.worldsender.mcanm.client.model.mhfcmodel.Utils;
import com.github.worldsender.mcanm.client.model.mhfcmodel.data.RawData;
import com.github.worldsender.mcanm.client.model.mhfcmodel.data.RawDataV1;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;

public abstract class VersionizedModelLoader {
	public static final long magic = 0x4d484643204d444cL; // = MHMD
	private static Map<Integer, VersionizedModelLoader> registeredLoaders = new HashMap<>();

	static {
		registerLoader(1, LoaderVersion1.instance);
	}
	/**
	 * I don't think we ever gonna reach this number but version will be
	 * interpreted as unsigned here.
	 *
	 * @param version
	 *            the version to register the loader for
	 * @param vml
	 *            the loader to register
	 */
	public static boolean registerLoader(int version, VersionizedModelLoader vml) {
		return registeredLoaders.put(version, vml) != null;
	}
	/**
	 * Interprets the resource location as a file with a version. The correct
	 * loader is selected and the data is loaded. After this operation the data
	 * will follow the specified format.
	 *
	 * @param resLocation
	 * @return
	 */
	public static RawData loadVersionized(ResourceLocation resLocation,
			IResourceManager resourceManager) throws ModelFormatException {
		if (resourceManager == null)
			throw new IllegalArgumentException(
					"Resource-Manager can't be null.");
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(
				resourceManager.getResource(resLocation).getInputStream()))) {
			return loadFromStream(dis, resLocation, resLocation.toString());
		} catch (IOException ioe) {
			throw new ModelFormatException(String.format(
					"Can't open resource %s", resLocation), ioe);
		}
	}
	/**
	 * FOR INTERNAL USE ONLY. Loads the model from an already opened
	 * {@link DataInputStream}. This allows possibly for server-hosted models.
	 *
	 * @param stream
	 *            the stream to read from
	 * @param filename
	 *            the name that is found in debug messages
	 * @return the read (raw) data.
	 * @throws ModelFormatException
	 *             if the inputstream is malformed
	 */
	public static RawData loadVersionized(DataInputStream dis, String filename)
			throws ModelFormatException {
		if (dis == null)
			throw new IllegalArgumentException(
					"DataInputStream can not be null");
		return loadFromStream(dis, null, filename);

	}
	/**
	 * Helper method
	 *
	 * @throws IOException
	 * @throws ModelFormatException
	 */
	private static RawData loadFromStream(DataInputStream dis,
			ResourceLocation originalLocation, String streamName)
			throws ModelFormatException {
		try {
			long foundMagic = dis.readLong();
			if (foundMagic != magic) {
				throw new ModelFormatException(String.format(
						"Wrong magic number. Found %x, expected %x.",
						foundMagic, magic));
			}

			UUID uuid = new UUID(dis.readLong(), dis.readLong());
			String artist = Utils.readString(dis);
			int version = dis.readInt();
			VersionizedModelLoader loader = registeredLoaders.get(version);
			if (loader == null)
				throw new ModelFormatException("Unrecognized model version.");
			RawData meta = new RawData(uuid, artist, originalLocation);
			RawData data = loader.loadFromInputStream(meta, version, dis);
			return data;
		} catch (EOFException eofe) {
			throw new ModelFormatException(String.format(
					"Unexpected end of file (%s).", streamName), eofe);
		} catch (IOException ioe) {
			throw new ModelFormatException(String.format(
					"Can't read from stream given (%s).", streamName), ioe);
		} catch (ModelFormatException mfe) {
			throw new ModelFormatException(String.format(
					"Model format exception in %s", streamName), mfe);
		}
	}
	/**
	 * Actually loads the inputstream into the {@link RawDataV1} format. This
	 * method should deal with all restrictions (names, etc.) and throw a
	 * ModelFormatException if the input violates any rules.
	 *
	 * @param meta
	 *            this {@link RawData} contains meta data that should be cloned
	 *            when constructing the read data (things like the artist, the
	 *            model's UUID)
	 * @param version
	 *            the version of the file if one handler is registered for
	 *            multiple versions
	 * @param is
	 *            the input stream to load from
	 * @return a fully loaded {@link RawDataV1}
	 * @throws IOException
	 *             when either the file is too short or another IOException
	 *             occurs
	 * @throws ModelFormatException
	 *             when the input stream is not conform to the specified format
	 */
	public abstract RawData loadFromInputStream(RawData meta, int version,
			DataInputStream is) throws IOException, ModelFormatException;
}
