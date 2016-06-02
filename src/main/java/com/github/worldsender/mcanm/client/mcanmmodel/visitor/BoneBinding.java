package com.github.worldsender.mcanm.client.mcanmmodel.visitor;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import com.github.worldsender.mcanm.client.mcanmmodel.stored.RawDataV1;
import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

public class BoneBinding {
	/** To be interpreted as unsigned */
	public byte boneIndex;
	public float bindingValue;

	public static BoneBinding[] readMultipleFrom(DataInputStream di) throws IOException {
		BoneBinding[] bindings = new BoneBinding[RawDataV1.MAX_NBR_BONEBINDINGS];
		int bindIndex;
		int i = 0;
		while (i < RawDataV1.MAX_NBR_BONEBINDINGS && (bindIndex = di.readUnsignedByte()) != 0xFF) {
			BoneBinding binding = new BoneBinding();
			// Read strength of binding
			float bindingValue = di.readFloat();
			if (Math.abs(bindingValue) > 100.0F || bindingValue < 0)
				throw new ModelFormatException(
						String.format(
								"Value for binding seems out of range: %f (expected to be in [0, 100]",
								bindingValue));
			// Apply attributes
			binding.boneIndex = (byte) bindIndex;
			binding.bindingValue = bindingValue;
			bindings[i++] = binding;
		}
		return Arrays.copyOf(bindings, i);
	}
}
