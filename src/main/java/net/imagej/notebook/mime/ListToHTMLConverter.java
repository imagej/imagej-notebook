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

package net.imagej.notebook.mime;

import java.io.IOException;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;
import org.scijava.Priority;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Converter from {@link List} to {@link HTMLObject}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class, priority = Priority.VERY_LOW)
public class ListToHTMLConverter extends MIMEConverter<List<?>, HTMLObject> {

	@Parameter
	private ConvertService convertService;

	@Override
	protected HTMLObject convert(final List<?> list) {
		return () -> html(list);
	}

	// -- Helper methods --

	private String html(final List<?> list) throws IOException {
		final StringBuilder sb = new StringBuilder();
		for (final Object item : list)
			sb.append(html(item));

		return sb.toString();
	}

	/** Gets an HTML string representing the given object. */
	private String html(final Object o) throws IOException {
		final HTMLObject htmlObj = convertService.convert(o, HTMLObject.class);
		return htmlObj == null ? escape(o.toString()) : htmlObj.data();
	}

	private static String escape(final String text) {
		return StringEscapeUtils.escapeHtml4(text);
	}
}
