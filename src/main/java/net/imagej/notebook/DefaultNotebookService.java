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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.imagej.Data;
import net.imagej.DatasetService;
import net.imagej.display.DataView;
import net.imagej.display.DatasetView;
import net.imagej.display.ImageDisplayService;
import net.imagej.notebook.mime.MIMEObject;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.ops.special.inplace.Inplaces;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Util;
import net.imglib2.view.IntervalView;

import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.util.ClassUtils;

/**
 * AWT-driven implementation of {@link NotebookService}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Service.class)
public class DefaultNotebookService extends AbstractService implements
	NotebookService
{

	@Parameter
	private ConvertService convertService;
	
	@Parameter
	private ImageDisplayService ids;
	
	@Parameter
	private DatasetService ds;

	@Parameter
	private OpService ops;

	// -- Service methods --

	@Override
	public void initialize() {
		try {
			// Anything that implements MIMEObject can be displayed.
			BeakerX.register(MIMEObject.class, (map, mimeObj) -> {
				map.put(mimeObj.mimeType(), mimeObj.data());
			});

			// Anything convertible to MIMEObject can also be displayed.
			// TODO: Fix bug in ConvertService#getCompatibleInputClasses.
			// It should check class assignability, not only exact equality.
			final List<Class<?>> mimeFriendlyTypes = //
				convertService.getInstances().stream() //
					.filter(c -> MIMEObject.class.isAssignableFrom(c.getOutputType())) //
					.map(c -> c.getInputType()) //
					.collect(Collectors.toList());
			for (final Class<?> mimeFriendlyType : mimeFriendlyTypes) {
				BeakerX.register(mimeFriendlyType, (map, object) -> {
					final MIMEObject mimeObj = mime(object);
					if (mimeObj != null) map.put(mimeObj.mimeType(), mimeObj.data());
				});
			}
		}
		catch (final NoClassDefFoundError exc) {
			// NB: BeakerX is not available; ignore.
		}
	}

	// -- NotebookService methods --

	@Override
	public Object display(final Object source) {
		final MIMEObject mimeObj = mime(source);
		return mimeObj == null ? source : mimeObj;
	}

	@Override
	public <T extends RealType<T> & NativeType<T>> RandomAccessibleInterval<T>
		mosaic(final int[] gridLayout,
			@SuppressWarnings("unchecked") final RandomAccessibleInterval<T>... images)
	{
		// Count the actual number of image dimensions.
		int numDims = 0;
		for (int i = 0; i < images.length; i++) {
			numDims = Math.max(numDims, images[i].numDimensions());
		}

		// Pad any missing grid dimensions.
		final int[] grid = new int[numDims];
		for (int d = 0; d < numDims; d++) {
			grid[d] = d < gridLayout.length ? gridLayout[d] : 1;
		}

		// Define a buffer for holding multidimensional position indices.
		final int[] pos = new int[numDims];

		// Compute grid box extents (width, height, etc.).
		final long[][] extents = new long[numDims][];
		for (int d = 0; d < numDims; d++)
			extents[d] = new long[grid[d]];
		for (int i = 0; i < images.length; i++) {
			IntervalIndexer.indexToPosition(i, grid, pos);
			for (int d = 0; d < numDims; d++) {
				if (pos[d] < grid[d]) {
					extents[d][pos[d]] = //
						Math.max(extents[d][pos[d]], images[i].dimension(d));
				}
			}
		}

		// Compute grid box offsets.
		final long[][] offsets = new long[numDims][];
		for (int d = 0; d < numDims; d++)
			offsets[d] = new long[grid[d] + 1];
		for (int d = 0; d < numDims; d++) {
			for (int g = 0; g < grid[d]; g++) {
				offsets[d][g + 1] = offsets[d][g] + extents[d][g];
			}
		}

		// Compute total mosaic dimensions.
		final long[] mosaicDims = new long[numDims];
		for (int d = 0; d < numDims; d++)
			mosaicDims[d] = offsets[d][offsets[d].length - 1];
		final FinalInterval mosaicBox = new FinalInterval(mosaicDims);

		final Img<T> result = //
			ops.create().img(mosaicBox, Util.getTypeFromInterval(images[0]));

		for (int i = 0; i < images.length; i++) {
			IntervalIndexer.indexToPosition(i, grid, pos);

			// Skip images which will not appear on the grid.
			boolean outOfBounds = false;
			for (int d = 0; d < numDims; d++) {
				if (pos[d] >= grid[d]) {
					outOfBounds = true;
					break;
				}
			}
			if (outOfBounds) continue;

			// Translate the origin of each image to match its position in the mosaic.
			final long[] offset = new long[numDims];
			for (int d = 0; d < numDims; d++)
				offset[d] = offsets[d][pos[d]];
			final IntervalView<T> translated = //
				ops.transform().translateView(ops.transform().zeroMinView(images[i]),
					offset);

			// Declare that all values outside the interval proper will be 0.
			// If we do not perform this step, we will get an error when querying
			// out-of-bounds coordinates.
			final RandomAccessible<T> extended = ops.transform().extendZeroView(
				translated);

			// Define the interval of the image to match the size of the mosaic.
			final RandomAccessibleInterval<T> expanded = //
				ops.transform().intervalView(extended, mosaicBox);

			// Add the full-size zero-padded translated image into the mosaic.
			Inplaces.binary1(ops, Ops.Math.Add.class, result, expanded).mutate1(
				result, expanded);
		}

		// TODO: Some day, use Views.arrange, Views.tile or Views.combine instead.
		return result;
	}

	@Override
	public NotebookTable methods(final Class<?> type, final String prefix) {
		final NotebookTable table = new NotebookTable();

		final Method[] methods = type.getMethods();
		// NB: Methods are returned in inconsistent order.
		Arrays.sort(methods, (m1, m2) -> {
			final int nameComp = m1.getName().compareTo(m2.getName());
			if (nameComp != 0) return nameComp;
			final int pCount1 = m1.getParameterCount();
			final int pCount2 = m2.getParameterCount();
			if (pCount1 != pCount2) return pCount1 - pCount2;
			final Class<?>[] pTypes1 = m1.getParameterTypes();
			final Class<?>[] pTypes2 = m2.getParameterTypes();
			for (int i = 0; i < pTypes1.length; i++) {
				final int typeComp = ClassUtils.compare(pTypes1[i], pTypes2[i]);
				if (typeComp != 0) return typeComp;
			}
			return ClassUtils.compare(m1.getReturnType(), m2.getReturnType());
		});

		for (final Method m : methods) {
			final String name = m.getName();
			if (!name.startsWith(prefix)) continue;

			final Class<?>[] paramTypes = m.getParameterTypes();
			final List<String> args = Arrays.asList(paramTypes).stream().map(//
				c -> c.getName() //
			).collect(Collectors.toList());
			String arguments = args.toString();
			arguments = arguments.substring(1, arguments.length() - 1);
			if (arguments.isEmpty()) arguments = "<none>";

			final String returns = m.getReturnType().getName();

			table.addRow(//
				"name", name, //
				"arguments", arguments, //
				"returns", returns //
			);
		}
		return table;
	}
	
	@Override
	public <T extends RealType<T>> DatasetView viewRealType(
		final RandomAccessibleInterval<T> source)
	{
		Data data = ds.create(source);
		DataView view = ids.createDataView(data);
		if (!(view instanceof DatasetView))
			throw new IllegalArgumentException(
				"source image cannot be cast to a DatasetView");
		DatasetView datasetView = (DatasetView) view;
		return datasetView;
	}

	// -- Helper methods --

	private MIMEObject mime(final Object object) {
		return convertService.convert(object, MIMEObject.class);
	}
}
