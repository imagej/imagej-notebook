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

package net.imagej.notebook.image;

import java.io.IOException;

import net.imagej.notebook.Images;
import net.imagej.notebook.mime.HTMLObject;
import net.imagej.notebook.mime.MIMEConverter;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;

import org.scijava.convert.Converter;
import org.scijava.plugin.Plugin;

/**
 * Converter from {@link RandomAccessibleInterval} to {@link HTMLObject}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class)
public class RAIToHTMLConverter extends
	MIMEConverter<RandomAccessibleInterval<?>, HTMLObject>
{

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected HTMLObject convert(final RandomAccessibleInterval<?> image) {
		return () -> {
			final Object element = Util.getTypeFromInterval(image);

			if (element instanceof ARGBType) {
				return encodeARGBTypeImage((RandomAccessibleInterval<ARGBType>) image);
			}
			else if (element instanceof RealType) {
				return encodeRealTypeImage((RandomAccessibleInterval) image);
			}
			else {
				throw new IllegalArgumentException("Unsupported image type: " + //
					element.getClass().getName());
			}
		};
	}

	private static String encodeARGBTypeImage(
		final RandomAccessibleInterval<ARGBType> image) throws IOException
	{
		// NB: ignoring alpha
		return encodeRealTypeImage(Converters.argbChannels(image, 1, 2, 3));
	}

	private static <T extends RealType<T>> String encodeRealTypeImage(
		final RandomAccessibleInterval<T> image) throws IOException
	{
		return Images.html(Images.bufferedImage(image));
	}
}
