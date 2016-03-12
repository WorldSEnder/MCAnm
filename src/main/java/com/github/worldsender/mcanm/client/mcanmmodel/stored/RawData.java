package com.github.worldsender.mcanm.client.mcanmmodel.stored;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.util.ExceptionLessFunctions;
import com.github.worldsender.mcanm.common.util.ResourceCache;

/**
 * Represents the model data right after being loaded. No optimizations or offset has happened yet. This is the raw data
 * return from the appropriate loader. <br>
 * This data will then be translated into an {@link IModelData} to be used to render the model.
 *
 * @author WorldSEnder
 *
 */
public class RawData implements IModelVisitable {
	public static final long magic = 0x4d484643204d444cL; // = MHMD
	public static final ResourceCache<RawData> CACHE = new ResourceCache<>();

	public static final RawData MISSING_DATA;

	static {
		MISSING_DATA = new RawData();
		MISSING_DATA.artist = "<unknown>";
		MISSING_DATA.modelUUID = new UUID(0, 0);
		MISSING_DATA.specificData = new IVersionSpecificData() {
			@Override
			public void visitBy(IModelVisitor visitor) {}
		};
	}

	private static interface VersionizedModelLoader {
		IVersionSpecificData loadFrom(DataInputStream dis) throws IOException, ModelFormatException;
	}

	private UUID modelUUID;
	private String artist;
	private IVersionSpecificData specificData;

	private RawData() {}

	@Override
	public void visitBy(IModelVisitor visitor) {
		visitor.visitModelUUID(getModelUUID());
		visitor.visitArtist(getArtist());
		specificData.visitBy(visitor);
	}

	public UUID getModelUUID() {
		return modelUUID;
	}

	public String getArtist() {
		return artist;
	}

	public static RawData retrieveFrom(IResource resource) throws ModelFormatException {
		return CACHE.getOrCompute(resource, ExceptionLessFunctions.uncheckedFunction(RawData::loadFrom));
	}

	public static RawData loadFrom(IResource resource) throws ModelFormatException {
		RawData data = new RawData();
		DataInputStream dis = resource.getInputStream();

		try {
			long foundMagic = dis.readLong();
			if (foundMagic != magic) {
				throw new ModelFormatException(
						String.format("Wrong magic number. Found %x, expected %x.", foundMagic, magic));
			}

			data.modelUUID = new UUID(dis.readLong(), dis.readLong());
			data.artist = Utils.readString(dis);
			int version = dis.readInt();
			VersionizedModelLoader loader = getLoaderForVersion(version);
			data.specificData = loader.loadFrom(dis);
			return data;
		} catch (EOFException eofe) {
			throw new ModelFormatException(
					String.format("Unexpected end of file (%s).", resource.getResourceName()),
					eofe);
		} catch (IOException ioe) {
			throw new ModelFormatException(
					String.format("Can't read from stream given (%s).", resource.getResourceName()),
					ioe);
		} catch (ModelFormatException mfe) {
			throw new ModelFormatException(
					String.format("Model format exception in %s", resource.getResourceName()),
					mfe);
		}
	}

	private static VersionizedModelLoader getLoaderForVersion(int version) {
		switch (version) {
		case 1:
			return RawDataV1::loadFrom;
		default:
			break;
		}
		return o -> {
			throw new ModelFormatException("Unknown version: " + version);
		};
	}
}
