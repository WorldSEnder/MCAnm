package com.github.worldsender.mcanm.common.resource;

import java.io.DataInputStream;
import java.util.Optional;

public class RawResource extends ResourceAdapter {

	public RawResource(DataInputStream dis) {
		super(dis);
	}

	@Override
	public String getResourceName() {
		return "<unknown resource>";
	}

	@Override
	public Optional<IResourceLocation> getOrigin() {
		return Optional.empty();
	}

}
