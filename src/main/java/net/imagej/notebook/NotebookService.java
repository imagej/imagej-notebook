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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.axis.Axes;
import net.imagej.display.DatasetView;
import net.imagej.display.DefaultDatasetView;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.scijava.table.Tables;

/**
 * Interface for services which provide handy methods for working with
 * scientific notebook software (e.g.,
 * <a href="http://beakernotebook.com/">Beaker Notebook</a>).
 *
 * @author Curtis Rueden
 */
public interface NotebookService extends ImageJService {

	/** Strategy to use for scaling the image intensity values. */
	enum ValueScaling {
		/**
		 * Scales the display according to a "best effort": "narrow" types with few
		 * sample values (e.g., {@code bit}, {@code uint2}, {@code uint4} and
		 * {@code uint8}) are scaled according to the {@code FULL} strategy, whereas
		 * "wide" types with many possible values (e.g., {@code uint16},
		 * {@code float32} and {@code float64}) are scaled according to the
		 * {@code DATA} strategy.
		 * <p>
		 * That rationale is that people are accustomed to seeing narrow image types
		 * rendered across the full range, whereas wide image types typically do not
		 * empass the entire range of the type and rendering them as such results in
		 * image which appear all or mostly black or gray.
		 * </p>
		 */
		AUTO,

		/**
		 * Scales the display to match the bounds of the data type. For example,
		 * {@code uint8} will be scaled to 0-255, regardless of the actual data
		 * values.
		 */
		FULL,

		/**
		 * Scales the display to match the actual min and max values of the data.
		 * For example, a {@code uint16} dataset with sample values ranging between
		 * 139 and 3156 will map 139 to minimum intensity and 3156 to maximum
		 * intensity.
		 */
		DATA
	}

	/**
	 * Converts the given object to a form renderable by scientific notebooks.
	 *
	 * @param source The object to render.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	Object display(Object source);

	/**
	 * Converts the given map data to a form renderable by scientific notebooks.
	 * <p>
	 * The map is treated as a single-column table, with each key of the map
	 * defining the row header for a particular row, and the corresponding value
	 * containing the cell of data. See {@link Tables#wrap(Map, String)}.
	 * </p>
	 *
	 * @param map Map data to render.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default Object display(final Map<?, ?> map) {
		return display(map, null);
	}

	/**
	 * Converts the given map data to a form renderable by scientific notebooks.
	 * See {@link #display(Map, String)} for details.
	 * <p>
	 * The map is treated as a single-column table, with each key of the map
	 * defining the row header for a particular row, and the corresponding value
	 * containing the cell of data. See {@link Tables#wrap(Map, String)}.
	 * </p>
	 * 
	 * @param map Map data to render.
	 * @param colHeader Table column header label. Pass null for no column header.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T> Object display(final Map<?, T> map, final String colHeader) {
		final Object table = Tables.wrap(map, colHeader);
		return display(table);
	}

	/**
	 * Converts the given list data to a form renderable by scientific notebooks.
	 * See {@link #display(List, String, List)} for details.
	 *
	 * @param list List data to render.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default Object display(final List<?> list) {
		return display(list, null, null);
	}

	/**
	 * Converts the given list data to a form renderable by scientific notebooks.
	 * See {@link #display(List, String, List)} for details.
	 *
	 * @param list List data to render.
	 * @param colHeader Table column header label. Pass null for no column header.
	 * @return an object that the notebook knows how to draw onscreen.
	 * @see Tables#wrap
	 */
	default Object display(final List<?> list, final String colHeader) {
		return display(list, colHeader, null);
	}

	/**
	 * Converts the given list data to a form renderable by scientific notebooks.
	 * See {@link #display(List, String, List)} for details.
	 *
	 * @param list List data to render.
	 * @param rowHeaders List of table row header labels. Pass null for no row
	 *          headers.
	 * @return an object that the notebook knows how to draw onscreen.
	 * @see Tables#wrap
	 */
	default Object display(final List<?> list, final List<String> rowHeaders) {
		return display(list, null, rowHeaders);
	}

