/*-
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2017 Board of Regents of the University of
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

import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

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
			 * Scales the display according to a "best effort": "narrow" types with
			 * few sample values (e.g., {@code bit}, {@code uint2}, {@code uint4} and
			 * {@code uint8}) are scaled according to the {@code FULL} strategy,
			 * whereas "wide" types with many possible values (e.g., {@code uint16},
			 * {@code float32} and {@code float64}) are scaled according to the
			 * {@code DATA} strategy.
			 * <p>
			 * That rationale is that people are accustomed to seeing narrow image
			 * types rendered across the full range, whereas wide image types
			 * typically do not empass the entire range of the type and rendering them
			 * as such results in image which appear all or mostly black or gray.
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
			 * For example, a {@code uint16} dataset with sample values ranging
			 * between 139 and 3156 will map 139 to minimum intensity and 3156 to
			 * maximum intensity.
			 */
			DATA
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 * 
	 * @param source The image to render.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	default Object display(final Dataset source) {
		return display((Img) source, //
			source.dimensionIndex(Axes.X), //
			source.dimensionIndex(Axes.Y), //
			source.dimensionIndex(Axes.CHANNEL), ValueScaling.AUTO);
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
		// NB: Assume <=3 samples in the 3rd dimension means channels. Of course,
		// we have no metadata with a vanilla RAI, but this is a best guess;
		// 3rd dimensions with >3 samples are probably something like Z or time.
		final int cAxis = //
			source.numDimensions() > 2 && source.dimension(2) <= 3 ? 2 : -1;

		return display(source, 0, 1, cAxis, ValueScaling.AUTO);
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
	<T extends RealType<T>> Object display(RandomAccessibleInterval<T> source,
		int xAxis, int yAxis, int cAxis, ValueScaling scaling, long... pos);

	/**
	 * Organizes the given list of images into an N-dimensional mosaic.
	 * <p>
	 * For example, passing a grid layout of {2, 2} with four images {A, B, C, D}
	 * will result in them being laid out along the first two axes (let's call
	 * them X and Y) in a 2 x 2 grid:
	 *
	 * <pre>
	 * AB
	 * CD
	 * </pre>
	 * </p>
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
}
