package com.github.worldsender.mcanm.common.animation.parts;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.github.worldsender.mcanm.common.exceptions.ModelFormatException;

/**
 * A spline is the line between two key-frames k<SUB>1</SUB> and k<SUB>2</SUB>.
 *
 * @author WorldSEnder
 *
 */
public abstract class Spline {
	private static Map<Byte, IEaseInSplineFactory> ease_in_factories;
	private static Map<Byte, IInterpolationSplineFactory> interpolation_factories;
	private static Map<Byte, IEaseOutSplineFactory> ease_out_factories;

	public static final byte EASE_IN_CONST = 0;

	public static final byte INTERPOLATION_CONST = 8;
	public static final byte INTERPOLATION_LINEAR = 9;
	public static final byte INTERPOLATION_BSPLINE = 10;

	public static final byte EASE_OUT_CONST = 16;

	static {
		ease_in_factories = new HashMap<>();
		interpolation_factories = new HashMap<>();
		ease_out_factories = new HashMap<>();
		// Default registry
		registerEaseInFactory(EASE_IN_CONST, ConstantEaseIn.factory);
		registerInterpolateFactory(INTERPOLATION_CONST, ConstantInterpolation.factory);
		registerInterpolateFactory(INTERPOLATION_LINEAR, LinearInterpolation.factory);
		registerInterpolateFactory(INTERPOLATION_BSPLINE, BSplineInterpolation.factory);
		registerEaseOutFactory(EASE_OUT_CONST, ConstantEaseOut.factory);
		// TODO: EASE_OUT_LINEAR
	}

	public static interface IInterpolationSplineFactory {
		/**
		 * This function should construct a new Spline between the two points given the left and right point, reading
		 * additional data from the {@link DataInputStream} given.
		 *
		 * @param left
		 *            the left control-point of this spline
		 * @param right
		 *            the right control-point of this spline
		 * @param additionalData
		 *            read additional data from this
		 * @return the constructed spline. Not <code>null</code>
		 * @throws IOException
		 *             if an {@link IOException} results from reading from the {@link DataInputStream} given
		 */
		public Spline newSpline(Vector2f left, Vector2f right, DataInputStream additionalData) throws IOException;
	}

	public static interface IEaseInSplineFactory {
		/**
		 * This function should construct a new Spline from -infinity to the right point, reading additional data from
		 * the {@link DataInputStream} given.
		 *
		 * @param right
		 *            the right control-point of this spline
		 * @param additionalData
		 *            read additional data from this
		 * @return the constructed spline. Not <code>null</code>
		 * @throws IOException
		 *             if an {@link IOException} results from reading from the {@link DataInputStream} given
		 */
		public Spline newSpline(Vector2f right, DataInputStream additionalData) throws IOException;
	}

	public static interface IEaseOutSplineFactory {
		/**
		 * This function should construct a new Spline from the left point towards infinity, reading additional data
		 * from the {@link DataInputStream} given.
		 *
		 * @param left
		 *            the right control-point of this spline
		 * @param additionalData
		 *            read additional data from this
		 * @return the constructed spline. Not <code>null</code>
		 * @throws IOException
		 *             if an {@link IOException} results from reading from the {@link DataInputStream} given
		 */
		public Spline newSpline(Vector2f left, DataInputStream additionalData) throws IOException;
	}

	/**
	 * Returns if the frame given is actually on this spline.
	 *
	 * @param frame
	 * @return
	 */
	public abstract boolean isInRange(float frame);

	/**
	 * Gets the value of this spline at the given frame. This function should but may not hold for any frame given but
	 * can throw an {@link IllegalArgumentException} in case the frame is illegal. It <b>can not</b> throw if
	 * {@link #isInRange(float)} returns <code>true</code> for the requested frame.
	 *
	 * @param frame
	 *            the frame to get the value at.
	 * @return the value of of this spline at the frame
	 */
	public abstract float getValueAt(float frame);

	/**
	 * Registers an {@link IEaseInSplineFactory} for the descriminator byte given.
	 *
	 * @param descriminator
	 *            the descriminator byte
	 * @param factory
	 *            the factory, can't be <code>null</code>
	 * @return a previously registered factory, if any
	 */
	public static IEaseInSplineFactory registerEaseInFactory(byte descriminator, IEaseInSplineFactory factory) {
		if (factory == null)
			throw new IllegalArgumentException("Factory can't be null");
		return ease_in_factories.put(descriminator, factory);
	}

