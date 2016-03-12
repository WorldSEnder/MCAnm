package com.github.worldsender.mcanm.common.animation.stored;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.worldsender.mcanm.common.animation.IAnimation;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.resource.IResource;
import com.github.worldsender.mcanm.common.util.ReloadableData;

public class StoredAnimation extends ReloadableData<RawData> implements IAnimation {
	private Map<String, AnimatedTransform> animations;

	private class AnimationVisitor implements IAnimationVisitor {
		private final Map<String, AnimatedTransform> visitedAnimations = new HashMap<>();

		@Override
		public void visitArtist(String artist) {}

		@Override
		public void visitBone(String name, AnimatedTransform transform) {
			visitedAnimations.put(name, transform);
		}

		@Override
		public void visitEnd() {
			StoredAnimation.this.animations = visitedAnimations;
		}
	}

	private StoredAnimation(IResource resource) throws ModelFormatException {
		super(
				resource,
				ReloadableData.convertThrowingLoader(RawData::retrieveFrom, "Error loading model from %s."),
				null);
		this.animations = new HashMap<>();
	}

	@Override
	public Optional<BoneTransformation> getCurrentTransformation(String bone, float frame) {
		AnimatedTransform anim = this.animations.get(bone);
		if (anim == null) {
			Optional.empty();
		}
		return Optional.of(anim.getTransformAt(frame));
	}

	@Override
	protected void loadData(RawData data) {
		animations.clear();
		data.visitBy(this.new AnimationVisitor());
	}
}