	/**
	 * Converts the given list data to a form renderable by scientific notebooks.
	 * <p>
	 * Three sorts of lists are supported:
	 * </p>
	 * <ol>
	 * <li>List of lists - If the first element is itself a list, a sequence of
	 * tables is produced. Each element of the list is processed recursively, with
	 * each item of the list defining its own structure.</li>
	 * <li>List of maps - If the first element is a map, that map's keys define
	 * the columns, with map values containing the cell data. See
	 * {@link Tables#wrap(List, List)}.</li>
	 * <li>List of other - Otherwise, the list is considered to define a
	 * single-column table, with each element of the list containing one row/cell
	 * of data. See {@link Tables#wrap(List, String, List)}.</li>
	 * </ol>
	 *
	 * @param list List data to render; see above for details.
	 * @param colHeader Table column header label. Pass null for no column header.
	 * @param rowHeaders List of table row header labels. Pass null for no row
	 *          headers.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T> Object display(final List<T> list, final String colHeader,
		final List<String> rowHeaders)
	{
		final Object table; // an org.scijava.table.Table
		if (list.isEmpty()) {
			// Empty table.
			table = Tables.wrap(list, colHeader, rowHeaders);
		}
		else if (list.get(0) instanceof List) {
			// List of elements; process recursively.
			@SuppressWarnings("unchecked")
			final List<List<?>> data = (List<List<?>>) list;
			final List<Object> tables = new ArrayList<>(data.size());
			for (final List<?> item : data) {
				final Object renderedItem = display(item, colHeader, rowHeaders);
				tables.add(renderedItem);
			}
			table = tables; // NB: List, not Table.
		}
		else if (list.get(0) instanceof Map) {
			// Multi-column table.
			@SuppressWarnings("unchecked")
			final List<Map<?, T>> data = (List<Map<?, T>>) list;
			table = Tables.wrap(data, rowHeaders);
		}
		else {
			// Single-column table.
			table = Tables.wrap(list, colHeader, rowHeaders);
		}
		return display(table);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 *
	 * @param source The image to render.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default Object display(final Dataset source) {
		return Images.bufferedImage(source);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 *
	 * @param source The image to render.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T extends RealType<T>> Object display(
		final RandomAccessibleInterval<T> source)
	{
		return Images.bufferedImage(source);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 *
	 * @param source The image to render.
	 * @param min The minimum value allowed on the display.
	 * @param max The maximum value allowed on the display.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T extends RealType<T>> Object display(
		final RandomAccessibleInterval<T> source, final double min,
		final double max)
	{
		return Images.bufferedImage(source, min, max);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 *
	 * @param source The image to render.
	 * @param min The minimum value per dimension allowed on the display.
	 * @param max The maximum value per dimension allowed on the display.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T extends RealType<T>> Object display(
		final RandomAccessibleInterval<T> source, final double[] min,
		final double[] max)
	{
		return Images.bufferedImage(source, min, max);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 *
	 * @param source The image to render.
	 * @param xAxis The image dimension to use for the X axis.
	 * @param yAxis The image dimension to use for the Y axis.
	 * @param cAxis The image dimension to use for compositing multiple channels,
	 *          or -1 for no compositing.
	 * @param scaling Value scaling strategy; see {@link ValueScaling}.
	 * @param pos Dimensional position of the image. Passing null or the empty
	 *          array will display the default (typically the first) position.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T extends RealType<T>> Object display(
		RandomAccessibleInterval<T> source, int xAxis, int yAxis, int cAxis,
		ValueScaling scaling, long... pos)
	{
		return Images.bufferedImage(source, xAxis, yAxis, cAxis, scaling, pos);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
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
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	default <T extends RealType<T>> Object display(
		RandomAccessibleInterval<T> source, int xAxis, int yAxis, int cAxis,
		double[] min, double[] max, long... pos)
	{
		return Images.bufferedImage(source, xAxis, yAxis, cAxis, min, max, pos);
	}

	/**
	 * Organizes the given list of images into an N-dimensional mosaic.
	 * <p>
	 * For example, passing a grid layout of {2, 2} with four images {A, B, C, D}
	 * will result in them being laid out along the first two axes (let's call
	 * them X and Y) in a 2 x 2 grid:
	 * </p>
	 *
	 * <pre>
	 * AB
	 * CD
	 * </pre>
	 * <p>
	 * The images do not need to be of equal size; images will be padded along
	 * each dimension as needed so that everything lines up in a grid. In the
	 * example above, if A and C have different widths, then the first column will
	 * be as wide as the wider of the two. Same for the second column with images
	 * B and D. If A and B have different heights, than the first row will be as
	 * tall as the taller of the two. And same for the second row with images C
	 * and D.
	 * </p>
	 * <p>
	 * Normally, the number of grid cells (i.e., the product of the grid
	 * dimensions) should match the given number of images. However, the algorithm
	 * handles a mismatch in either direction. If the number of grid cells is less
	 * than the number of images, than the excess images are discarded&mdash;i.e.,
	 * they will not appear anywhere in the mosaic. On the other hand, if the
	 * number of grid cells exceeds the given number of images, then some grid
	 * cells will be empty. The cells are filled along the first dimension
	 * fastest, so e.g. a grid layout of {2, 3, 2} will fill as follows: 000, 100,
	 * 010, 110, 020, 120, 001, 101, 011, 111, 021, 121.
	 * </p>
	 *
	 * @param gridLayout Dimensions of the grid.
	 * @param images Images to combine into the mosaic.
	 * @return A single mosaic image, laid out as specified.
	 */
	<T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T> mosaic(
		final int[] gridLayout,
		@SuppressWarnings("unchecked") final RandomAccessibleInterval<T>... images);

