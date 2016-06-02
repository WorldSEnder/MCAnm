package com.github.worldsender.mcanm.common.skeleton.stored;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitable;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitor;
import com.github.worldsender.mcanm.common.util.ResourceCache;

public class RawData implements ISkeletonVisitable {
	public static final long MAGIC_NUMBER = Utils.asciiToMagicNumber("MHFC SKL");

	public static final RawData MISSING_DATA;

	static {
		MISSING_DATA = new RawData();
		MISSING_DATA.modelUUID = new UUID(0, 0);
		MISSING_DATA.artist = "<unknown>";
		MISSING_DATA.specificData = new IVersionSpecificData() {
			@Override
			public void visitBy(ISkeletonVisitor visitor) {}
		};
	}

	private static final ResourceCache<RawData> CACHE = new ResourceCache<>();

	private static interface VersionizedModelLoader {
		IVersionSpecificData loadFrom(DataInputStream dis) throws IOException, ModelFormatException;
	}

	private UUID modelUUID;
	private String artist;
	private IVersionSpecificData specificData;

	private RawData() {}

	@Override
	public void visitBy(ISkeletonVisitor visitor) {
		specificData.visitBy(visitor);
		visitor.visitEnd();
	}

	public static RawData retrieveFrom(IResource resource) {
		return CACHE.getOrCompute(resource, r -> {
			try {
				return RawData.loadFrom(r);
			} catch (ModelFormatException mfe) {
				MCAnm.logger().error(String.format("Error loading skeleton from %s.", r.getResourceName()), mfe);
				return RawData.MISSING_DATA;
			}
		});
	}

	private static RawData loadFrom(IResource resource) throws ModelFormatException {
		RawData data = new RawData();
		DataInputStream dis = resource.getInputStream();

		try {
			long foundMagic = dis.readLong();
			if (foundMagic != MAGIC_NUMBER) {
				throw new ModelFormatException(
						String.format("Wrong MAGIC_NUMBER number. Found %x, expected %x.", foundMagic, MAGIC_NUMBER));
			}

			data.modelUUID = new UUID(dis.readLong(), dis.readLong());
			data.artist = Utils.readString(dis);
			int version = dis.readInt();
			data.specificData = getLoaderForVersion(version).loadFrom(dis);
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
					String.format("Illegal Skeleton format in %s", resource.getResourceName()),
					mfe);
		}
		return data;
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
