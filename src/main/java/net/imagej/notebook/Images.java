/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2017 - 2018 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.notebook;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

import javax.imageio.ImageIO;

import net.imagej.Dataset;
import net.imagej.axis.Axes;
import net.imagej.display.ColorTables;
import net.imagej.notebook.NotebookService.ValueScaling;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.projector.composite.CompositeXYProjector;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

/**
 * Utility methods for adapting image data structures.
 *
 * @author Curtis Rueden
 * @author Gabriel Selzer
 * @author Dasong Gao
 */
public final class Images {

	private Images() {
		// Prevent instantiation of utility class.
	}

	/**
	 * Converts the given {@link RenderedImage} into a stream of PNG bytes.
	 *
	 * @param image The image to convert to a byte stream.
	 * @return A stream of bytes in PNG format.
	 */
	public static byte[] encode(final RenderedImage image) throws IOException {
		return encode(image, "png");
	}

	/**
	 * Converts the given {@link RenderedImage} into a stream of bytes.
	 *
	 * @param image The image to convert to a byte stream.
	 * @param format The informal name of the format for the returned bytes; e.g.
	 *          "png" or "jpg". See
	 *          {@link ImageIO#getImageWritersByFormatName(String)}.
	 * @return A stream of bytes in the requested format, or null if the image
	 *         cannot be converted to the specified format.
	 */
	public static byte[] encode(final RenderedImage image, final String format)
		throws IOException
	{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final boolean success = ImageIO.write(image, format, baos);
		return success ? baos.toByteArray() : null;
	}

	/**
	 * Converts the given byte array into a {@link BufferedImage}.
	 *
	 * @param data The bytes to convert to an image.
	 * @return A {@link BufferedImage} of the given bytes, or null if an image
	 *         cannot be decoded from the specified data stream.
	 */
	public static BufferedImage decode(final byte[] data) throws IOException {
		return ImageIO.read(new ByteArrayInputStream(data));
	}

	public static String base64(final RenderedImage image) throws IOException {
		return Base64.getEncoder().encodeToString(encode(image));
	}

	public static String html(final RenderedImage image) throws IOException {
		return html(image, null);
	}

	public static String html(final RenderedImage image, final String title)
		throws IOException
	{
		final String titleAttributes = title == null ? "" : //
			"alt=\"" + title + "\" title=\"" + title + "\" ";
		return "<img src=\"data:image/png;charset=utf-8;base64," + //
			base64(image) + "\" " + titleAttributes + "/>";
	}

	/**
	 * Converts the given {@link Dataset} to a {@link BufferedImage}.
	 *
	 * @param source The image to render.
	 * @return {@link BufferedImage} representation.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T extends RealType<T>> BufferedImage bufferedImage(
		final Dataset source)
	{
		return bufferedImage((Img) source, //
			source.dimensionIndex(Axes.X), //
			source.dimensionIndex(Axes.Y), //
			source.dimensionIndex(Axes.CHANNEL), ValueScaling.AUTO);
	}

	/**
	 * Converts the given {@link RandomAccessibleInterval} to a
	 * {@link BufferedImage}.
	 *
	 * @param source The image to render.
	 * @return {@link BufferedImage} representation.
	 */
	public static <T extends RealType<T>> BufferedImage bufferedImage(
		final RandomAccessibleInterval<T> source)
	{
		// NB: Assume <=3 samples in the 3rd dimension means channels. Of course,
		// we have no metadata with a vanilla RAI, but this is a best guess;
		// 3rd dimensions with >3 samples are probably something like Z or time.
		final int cAxis = //
			source.numDimensions() > 2 && source.dimension(2) <= 3 ? 2 : -1;

		return bufferedImage(source, 0, 1, cAxis, ValueScaling.AUTO);
	}

	/**
	 * Converts the given {@link RandomAccessibleInterval} to a
	 * {@link BufferedImage}.
	 *
	 * @param source The image to render.
	 * @param min The minimum value allowed on the display.
	 * @param max The maximum value allowed on the display.
	 * @return {@link BufferedImage} representation.
	 */
	public static <T extends RealType<T>> BufferedImage bufferedImage(
		final RandomAccessibleInterval<T> source, final double min,
		final double max)
	{
		// NB: Assume <=3 samples in the 3rd dimension means channels. Of course,
		// we have no metadata with a vanilla RAI, but this is a best guess;
		// 3rd dimensions with >3 samples are probably something like Z or time.
		final int cAxis = //
			source.numDimensions() > 2 && source.dimension(2) <= 3 ? 2 : -1;
		// This cast to int is safe since we know from the above condition that
		// -1 <= cAxis <= 3
		final int channels = cAxis >= 0 ? (int) source.dimension(cAxis) : 1;

		final double[] minArray = new double[channels];
		final double[] maxArray = new double[channels];

		for (int i = 0; i < minArray.length; i++) {
			minArray[i] = min;
			maxArray[i] = max;
		}

		return bufferedImage(source, 0, 1, cAxis, minArray, maxArray);
	}