	/**
	 * Outputs a table of public methods for the given object.
	 *
	 * @param o The object for which to generate a table of its methods.
	 * @return a table of the object's public methods.
	 */
	default NotebookTable methods(final Object o) {
		if (o == null) return null;
		return methods(o.getClass());
	}

	/**
	 * Outputs a table of public methods for the given object.
	 *
	 * @param o The object for which to generate a table of its methods.
	 * @param prefix The starting characters to use for limiting method names.
	 * @return a table of the object's public methods.
	 */
	default NotebookTable methods(final Object o, final String prefix) {
		if (o == null) return null;
		return methods(o.getClass(), prefix);
	}

	/**
	 * Outputs a table of public methods for the given class.
	 *
	 * @param type The class for which to generate a table of its methods.
	 * @return a table of the class's public methods.
	 */
	default NotebookTable methods(final Class<?> type) {
		return methods(type, "");
	}

	/**
	 * Outputs a table of public methods for the given class.
	 *
	 * @param type The class for which to generate a table of its methods.
	 * @param prefix The starting characters to use for limiting method names.
	 * @return a table of the class's public methods.
	 */
	NotebookTable methods(Class<?> type, String prefix);
	
	/**
	 * Conveniently wraps a {@link RandomAccessibleInterval} into a
	 * {@link DefaultDatasetView}.
	 * 
	 * @param source - the input data
	 * @return a DefaultDatasetView containing the data
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	default DatasetView view(final RandomAccessibleInterval<?> source) {

		final Object element = Util.getTypeFromInterval(source);
		if (element instanceof ARGBType) {
			return viewRealType(Converters.argbChannels(
				(RandomAccessibleInterval<ARGBType>) source, 1, 2, 3));
		}
		else if (element instanceof RealType) {
			return viewRealType((RandomAccessibleInterval<RealType>) source);
		}
		else {
			throw new IllegalArgumentException("Unsupported image type: " + element
				.getClass().getName());
		}
	}

	/**
	 * Conveniently wraps a {@link RandomAccessibleInterval} into a
	 * {@link DefaultDatasetView} and presets the channel ranges to the given
	 * minimum and maximum values.
	 * 
	 * @param source - the input data
	 * @param min - the minimum for the channel ranges
	 * @param max - the maximum for the channel ranges
	 * @return a DefaultDatasetView containing the data
	 */
	default DatasetView view(final RandomAccessibleInterval<?> source,
		final double min, final double max)
	{
		DatasetView output = view(source);
		output.setChannelRanges(min, max);
		output.rebuild();
		return output;
	}

	/**
	 * Conveniently wraps a {@link RandomAccessibleInterval} into a
	 * {@link DefaultDatasetView} and presets the channel ranges to the given
	 * minimum and maximum arrays.
	 * 
	 * @param source - the input data
	 * @param min - the minimum for the channel ranges
	 * @param max - the maximum for the channel ranges
	 * @return a DefaultDatasetView containing the data
	 */
	default DatasetView view(final RandomAccessibleInterval<?> source,
		final double[] min, final double[] max)
	{

		if (min.length < source.numDimensions() || max.length < source
			.numDimensions()) throw new IllegalArgumentException(
				"Channel maximum and minimum arrays do not match image dimensions!");

		DatasetView output = view(source);

		for (int c = 0; c < output.getData().dimension(Axes.CHANNEL); c++) {
			output.setChannelRange(c, min[c], max[c]);
		}

		output.rebuild();
		return output;
	}

	/**
	 * Conveniently wraps a {@link RandomAccessibleInterval} of {@link RealType}
	 * into a {@link DefaultDatasetView} and presets the channel ranges to the
	 * given minimum and maximum arrays.
	 * 
	 * @param source - the input data
	 * @return a DefaultDatasetView containing the data
	 */
	<T extends RealType<T>> DatasetView viewRealType(
		final RandomAccessibleInterval<T> source);
}
