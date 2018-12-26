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

		// Start table and add extra heading column in case there's row headings
		String htmlTable = startTable();
		htmlTable += appendRowLabelHeading();

		// Add headings
		for (int i = 0; i < table.getColumnCount(); i++) {
			htmlTable += appendHeadings(html(table.getColumnHeader(i)), //
				i == table.getColumnCount() - 1);
		}

		// Add data
		for (int i = 0; i < table.getRowCount(); i++) {
			if (table.getRowHeader(i) != null) rowLabels = true;
			htmlTable += appendRowLabelData(table.getRowHeader(i));
			for (int j = 0; j < table.getColumnCount(); j++) {
				htmlTable += appendData(html(table.get(j, i)), false, //
					j == table.getColumnCount());
			}
		}
		htmlTable += endTable();

		return getTableStyle(rowLabels) + htmlTable;
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

	/** Returns the style tag used to style HTML tables. */
	private static String getTableStyle(final boolean displayRowLabel) {
		final String rowLabelStyle = displayRowLabel ? //
			"table.scijava td.rowLabel, table.scijava th.rowLabel {display: none;}" : "";
		return "<style>" +
			"table.scijava {color: #333; font-family: Helvetica, Arial, sans-serif; border-collapse: collapse; border-spacing: 0;}" +
			"table.scijava td, table.scijava th {border: 1px solid #C9C7C7;}" +
			"table.scijava th, table.scijava td.rowLabel {background: #626262; color: #FFFFFF; font-weight: bold; text-align: left;}" +
			"table.scijava td {text-align: left;}" +
			"table.scijava tr:nth-child(even) {background: #F3F3F3;}" +
			"table.scijava tr:nth-child(odd) {background: #FFFFFF;}" +
			"table.scijava tbody tr:hover {background: #BDF4B5;}" + //
			rowLabelStyle + "</style>";
	}

	private static String startTable() {
		return "<table class =\"scijava\"><thead><tr>";
	}

	private static String appendRowLabelHeading() {
		return "<th class=\"rowLabel\">&nbsp;</th>";
	}

	private static String appendHeadings(final String data, final boolean end) {
		final String html = "<th>" + data + "</th>";
		if (end) return html + "</tr></thead><tbody>";
		return html;
	}

	private static String appendData(final String data, final boolean start,
		final boolean end)
	{
		String html = "";
		if (start) html += "<tr>";
		html += "<td>" + data + "</td>";
		if (end) html += "</tr>";
		return html;
	}

	private static String appendRowLabelData(final String data) {
		String html = "<tr><td class =\"rowLabel\">";

		if (data == null) html += "&nbsp;";
		else html += data;

		html += "</td>";
		return html;
	}

	private static String endTable() {
		return "</tbody></table>";
	}
}
