package com.github.worldsender.mcanm.client.model.mcanmmodel.animation.stored;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.worldsender.mcanm.client.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.client.model.mcanmmodel.Utils;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public class StoredAnimation implements IAnimation {
	public static final long MAGIC_NUMBER = 0x4d48464320414e4dL;

	/**
	 * Gets the {@link DataInputStream} from the {@link ResourceLocation}, throwing exceptions where appropriate
	 *
	 * @param resLoc
	 *            the resource location to read from
	 * @param manager
	 *            the manager to use to resolve the resource
	 * @return a {@link DataInputStream} to read from
	 * @throws ModelFormatException
	 */
	private static DataInputStream getStream(ResourceLocation resLoc, IResourceManager manager)
			throws ModelFormatException {
		if (manager == null)
			throw new IllegalArgumentException("Manager can't be null");
		try {
			return new DataInputStream(manager.getResource(resLoc).getInputStream());
		} catch (IOException ioe) {
			throw new ModelFormatException(String.format("Can't read from resource %s.", resLoc), ioe);
		}
	}

	private final ResourceLocation origin;
	private final Map<String, AnimatedTransform> animations;

	/**
	 * Reads an Animation from the given {@link ResourceLocation}. Exceptions in the file-format will be thrown.
	 *
	 * @param file
	 *            the file to load from
	 * @throws ModelFormatException
	 *             when the file is not a correct .mhanm-file
	 */
	public StoredAnimation(ResourceLocation file) throws ModelFormatException {
		this(file, Minecraft.getMinecraft().getResourceManager());
	}

	/**
	 *
	 * @param file
	 * @param resManager
	 * @throws ModelFormatException
	 */
	public StoredAnimation(ResourceLocation file, IResourceManager resManager) throws ModelFormatException {
		this(getStream(file, resManager), file);
	}

	/**
	 *
	 * @param dis
	 * @throws ModelFormatException
	 */
	public StoredAnimation(DataInputStream dis) throws ModelFormatException {
		this(dis, null);
	}

	/**
	 *
	 * @param dis
	 * @param src
	 * @throws ModelFormatException
	 */
	private StoredAnimation(DataInputStream dataIn, ResourceLocation src) throws ModelFormatException {
		this.origin = src;
		this.animations = new HashMap<>();
		loadFrom(dataIn, String.valueOf(src));
	}

	/**
	 * Actually loads the model, used in constructor and on reload
	 *
	 * @param dataIn
	 *            data-in stream
	 * @param origin
	 *            debug string
	 * @throws ModelFormatException
	 *             any error
	 */
	private void loadFrom(DataInputStream dataIn, String origin) throws ModelFormatException {
		try (DataInputStream dis = dataIn) {
			long magic = dis.readLong();
			if (magic != MAGIC_NUMBER)
				throw new ModelFormatException("Wrong magic number");
			@SuppressWarnings("unused")
			String artist = Utils.readString(dis);
			int animatedBoneCount = dis.readUnsignedByte();
			for (int i = 0; i < animatedBoneCount; ++i) {
				String boneName = Utils.readString(dis);
				AnimatedTransform boneTransform = new AnimatedTransform(dis);
				this.animations.put(boneName, boneTransform);
			}
		} catch (EOFException eof) {
			throw new ModelFormatException("Unexpected end of stream.", eof);
		} catch (IOException ioe) {
			throw new ModelFormatException(String.format("Error handling resource %s.", origin), ioe);
		} catch (ModelFormatException mfe) {
			throw new ModelFormatException(String.format("Model format error in resource %s.", origin), mfe);
		}
	}

	/**
	 * Reloads the model using the given {@link IResourceManager}. It is an error to reload an animation that was not
	 * loaded from a {@link ResourceLocation} although this method silently ignores this.
	 *
	 * @param resManager
	 *            the {@link IResourceManager} to use to reload the animation
	 */
	public void reload(IResourceManager resManager) {
		if (this.origin == null)
			return;
		this.loadFrom(getStream(origin, resManager), String.valueOf(origin));
	}

	@Override
	public Optional<BoneTransformation> getCurrentTransformation(String bone, float frame) {
		AnimatedTransform anim = this.animations.get(bone);
		if (anim == null) {
			Optional.empty();
		}
		return Optional.of(anim.getTransformAt(frame));
	}

	/**
	 * @return the origin
	 */
	public ResourceLocation getOrigin() {
		return origin;
	}
}
