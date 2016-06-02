package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.animation.visitor.IAnimationVisitable;
import com.github.worldsender.mcanm.common.animation.visitor.IAnimationVisitor;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.util.ResourceCache;

public class RawData implements IAnimationVisitable {
	public static final long MAGIC_NUMBER = Utils.asciiToMagicNumber("MHFC ANM");

	public static final RawData MISSING_DATA;

	static {
		MISSING_DATA = new RawData();
		MISSING_DATA.artist = "<unknown>";
		MISSING_DATA.versionized = new IVersionSpecificData() {
			@Override
			public void visitBy(IAnimationVisitor visitor) {}
		};
	}

	private static interface VersionizedModelLoader {
		IVersionSpecificData loadFrom(DataInputStream dis) throws IOException, ModelFormatException;
	}

	private static final ResourceCache<RawData> CACHE = new ResourceCache<>();

	private String artist;
	private IVersionSpecificData versionized;

	private RawData() {}

	@Override
	public void visitBy(IAnimationVisitor visitor) {
		visitor.visitArtist(artist);
		versionized.visitBy(visitor);
		visitor.visitEnd();
	}

	public static RawData retrieveFrom(IResource resource) {
		return CACHE.getOrCompute(resource, r -> {
			try {
				return RawData.loadFrom(r);
			} catch (ModelFormatException mfe) {
				MCAnm.logger().error(String.format("Error loading animation from %s.", r.getResourceName()), mfe);
				return RawData.MISSING_DATA;
			}
		});
	}

	private static RawData loadFrom(IResource resource) throws ModelFormatException {
		DataInputStream dis = resource.getInputStream();
		RawData data = new RawData();

		try {
			long magic = dis.readLong();
			if (magic != MAGIC_NUMBER)
				throw new ModelFormatException(
						String.format("Wrong MAGIC_NUMBER number. Found %x, expected %x.", magic, MAGIC_NUMBER));
			data.artist = Utils.readString(dis);
			// Why did I use a byte here, but an int for all other formats??? -.-
			int version = dis.readUnsignedByte();
			data.versionized = getLoaderForVersion(version).loadFrom(dis);
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
					String.format("Illegal Animation format in %s", resource.getResourceName()),
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
