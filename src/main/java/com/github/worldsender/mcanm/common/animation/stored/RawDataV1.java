package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.animation.stored.AnimatedTransform.AnimatedTransformBuilder;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

public class RawDataV1 implements IVersionSpecificData {
	private static final AnimatedTransformBuilder BUILDER = new AnimatedTransformBuilder();

	private Map<String, AnimatedTransform> animations;

	private RawDataV1() {
		animations = new HashMap<>();
	}

	@Override
	public void visitBy(IAnimationVisitor visitor) {
		for (Map.Entry<String, AnimatedTransform> e : animations.entrySet()) {
			visitor.visitBone(e.getKey(), e.getValue());
		}
	}

	public static RawDataV1 readFrom(DataInputStream dis) throws IOException, ModelFormatException {
		RawDataV1 data = new RawDataV1();
		int animatedBoneCount = dis.readUnsignedByte();
		for (int i = 0; i < animatedBoneCount; ++i) {
			String boneName = Utils.readString(dis);
			AnimatedTransform boneTransform = BUILDER.fromStream(dis).buildAndReset();
			data.animations.put(boneName, boneTransform);
		}
		return data;
	}
}
