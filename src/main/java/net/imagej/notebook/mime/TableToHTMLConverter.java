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
		boolean rowLabels = false;
		final StringBuilder sb = new StringBuilder();

		// Add headings.
		for (int i = 0; i < table.getColumnCount(); i++) {
			sb.append("<th>" + html(table.getColumnHeader(i)) + "</th>");
		}
		sb.append("</tr></thead><tbody>");

		// Add rows.
		for (int i = 0; i < table.getRowCount(); i++) {
			final String rowHeader = table.getRowHeader(i);
			if (rowHeader != null) rowLabels = true;
			sb.append("<tr><td class =\"rowLabel\">");
			sb.append(rowHeader == null ? "&nbsp;" : rowHeader);
			sb.append("</td>");
			for (int j = 0; j < table.getColumnCount(); j++) {
				sb.append("<td>" + html(table.get(j, i)) + "</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</tbody></table>");

		return tableStart(rowLabels) + sb;
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

	/** Unique ID for every table class produced. */
	private static long id = 0;

	/** Gets the start of the table markup, including style and initial tags. */
	private static String tableStart(final boolean displayRowLabel) {
		final String tableClass = "table.scijava" + id++;
		final String rowLabelStyle = displayRowLabel ? "" : //
			tableClass + " td.rowLabel, " + tableClass + " th.rowLabel {display: none;}";
		return "<style>" +
			tableClass + " {color: #333; font-family: Helvetica, Arial, sans-serif; border-collapse: collapse; border-spacing: 0;}" +
			tableClass + " td, " + tableClass + " th {border: 1px solid #C9C7C7;}" +
			tableClass + " th, " + tableClass + " td.rowLabel {background: #626262; color: #FFFFFF; font-weight: bold; text-align: left;}" +
			tableClass + " td {text-align: left;}" +
			tableClass + " tr:nth-child(even) {background: #F3F3F3;}" +
			tableClass + " tr:nth-child(odd) {background: #FFFFFF;}" +
			tableClass + " tbody tr:hover {background: #BDF4B5;}" + //
			rowLabelStyle + "</style><table class =\"" + tableClass +
			"\"><thead><tr>" + "<th class=\"rowLabel\">&nbsp;</th>";
	}
}
