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
 */
public interface NotebookService extends ImageJService {

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
			source.dimensionIndex(Axes.CHANNEL));
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
		return display(source, 0, 1, -1);
	}

	/**
	 * Converts the given image to a form renderable by scientific notebooks.
	 * 
	 * @param source The image to render.
	 * @param xAxis The image dimension to use for the X axis.
	 * @param yAxis The image dimension to use for the Y axis.
	 * @param cAxis The image dimension to use for compositing multiple channels.
	 * @return an object that the notebook knows how to draw onscreen.
	 */
	<T extends RealType<T>> Object display(RandomAccessibleInterval<T> source,
		int xAxis, int yAxis, int cAxis);

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
}
