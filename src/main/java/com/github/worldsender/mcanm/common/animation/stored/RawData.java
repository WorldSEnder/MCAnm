package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.util.ExceptionLessFunctions;
import com.github.worldsender.mcanm.common.util.ResourceCache;

public class RawData implements IAnimationVisitable {
	public static final long MAGIC_NUMBER = 0x4d48464320414e4dL;
	public static final ResourceCache<RawData> CACHE = new ResourceCache<>();

	public static final RawData MISSING_DATA;

	static {
		MISSING_DATA = new RawData();
		MISSING_DATA.artist = "<unknown>";
		MISSING_DATA.versionized = new IVersionSpecificData() {
			@Override
			public void visitBy(IAnimationVisitor visitor) {}
		};
	}

	private String artist;
	private IVersionSpecificData versionized;

	private RawData() {}

	@Override
	public void visitBy(IAnimationVisitor visitor) {
		visitor.visitArtist(artist);
		versionized.visitBy(visitor);
		visitor.visitEnd();
	}

	public static RawData retrieveFrom(IResource resource) throws ModelFormatException {
		return CACHE.getOrCompute(resource, ExceptionLessFunctions.uncheckedFunction(RawData::loadFrom));
	}

	public static RawData loadFrom(IResource resource) throws ModelFormatException {
		DataInputStream dis = resource.getInputStream();
		RawData data = new RawData();

		try {
			long magic = dis.readLong();
			if (magic != MAGIC_NUMBER)
				throw new ModelFormatException(
						String.format("Wrong magic number. Found %x, expected %x.", magic, MAGIC_NUMBER));
			data.artist = Utils.readString(dis);
			data.versionized = RawDataV1.readFrom(dis);
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

}
