package com.github.worldsender.mcanm.common.animation.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.worldsender.mcanm.common.Utils;
import com.github.worldsender.mcanm.common.animation.parts.AnimatedTransform;
import com.github.worldsender.mcanm.common.animation.parts.AnimatedTransform.AnimatedTransformBuilder;
import com.github.worldsender.mcanm.common.animation.stored.parts.RawAnimatedBone;
import com.github.worldsender.mcanm.common.animation.visitor.IAnimationVisitor;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

public class RawDataV1 implements IVersionSpecificData {

	private RawAnimatedBone[] animatedBones;

	private RawDataV1() {}

	@Override
	public void visitBy(IAnimationVisitor visitor) {
		for (RawAnimatedBone bone : animatedBones) {
			visitor.visitBone(bone.name, bone.transform);
		}
	}

	public static RawDataV1 loadFrom(DataInputStream dis) throws IOException, ModelFormatException {
		AnimatedTransformBuilder builder = new AnimatedTransformBuilder();

		RawDataV1 data = new RawDataV1();
		int animatedBoneCount = dis.readUnsignedByte();
		data.animatedBones = new RawAnimatedBone[animatedBoneCount];
		Set<String> pastNames = new HashSet<>();
		for (int i = 0; i < animatedBoneCount; ++i) {
			String boneName = Utils.readString(dis);
			if (!pastNames.add(boneName)) {
				throw new ModelFormatException("Duplicate bone name: " + boneName);
			}
			AnimatedTransform boneTransform = builder.fromStream(dis).buildAndReset();
			data.animatedBones[i] = new RawAnimatedBone(boneName, boneTransform);
		}
		return data;
	}
}
