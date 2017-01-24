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

import java.util.ArrayList;

import net.imagej.display.ColorTables;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable8;
import net.imglib2.display.projector.composite.CompositeXYProjector;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

/**
 * AWT-driven implementation of {@link NotebookService}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultNotebookService extends AbstractService implements
	NotebookService
{

	@Override
	public <T extends RealType<T>> Object display(
		final RandomAccessibleInterval<T> source, final int xAxis, final int yAxis,
		final int cAxis)
	{
		final long[] offset = new long[source.numDimensions()];
		for (int d = 0; d < offset.length; d++) {
			offset[d] = -source.min(d);
		}
		final IntervalView<T> image = Views.translate(source, offset);

		final int w = xAxis >= 0 ? (int) image.dimension(xAxis) : 1;
		final int h = yAxis >= 0 ? (int) image.dimension(yAxis) : 1;
		final int c = cAxis >= 0 ? (int) image.dimension(cAxis) : 1;
		final ARGBScreenImage target = new ARGBScreenImage(w, h);
		final ArrayList<Converter<T, ARGBType>> converters = new ArrayList<>(c);
		for (int i = 0; i < c; i++) {
			// NB: No autoscaling, for now.
			final double min = image.firstElement().getMinValue();
			final double max = image.firstElement().getMaxValue();
			final ColorTable8 lut = c == 1 ? //
				ColorTables.GRAYS : ColorTables.getDefaultColorTable(i);
			converters.add(new RealLUTConverter<T>(min, max, lut));
		}
		final CompositeXYProjector<T> proj = new CompositeXYProjector<>(image,
			target, converters, cAxis);
		proj.setComposite(true);
		proj.map();
		return target.image();
	}
}
