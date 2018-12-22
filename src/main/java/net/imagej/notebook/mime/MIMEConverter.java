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

import org.scijava.convert.AbstractConverter;
import org.scijava.convert.Converter;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.util.Types;

/**
 * Base class for plugins that convert things to {@link MIMEObject}s.
 * 
 * @author Curtis Rueden
 */
public abstract class MIMEConverter<I, O extends MIMEObject> extends
	AbstractConverter<I, O>
{

	@Parameter
	protected LogService log;

	@Override
	public <T> T convert(final Object src, final Class<T> dest) {
		if (!(getInputType().isInstance(src))) //
			throw new IllegalArgumentException(src.getClass().getName() +
				" is not an instance of " + getInputType().getName());
		if (!dest.isAssignableFrom(getOutputType()))
			throw new IllegalArgumentException(dest.getName() +
				" is not assignable from " + getOutputType().getName());
		@SuppressWarnings("unchecked")
		final I typedSrc = (I) src;
		@SuppressWarnings("unchecked")
		final T result = (T) convert(typedSrc);
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<O> getOutputType() {
		return (Class<O>) Types.raw(Types.param(getClass(), Converter.class, 1));
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<I> getInputType() {
		return (Class<I>) Types.raw(Types.param(getClass(), Converter.class, 0));
	}

	protected abstract O convert(final I obj);
}