	/**
	 * Constructs a new spline from the read descriminator byte using previously registered with
	 * {@link #registerEaseInFactory(byte, IEaseInSplineFactory)}. This will be the spline from -infinity to right.x.
	 *
	 * @param descr
	 *            the read descriminator byte
	 * @param right
	 *            right point of this spline
	 * @param dis
	 *            a {@link DataInputStream} to read additional data from
	 * @return
	 * @throws ModelFormatException
	 * @throws IOException
	 */
	public static Spline easeIn(byte descr, Vector2f right, DataInputStream dis)
			throws ModelFormatException,
			IOException {
		IEaseInSplineFactory factory = ease_in_factories.get(descr);
		if (factory == null)
			throw new ModelFormatException("Unknown ease-in mode.");
		return factory.newSpline(right, dis);
	}

	/**
	 * Registers an {@link IInterpolationSplineFactory} for the descriminator byte given.
	 *
	 * @param descriminator
	 *            the descriminator byte
	 * @param factory
	 *            the factory, can't be <code>null</code>
	 * @return a previously registered factory, if any
	 */
	public static IInterpolationSplineFactory registerInterpolateFactory(
			byte descriminator,
			IInterpolationSplineFactory factory) {
		if (factory == null)
			throw new IllegalArgumentException("Factory can't be null");
		return interpolation_factories.put(descriminator, factory);
	}

	/**
	 * Constructs a new spline from the read descriminator byte using previously registered interpolation_factories.
	 * Generally left.x should be less than right.x.
	 *
	 * @param descr
	 *            the read descriminator byte
	 * @param left
	 *            left point of this spline
	 * @param right
	 *            right point of this spline
	 * @param dis
	 *            a {@link DataInputStream} to read additional data from
	 * @return
	 * @throws ModelFormatException
	 * @throws IOException
	 */
	public static Spline interpolating(byte descr, Vector2f left, Vector2f right, DataInputStream dis)
			throws ModelFormatException,
			IOException {
		IInterpolationSplineFactory factory = interpolation_factories.get(descr);
		if (factory == null)
			throw new ModelFormatException("Unknown interpolate mode.");
		return factory.newSpline(left, right, dis);
	}

	/**
	 * Registers an {@link IEaseOutSplineFactory} for the descriminator byte given.
	 *
	 * @param descriminator
	 *            the descriminator byte
	 * @param factory
	 *            the factory, can't be <code>null</code>
	 * @return a previously registered factory, if any
	 */
	public static IEaseOutSplineFactory registerEaseOutFactory(byte descriminator, IEaseOutSplineFactory factory) {
		if (factory == null)
			throw new IllegalArgumentException("Factory can't be null");
		return ease_out_factories.put(descriminator, factory);
	}

	/**
	 * Constructs a new spline from the read descriminator byte using previously registered with
	 * {@link #registerEaseOutFactory(byte, IEaseOutSplineFactory)}. This will be the spline from left.x to positive
	 * infinity.
	 *
	 * @param descr
	 *            the read descriminator byte
	 * @param left
	 *            right point of this spline
	 * @param dis
	 *            a {@link DataInputStream} to read additional data from
	 * @return
	 * @throws ModelFormatException
	 * @throws IOException
	 */
	public static Spline easeOut(byte descr, Vector2f left, DataInputStream dis)
			throws ModelFormatException,
			IOException {
		IEaseOutSplineFactory factory = ease_out_factories.get(descr);
		if (factory == null)
			throw new ModelFormatException("Unknown ease-out mode.");
		return factory.newSpline(left, dis);
	}

	/**
	 * Utility function for reading a sole point from the {@link DataInputStream}.
	 *
	 * @throws IOException
	 *             if an {@link IOException} occurs.
	 */
	public static Vector2f readPoint(DataInputStream dis) throws IOException {
		float x = dis.readFloat();
		float y = dis.readFloat();
		return new Vector2f(x, y);
	}
}
