package com.github.worldsender.mcanm.client.model.mhfcmodel.animation;

import static org.lwjgl.opengl.GL11.glScaled;

import com.google.common.base.Predicate;
/**
 * An animatable type that is typically rendered with a
 * {@link RenderAnimatedModel} or a //TODO: RenderMHFCModelTile
 *
 * @author WorldSEnder
 *
 */
public interface IAnimatedObject {
	/**
	 * A scale that gets applied before the object is rendered but after it has
	 * been put into place.<br>
	 *
	 * @author WorldSEnder
	 *
	 */
	public static class Scale {
		protected double scX, scY, scZ;
		/**
		 * This constructor makes a scale object that applies the same scale for
		 * all directions x,y and z.
		 *
		 * @param scale
		 *            the scale
		 */
		public Scale(double scale) {
			this(scale, scale, scale);
		}
		/**
		 * This constructor allows to control each direction.
		 *
		 * @param scaleX
		 *            the scale in x direction
		 * @param scaleY
		 *            the scale in y direction
		 * @param scaleZ
		 *            the scale in z direction
		 */
		public Scale(double scaleX, double scaleY, double scaleZ) {
			this.scX = scaleX;
			this.scY = scaleY;
			this.scZ = scaleZ;
		}
		/**
		 * Applies the scale
		 */
		public void apply() {
			glScaled(scX, scY, scZ);
		}
		/**
		 * Sets the scale uniformly for each dimension
		 *
		 * @param newScale
		 *            the new scale
		 * @see #setScale(double, double, double)
		 */
		public void setScale(double newScale) {
			this.setScale(newScale, newScale, newScale);
		}
		/**
		 * Sets the scale for each direction
		 *
		 * @param newScaleX
		 *            the new x-scale
		 * @param newScaleY
		 *            the new y-scale
		 * @param newScaleZ
		 *            the new z-scale
		 * @see #setScale(double)
		 */
		public void setScale(double newScaleX, double newScaleY,
				double newScaleZ) {
			this.scX = newScaleX;
			this.scY = newScaleY;
			this.scZ = newScaleZ;
		}
		/**
		 * Sets the scale in x direction.
		 *
		 * @param newScale
		 */
		public void setXScale(double newScale) {
			this.scX = newScale;
		}
		/**
		 * Sets the scale in y direction.
		 *
		 * @param newScale
		 */
		public void setYScale(double newScale) {
			this.scY = newScale;
		}
		/**
		 * Sets the scale in z direction.
		 *
		 * @param newScale
		 */
		public void setZScale(double newScale) {
			this.scZ = newScale;
		}
		/**
		 * Rescales this Scale object by multiplying each scale value with
		 * mulScale
		 *
		 * @param mulScale
		 *            the additional scale, multiplicative
		 * @see #rescale(double, double, double)
		 */
		public void rescale(double mulScale) {
			this.rescale(mulScale, mulScale, mulScale);
		}
		/**
		 * Rescales this Scale object by multiplying each scale value with the
		 * appropriate given value
		 *
		 * @param mulScaleX
		 *            the additional scale in x direction
		 * @param mulScaleY
		 *            the additional scale in y direction
		 * @param mulScaleZ
		 *            the additional scale in z direction
		 * @see #rescale(double)
		 */
		public void rescale(double mulScaleX, double mulScaleY, double mulScaleZ) {
			this.scX *= mulScaleX;
			this.scY *= mulScaleY;
			this.scZ *= mulScaleZ;
		}
	}
	/**
	 * A suitable {@link Predicate} to return in
	 * {@link #getPartPredicate(float)} to render all parts without exception.
	 * Note that this will not test if the currently executed animation wants to
	 * display the part.
	 */
	public static final Predicate<String> RENDER_ALL = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return true;
		}
	};
	/**
	 * A suitable {@link Predicate} to return in
	 * {@link #getPartPredicate(float)} to render no parts without exception.
	 * Note that this will not test if the currently executed animation wants to
	 * display the part.
	 */
	public static final Predicate<String> RENDER_NONE = new Predicate<String>() {
		@Override
		public boolean apply(String input) {
			return false;
		}
	};
	public static final Scale NO_SCALE = new Scale(1.0D);
	/**
	 * Gets the animation to play. If you return null here the model will be
	 * displayed in bind pose.
	 *
	 * @return the current attack
	 */
	public IAnimation getCurrentAnimation();
	/**
	 * Returns the current Frame in the animation. This is not inside the
	 * {@link IAnimation} so that each object/entity can decide on its own.
	 */
	public int getCurrentFrame();
	/**
	 * Returns a {@link Scale}-object to scale by before rendering the object.
	 */
	public Scale getScale();
	/**
	 * Returns for a specific subFrame in the current animation a predicate that
	 * get applied all model-parts to test if they should be rendered.
	 *
	 * @param subFrame
	 *            makes the method subFrame sensible
	 *
	 * @return a predicate to match parts against that may be rendered
	 */
	public Predicate<String> getPartPredicate(float subFrame);
}
