package com.github.worldsender.mcanm.common.animation.stored.parts;

import java.util.Objects;

import com.github.worldsender.mcanm.common.animation.parts.AnimatedTransform;

public class RawAnimatedBone {
	public final String name;
	public final AnimatedTransform transform;

	public RawAnimatedBone(String name, AnimatedTransform transform) {
		this.name = Objects.requireNonNull(name);
		this.transform = Objects.requireNonNull(transform);
	}
}