	/**
	 * Converts the given {@link RandomAccessibleInterval} to a
	 * {@link BufferedImage}.
	 *
	 * @param source The image to render.
	 * @param min The minimum value per dimension allowed on the display.
	 * @param max The maximum value per dimension allowed on the display.
	 * @return {@link BufferedImage} representation.
	 */
	public static <T extends RealType<T>> Object bufferedImage(
		final RandomAccessibleInterval<T> source, final double[] min,
		final double[] max)
	{
		// NB: Assume <=3 samples in the 3rd dimension means channels. Of course,
		// we have no metadata with a vanilla RAI, but this is a best guess;
		// 3rd dimensions with >3 samples are probably something like Z or time.
		final int cAxis = //
			source.numDimensions() > 2 && source.dimension(2) <= 3 ? 2 : -1;

		return bufferedImage(source, 0, 1, cAxis, min, max);
	}

	/**
	 * Converts the given {@link RandomAccessibleInterval} to a
	 * {@link BufferedImage}.
	 *
	 * @param source The image to render.
	 * @param xAxis The image dimension to use for the X axis.
	 * @param yAxis The image dimension to use for the Y axis.
	 * @param cAxis The image dimension to use for compositing multiple channels,
	 *          or -1 for no compositing.
	 * @param scaling Value scaling strategy; see {@link ValueScaling}.
	 * @param pos Dimensional position of the image. Passing null or the empty
	 *          array will display the default (typically the first) position.
	 * @return {@link BufferedImage} representation.
	 */
	public static <T extends RealType<T>> BufferedImage bufferedImage(
		final RandomAccessibleInterval<T> source, final int xAxis, final int yAxis,
		final int cAxis, final ValueScaling scaling, final long... pos)
	{
		final double min, max;
		final boolean full = scaling == ValueScaling.FULL || //
			scaling == ValueScaling.AUTO && isNarrowType(source);

		final T firstElement = Views.iterable(source).firstElement();

		if (full) {
			// scale the intensities based on the full range of the type
			min = firstElement.getMinValue();
			max = firstElement.getMaxValue();
		}
		else {
			// scale the intensities based on the sample values
			final IterableInterval<T> ii = Views.flatIterable(source);
			final T tMin = ii.firstElement().createVariable();
			final T tMax = tMin.createVariable();
			ComputeMinMax.computeMinMax(source, tMin, tMax);
			min = tMin.getRealDouble();
			max = tMax.getRealDouble();
		}

		// create arrays from generated min/max
		final int arraySize = cAxis >= 0 ? (int) source.dimension(cAxis) : 1;
		final double[] minArray = new double[arraySize];
		final double[] maxArray = new double[arraySize];
		for (int i = 0; i < minArray.length; i++) {
			minArray[i] = min;
			maxArray[i] = max;
		}

		return bufferedImage(source, xAxis, yAxis, cAxis, minArray, maxArray, pos);
	}

	/**
	 * Converts the given {@link RandomAccessibleInterval} to a
	 * {@link BufferedImage}.
	 *
	 * @param source The image to render.
	 * @param xAxis The image dimension to use for the X axis.
	 * @param yAxis The image dimension to use for the Y axis.
	 * @param cAxis The image dimension to use for compositing multiple channels,
	 *          or -1 for no compositing.
	 * @param min The minimum value per dimension allowed on the display
	 * @param max The maximum value per dimension allowed on the display
	 * @param pos Dimensional position of the image. Passing null or the empty
	 *          array will display the default (typically the first) position.
	 * @return {@link BufferedImage} representation.
	 */
	public static <T extends RealType<T>> BufferedImage bufferedImage(
		final RandomAccessibleInterval<T> source, final int xAxis, final int yAxis,
		final int cAxis, final double[] min, final double[] max, final long... pos)
	{
		final IntervalView<T> image = Views.zeroMin(source);

		final int w = xAxis >= 0 ? (int) image.dimension(xAxis) : 1;
		final int h = yAxis >= 0 ? (int) image.dimension(yAxis) : 1;
		final int c = cAxis >= 0 ? (int) image.dimension(cAxis) : 1;
		final ARGBScreenImage target = new ARGBScreenImage(w, h);
		final ArrayList<Converter<T, ARGBType>> converters = new ArrayList<>(c);

		if (min.length != c || max.length != c) throw new IllegalArgumentException(
			"clamping arrays must be of the same length as the number of channels!");

		for (int i = 0; i < c; i++) {
			final ColorTable8 lut = c == 1 ? //
				ColorTables.GRAYS : ColorTables.getDefaultColorTable(i);
			converters.add(new RealLUTConverter<T>(min[i], max[i], lut));
		}
		final CompositeXYProjector<T> proj = new CompositeXYProjector<>(image,
			target, converters, cAxis);
		if (pos != null && pos.length > 0) proj.setPosition(pos);
		proj.setComposite(true);
		proj.map();

		return target.image();
	}

	// -- Helper methods --

	private static <T extends RealType<T>> boolean isNarrowType(
		final RandomAccessibleInterval<T> source)
	{
		return Util.getTypeFromInterval(source).getBitsPerPixel() <= 8;
	}
}
