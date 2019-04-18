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

package net.imagej.notebook.chart;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import net.imagej.axis.Axes;
import net.imagej.space.TypedSpace;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.view.Views;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;

/**
 * Utility methods for calculating histograms and plotting charts.
 *
 * @author Curtis Rueden
 */
public final class Charts {

	private static final Color RED = new Color(0xED4337);
	private static final Color GREEN = new Color(0x90D860);
	private static final Color BLUE = new Color(0x7989FF);

	// TODO: Migrate channel slicing methods to ImageJ Ops.

	public static <T extends RealType<T>> List<RandomAccessibleInterval<T>>
		channels(final RandomAccessibleInterval<T> image)
	{
		final int cAxis = image instanceof TypedSpace ? //
			((TypedSpace<?>) image).dimensionIndex(Axes.CHANNEL) : -1;
		return slices(image, cAxis);
	}

	public static <T extends RealType<T>> List<RandomAccessibleInterval<T>>
		slices(final RandomAccessibleInterval<T> image, int axis)
	{
		if (axis < 0 || axis >= image.numDimensions()) return Arrays.asList(image);
		final long dimSize = image.dimension(axis);
		if (dimSize > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Dimension #" + axis + " of length " +
				dimSize + " is too large to slice");
		}
		final int sliceCount = (int) dimSize;
		final List<RandomAccessibleInterval<T>> slices = new ArrayList<>(sliceCount);
		for (int i = 0; i < sliceCount; i++) {
			slices.add(Views.hyperSlice(image, axis, i));
		}
		return slices;
	}

	public static BufferedImage plot(final Histogram1d<?>... histograms) {
		return plot(Arrays.asList(histograms));
	}

	public static BufferedImage plot(final List<Histogram1d<?>> histograms) {
		final int channelCount = histograms.size();
		final Function<Integer, String> cNames;
		final Function<Integer, Color> colors;
		if (channelCount == 1) {
			// single channel; use grayscale
			cNames = index -> "Counts";
			colors = index -> Color.yellow.darker();
		}
		else if (channelCount == 3) {
			// three channels; use RGB
			final String[] rgbNames = { "red", "green", "blue" };
			final Color[] rgbColors = { RED, GREEN, BLUE };
			cNames = index -> rgbNames[index];
			colors = index -> rgbColors[index];
		}
		else {
			// N channels; linear gradient from red to blue
			final Function<Color, float[]> hsb = c -> Color.RGBtoHSB(RED.getRed(), RED.getGreen(), RED.getBlue(), null);
			float[] hsbRed = hsb.apply(RED);
			float[] hsbBlue = hsb.apply(BLUE);
			cNames = index -> "Channel " + index;
			colors = index -> {
				final float norm = (float) index / (channelCount - 1);
				final float h = hsbRed[0] * (1 - norm) + hsbBlue[0] * norm;
				final float s = hsbRed[1] * (1 - norm) + hsbBlue[1] * norm;
				final float b = hsbRed[2] * (1 - norm) + hsbBlue[2] * norm;
				return new Color(Color.HSBtoRGB(h, s, b));
			};
		}

		// Create a chart.
		final CategoryChart chart = //
				new CategoryChartBuilder()//
				.width(800).height(400)//
				.title("Histogram")//
				.xAxisTitle("Bin").yAxisTitle("Count").build();
		chart.getStyler().setOverlapped(true).setPlotGridVerticalLinesVisible(false);

		for (int c = 0; c < histograms.size(); c++) {
			final Histogram1d<?> histogram = histograms.get(c);

			final Class<?> dataType = histogram.firstDataValue().getClass();
			if (!RealType.class.isAssignableFrom(dataType)) {
				throw new IllegalArgumentException("Unsupported histogram type: " +
					dataType.getName());
			}
			@SuppressWarnings({ "rawtypes", "unchecked" })
			final double[][] data = valuesAndCounts((Histogram1d) histogram);

			chart.addSeries(cNames.apply(c), data[0], data[1]).setFillColor(colors.apply(c));
		}
		return BitmapEncoder.getBufferedImage(chart);
	}

	private static <T extends RealType<T>> double[][] valuesAndCounts(
		final Histogram1d<T> histogram)
	{
		final long size = histogram.size();
		if (size > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Histogram is too large to plot");
		}
		final int binCount = (int) size;

		// Extract values and counts from the histogram.
		final Cursor<LongType> cursor = histogram.cursor();
		final T value = histogram.firstDataValue().createVariable();
		final double[] values = new double[binCount];
		final double[] counts = new double[binCount];
		for (int b = 0; b < binCount; b++) {
			counts[b] = cursor.next().getRealDouble();
			//histogram.getLowerBound(b, value);
			histogram.getCenterValue(b, value);
			values[b] = value.getRealDouble();
		}
		return new double[][] { values, counts };
	}
}
