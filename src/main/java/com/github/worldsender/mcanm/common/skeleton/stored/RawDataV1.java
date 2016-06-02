package com.github.worldsender.mcanm.common.skeleton.stored;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.parts.RawBone;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.common.skeleton.visitor.IBoneVisitor;
import com.github.worldsender.mcanm.common.skeleton.visitor.ISkeletonVisitor;

public class RawDataV1 implements IVersionSpecificData {

	private RawBone[] bones;

	@Override
	public void visitBy(ISkeletonVisitor visitor) {
		for (RawBone bone : bones) {
			IBoneVisitor boneVisitor = visitor.visitBone(bone.name);
			if (bone.parent != 0xFF) {
				boneVisitor.visitParent(bone.parent);
			}
			boneVisitor.visitLocalOffset(bone.offset);
			boneVisitor.visitLocalRotation(bone.rotation);
			boneVisitor.visitEnd();
		}
	}

	public static final RawDataV1 loadFrom(DataInputStream dis) throws IOException, ModelFormatException {
		RawDataV1 data = new RawDataV1();

		int nbrBones = dis.readUnsignedByte();
		// Read bones
		RawBone[] bones = new RawBone[nbrBones];
		Set<String> boneNameSet = new HashSet<>();
		for (int i = 0; i < nbrBones; i++) {
			RawBone newBone = RawBone.readBoneFrom(dis);
			if (!boneNameSet.add(newBone.name))
				throw new ModelFormatException("Two bones with same name " + newBone.name);
			bones[i] = newBone;
		}
		readBoneParents(dis, bones); // Structure has to be tree-like

		data.bones = bones;
		return data;
	}

	private static void readBoneParents(DataInputStream di, RawBone[] bones) throws IOException {
		int nbrBones = bones.length;
		for (RawBone bone : bones) {
			int parentIndex = di.readUnsignedByte();
			if (parentIndex != 255 && parentIndex >= nbrBones) {
				throw new ModelFormatException(
						String.format("ParentIndex (%d) has to be smaller than nbrBones (%d).", parentIndex, nbrBones));
			}
			bone.parent = (byte) parentIndex;
		}
	}

}
