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

package net.imagej.notebook.table;

import java.io.IOException;

import net.imagej.notebook.mime.HTMLObject;
import net.imagej.notebook.mime.MIMEConverter;

import org.apache.commons.text.StringEscapeUtils;
import org.scijava.convert.ConvertService;
import org.scijava.convert.Converter;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.table.Table;

/**
 * Converter from {@link Table} to {@link HTMLObject}.
 *
 * @author Alison Walter
 * @author Curtis Rueden
 */
@Plugin(type = Converter.class)
public class TableToHTMLConverter extends
	MIMEConverter<Table<?, ?>, HTMLObject>
{

	@Parameter
	private ConvertService convertService;

	@Override
	protected HTMLObject convert(final Table<?, ?> table) {
		return () -> html(table);
	}

	// -- Helper methods --

	private String html(final Table<?, ?> table) throws IOException {
		// Check for the presence of row and/or column headers.
		boolean colLabels = false;
		for (int col = 0; col < table.getColumnCount(); col++) {
			if (table.getColumnHeader(col) != null) { colLabels = true; break; }
		}
		boolean rowLabels = false;
		for (int row = 0; row < table.getRowCount(); row++) {
			if (table.getRowHeader(row) != null) { rowLabels = true; break; }
		}

		final StringBuilder sb = new StringBuilder();

		// Begin the table.
		sb.append("<table class =\"scijava\">");

		// Add column headers if present.
		if (colLabels) {
			sb.append("<thead><tr>");
			if (rowLabels) sb.append("<th class=\"rowLabel\">&nbsp;</th>");
			for (int col = 0; col < table.getColumnCount(); col++) {
				sb.append("<th>" + html(table.getColumnHeader(col)) + "</th>");
			}
			sb.append("</tr></thead>");
		}

		// Add rows.
		sb.append("<tbody>");
		for (int row = 0; row < table.getRowCount(); row++) {
			sb.append("<tr>");
			// Add row header if present.
			if (rowLabels) {
				sb.append("<td class =\"rowLabel\">" + //
					html(table.getRowHeader(row)) + "</td>");
			}
			// Add table data.
			for (int col = 0; col < table.getColumnCount(); col++) {
				sb.append("<td>" + html(table.get(col, row)) + "</td>");
			}
			sb.append("</tr>");
		}

		// Terminate the table.
		sb.append("</tbody></table>");

		return sb.toString();
	}

	/** Gets an HTML string representing the given object. */
	private String html(final Object o) throws IOException {
		if (o == null) return "";
		final HTMLObject htmlObj = convertService.convert(o, HTMLObject.class);
		return htmlObj == null ? escape(o.toString()) : htmlObj.data();
	}

	private static String escape(final String text) {
		return StringEscapeUtils.escapeHtml4(text);
	}
}
