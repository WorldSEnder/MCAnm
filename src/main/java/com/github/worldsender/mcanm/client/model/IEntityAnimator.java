package com.github.worldsender.mcanm.client.model;

import com.github.worldsender.mcanm.client.model.util.RenderPassInformation;

import net.minecraft.entity.EntityLiving;

public interface IEntityAnimator<T extends EntityLiving> {
	public static final IEntityAnimator<EntityLiving> STATIC_ANIMATOR = new IEntityAnimator<EntityLiving>() {
		@Override
		public IRenderPassInformation preRenderCallback(
				EntityLiving entity,
				RenderPassInformation buffer,
				float partialTick,
				float uLimbSwing,
				float interpolatedSwing,
				float uRotfloat,
				float headYaw,
				float interpolatedPitch) {
			return buffer;
		};
	};

	@SuppressWarnings("unchecked")
	public static <T extends EntityLiving> IEntityAnimator<T> STATIC_ENTITY() {
		// Actually safe cast, because T is only in, not out
		return (IEntityAnimator<T>) STATIC_ANIMATOR;
	}

	/**
	 * Pre-render callback for the animator.<br>
	 * This method should, when called, fill the given {@link IRenderPassInformation} with the correct values for the
	 * current call and return it.<br>
	 * Additional OpenGL transformations can be safely done, too (matrix is pushed). E.g. resizing the model, etc...
	 *
	 * @param entity
	 *            the entity being rendered
	 * @param buffer
	 *            an {@link IRenderPassInformation} you can write to
	 * @param subFrame
	 *            the current partial frame
	 * @param uLimbSwing
	 *            the current limb-swing
	 * @param interpolatedSwing
	 *            the interpolated limb-swing
	 * @param uRotfloat
	 *            Unknown, just being passed on
	 * @param headYaw
	 *            the current yaw of the entity
	 * @param interpolatedPitch
	 *            the interpolated pitch
	 * @return the {@link IRenderPassInformation} to use for this pass. Most of the time return the passed in buffer
	 *         after setting your values
	 */
	IRenderPassInformation preRenderCallback(
			T entity,
			RenderPassInformation buffer,
			float partialTick,
			float uLimbSwing,
			float interpolatedSwing,
			float uRotfloat,
			float headYaw,
			float interpolatedPitch);
}
