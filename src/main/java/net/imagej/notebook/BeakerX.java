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

import com.twosigma.beakerx.mimetype.MIMEContainer;
import com.twosigma.beakerx.util.Images;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import jupyter.Displayer;
import jupyter.Displayers;

/**
 * Helper class to isolate the BeakerX dependencies.
 *
 * @author Curtis Rueden
 */
class BeakerX {

	public interface DisplayerPopulator<T> {

		void populate(Map<String, String> map, T object) throws Exception;
	}

	public static <T> void register(final Class<T> clazz,
		final DisplayerPopulator<T> populator)
	{
		Displayers.register(clazz, new Displayer<T>() {

			@Override
			public Map<String, String> display(final T object) {
				final HashMap<String, String> m = new HashMap<>();
				try {
					populator.populate(m, object);
				}
				catch (final Exception exc) {
					final StringWriter sw = new StringWriter();
					exc.printStackTrace(new PrintWriter(sw));
					m.put(MIMEContainer.MIME.TEXT_HTML, "<div><pre>" + sw.toString() +
						"</pre></div>");
				}
				return m;
			}
		});
	}

	public static String base64(final RenderedImage image) throws IOException {
		final byte[] data = Images.encode(image);
		return Base64.getEncoder().encodeToString(data);
	}

}
