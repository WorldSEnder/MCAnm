package com.github.worldsender.mcanm.common.resource;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Use with extreme care. This resource location will only ever resolve the DataInputStream *exactly* once.
 * 
 * @author WorldSEnder
 *
 */
public class RawResourceLocation extends ResourceLocationAdapter {
	private DataInputStream dis;
	private String name;

	public RawResourceLocation(DataInputStream dis) {
		this("<raw stream>", dis);
	}

	public RawResourceLocation(String name, DataInputStream dis) {
		this.dis = Objects.requireNonNull(dis);
		this.name = Objects.requireNonNull(name);
	}

	/**
	 * Checks if the stream is still available. NOT THREAD-SAFE by itself
	 * 
	 * @throws NoSuchFileException
	 */
	private void checkConsumed() throws NoSuchFileException {
		if (dis == null) {
			throw new NoSuchFileException("<already consumed stream>");
		}
	}

	private DataInputStream consumeStream() throws IOException {
		checkConsumed();
		synchronized (dis) {
			checkConsumed(); // Double-tap
			DataInputStream d = dis;
			dis = null;
			return d;
		}
	}

	@Override
	public boolean shouldCache() {
		return false;
	}

	@Override
	public void registerReloadListener(Consumer<IResourceLocation> reloadListener) {
		reloadListener.accept(this);
	}

	@Override
	public IResource open() throws IOException {
		return new ResourceAdapter(this, consumeStream()) {
		};
	}

	@Override
	public String getResourceName() {
		return name;
	}

}
