package com.github.worldsender.mcanm.common.animation;

import java.util.HashMap;
import java.util.Map;

import com.github.worldsender.mcanm.common.animation.parts.AnimatedTransform;
import com.github.worldsender.mcanm.common.animation.stored.RawData;
import com.github.worldsender.mcanm.common.animation.visitor.IAnimationVisitable;
import com.github.worldsender.mcanm.common.animation.visitor.IAnimationVisitor;
import com.github.worldsender.mcanm.common.resource.IResourceLocation;
import com.github.worldsender.mcanm.common.util.ReloadableData;

public class StoredAnimation extends ReloadableData<IAnimationVisitable> implements IAnimation {
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

	public StoredAnimation(IResourceLocation resource) {
		super(resource, RawData::retrieveFrom, RawData.MISSING_DATA);
	}

	@Override
	protected void preInit(Object... args) {
		this.animations = new HashMap<>();
	}

	@Override
	public boolean storeCurrentTransformation(String bone, float frame, BoneTransformation transform) {
		AnimatedTransform anim = this.animations.get(bone);
		if (anim == null) {
			return false;
		}
		anim.storeTransformAt(frame, transform);
		return true;
	}

	@Override
	protected void loadData(IAnimationVisitable data) {
		animations.clear();
		data.visitBy(this.new AnimationVisitor());
	}
}
