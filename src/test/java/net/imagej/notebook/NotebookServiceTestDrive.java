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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * A manual visual test of the {@link NotebookService}.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Command.class)
public class NotebookServiceTestDrive<T extends RealType<T> & NativeType<T>>
	implements Command
{

	@Parameter
	private IOService io;

	@Parameter
	private LogService log;

	@Parameter
	private OpService op;

	@Parameter
	private NotebookService nb;

	@Parameter
	private File imageFile;

	@Parameter(label = "Pan images")
	private boolean panImages;

	@Parameter(label = "Scale images")
	private boolean scaleImages;

	public static void main(final String[] args) throws Exception {
		final Context ctx = new Context();
		ctx.service(UIService.class).showUI();
		final CommandService commandService = ctx.service(CommandService.class);
		commandService.run(NotebookServiceTestDrive.class, true).get();
	}

	@Override
	public void run() {
		final Object image;
		try {
			image = io.open(imageFile.getAbsolutePath());
		}
		catch (final IOException exc) {
			log.error(exc);
			return;
		}

		@SuppressWarnings("unchecked")
		final RandomAccessibleInterval<T> rai = (RandomAccessibleInterval<T>) image;

		RandomAccessibleInterval<T> rai1 = rai, rai2 = rai;

		if (panImages) {
			final long dim0 = rai.dimension(0);
			final long dim1 = rai.dimension(1);
			final long dim2 = rai.dimension(2);
			rai1 = op.transform().intervalView(//
				op.transform().translateView(rai1, 50, -20, 0), //
				FinalInterval.createMinSize(50, -20, 0, dim0, dim1, dim2));
			rai2 = op.transform().intervalView(//
				op.transform().translateView(rai2, -80, 150, 0), //
				FinalInterval.createMinSize(-80, 150, 0, dim0, dim1, dim2));
		}

		if (scaleImages) {
			final double[] scale = { 3.5, 3.5, 1 };
			rai1 = op.transform().scaleView(rai1, scale,
				new LanczosInterpolatorFactory<>());
			rai2 = op.transform().scaleView(rai2, scale,
				new NLinearInterpolatorFactory<>());
		}

		@SuppressWarnings("unchecked")
		final RandomAccessibleInterval<T> mosaic = //
			nb.mosaic(new int[] {2}, rai1, rai2);

		show(nb.display(mosaic));
	}

	public static void show(final Object o) {
		final BufferedImage bi = (BufferedImage) o;
		final JFrame f = new JFrame();
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.getContentPane().add(new JLabel(new ImageIcon(bi)));
		f.pack();
		f.setVisible(true);
	}
}
