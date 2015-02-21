package com.github.worldsender.mcanm.client.model.mcanmmodel.animation.stored;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import com.github.worldsender.mcanm.MCAnm;
import com.github.worldsender.mcanm.client.exceptions.ModelFormatException;
import com.github.worldsender.mcanm.client.model.mcanmmodel.animation.IAnimation;

/**
 * A central place to load animations from. Not only does this class provide the
 * utility of guaranteed no-throw method but also reuses already loaded
 * animations if requested twice.
 *
 * @author WorldSEnder
 *
 */
public class AnimationRegistry implements IResourceManagerReloadListener {
	public static final AnimationRegistry instance = new AnimationRegistry();

	private Map<ResourceLocation, StoredAnimation> registry = new HashMap<>();
	/**
	 * Loads an animation from the given resource location and registers it via
	 * {@link #registerAnimation(StoredAnimation)}.<br>
	 * If an animation for the requested {@link ResourceLocation} has already
	 * been registered the registered one is returned instead.<br>
	 * This method catches {@link ModelFormatException}s and any other occurring
	 * Exceptions and logs them with {@link MCAnm#logger}. This method will then
	 * return <code>null</code> and the animation will not be registered.<br>
	 *
	 * @param resLoc
	 *            the requested {@link ResourceLocation}
	 * @return an animation that has been loaded from that
	 *         {@link ResourceLocation}
	 */
	public static StoredAnimation loadAnimation(ResourceLocation resLoc) {
		return instance.loadAnimation(resLoc, Minecraft.getMinecraft()
				.getResourceManager());
	}
	public static IAnimation loadAnimation(String string) {
		return loadAnimation(new ResourceLocation(string));
	}
	/**
	 * Same as {@link #loadAnimation(ResourceLocation)} but if no animation has
	 * been registered for the given {@link ResourceLocation} it is loaded using
	 * the {@link IResourceManager} given.
	 *
	 * @param resLoc
	 *            the requested {@link ResourceLocation}
	 * @param resManager
	 *            the resource to use to load the animation
	 * @return an animation that has been loaded from that
	 *         {@link ResourceLocation}
	 * @see #loadAnimation(ResourceLocation)
	 */
	public StoredAnimation loadAnimation(ResourceLocation resLoc,
			IResourceManager resManager) {
		try {
			StoredAnimation registeredAnim = registry.get(resLoc);
			if (registeredAnim != null)
				return registeredAnim;
			StoredAnimation newAnim = new StoredAnimation(resLoc, resManager);
			registry.put(resLoc, newAnim);
			return newAnim;
		} catch (ModelFormatException mfe) {
			MCAnm.logger.error(
					String.format("Error loading animation %s", resLoc), mfe);
		}
		return null;
	}
	/**
	 * Registers the given animation in the registry. The animation has to be
	 * constructed with either the {@link #StoredAnimation(ResourceLocation)} or
	 * {@link #StoredAnimation(ResourceLocation, IResourceManager)}, else an
	 * invalid argument exception is being thrown. The animation shall not be
	 * <code>null</code>.<br>
	 * This method does nothing if an animation with the same origin has been
	 * registered before.
	 *
	 * @param animation
	 *            the animation to register
	 * @return true if no animation for the {@link ResourceLocation} from which
	 *         <code>animation</code> has been loaded from has been registered
	 *         before
	 */
	public boolean registerAnimation(StoredAnimation animation) {
		if (animation == null)
			throw new IllegalArgumentException("Animation can't be null");
		if (animation.getOrigin() == null)
			throw new IllegalArgumentException(
					"The animation has to be loaded from a ResourceLocation.");
		if (registry.containsKey(animation.getOrigin())) {
			registry.put(animation.getOrigin(), animation);
			return true;
		}
		return false;
	}
	@Override
	public void onResourceManagerReload(IResourceManager resManager) {
		if (!MCAnm.enableReload.getBoolean())
			return;
		for (StoredAnimation anim : this.registry.values()) {
			anim.reload(resManager);
		}
	}
}
