/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.2 -- January 1, 2017
**  
**  Copyright (C) 2017 by Alden Leung, Ting-Fung Chan, All rights reserved.
**  Contact:  alden.leung@gmail.com, tf.chan@cuhk.edu.hk
**  Organization:  School of Life Sciences, The Chinese University of Hong Kong,
**                 Shatin, NT, Hong Kong SAR
**  
**  This file is part of OMTools.
**  
**  OMTools is free software; you can redistribute it and/or 
**  modify it under the terms of the GNU General Public License 
**  as published by the Free Software Foundation; either version 
**  3 of the License, or (at your option) any later version.
**  
**  OMTools is distributed in the hope that it will be useful,
**  but WITHOUT ANY WARRANTY; without even the implied warranty of
**  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**  GNU General Public License for more details.
**  
**  You should have received a copy of the GNU General Public 
**  License along with OMTools; if not, see 
**  <http://www.gnu.org/licenses/>.
**************************************************************************/


package aldenjava.opticalmapping.visualizer.viewpanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingWorker;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

public class ViewSaver extends SwingWorker<Void, Void> {

	private JComponent[] comp;
	private String filename;
	private ImageSaveFormat imageFormat;

	public ViewSaver(JComponent[] comp, String filename, ImageSaveFormat imageFormat) {
		if (comp.length == 0)
			throw new IllegalArgumentException("Error: drawing empty components.");
		this.comp = comp;

		if (!filename.toLowerCase().endsWith('.' + imageFormat.getExtension()))
			filename += '.' + imageFormat.getExtension();
		this.filename = filename;
		this.imageFormat = imageFormat;
		Integer width = null;
		for (JComponent cp : comp)
			if (width == null)
				width = cp.getWidth();
			else if (width != cp.getWidth())
				throw new IllegalArgumentException("Mismatched width of Jcomponents");
	}

	@Override
	protected Void doInBackground() throws IOException {
		VerbosePrinter.println("Drawing on: " + filename);
		switch (imageFormat) {
			case SVG:
				paintSVG();
				break;
			case PNG:
				paintPNG();
				break;
			case JPG:
				paintJPG();
				break;
			default:
				break;			
		}
		return null;
	}
	@Override
	public void done() {
		try {
			get();
			VerbosePrinter.println("Completed drawing on: " + filename);
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (this) {
			notify();
		}
	}

	private void paintSVG() throws IOException {
		// Use the default SVG printing code
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		// Create an instance of org.w3c.dom.Document.
		String svgNS = "http://www.w3.org/2000/svg";
		Document document = domImpl.createDocument(svgNS, "svg", null);

		// Create an instance of the SVG Generator.
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
		// Ask the test to render into the SVG Graphics2D implementation.
		int height = 0;
		for (JComponent cp : comp) {
			if (height != 0)
				svgGenerator.translate(0, height);
			cp.paint(svgGenerator);
			height = cp.getHeight();
		}
		// Finally, stream out SVG to the standard output using
		// UTF-8 encoding.
		boolean useCSS = true; // we want to use CSS style attributes
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		svgGenerator.stream(bw, useCSS);
		bw.close();
	}

	private void paintPNG() throws IOException {		
		int totalHeight = 0;
		for (JComponent cp : comp)
			totalHeight += cp.getHeight();
		BufferedImage bi = new BufferedImage(comp[0].getWidth(), totalHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, comp[0].getWidth(), totalHeight);
		int height = 0;
		for (JComponent cp : comp) {
			if (height != 0)
				g.translate(0, height);
			cp.paint(g);
			height = cp.getHeight();
		}

		ImageIO.write(bi, "png", new File(filename));
	}

	private void paintJPG() throws IOException {
		int totalHeight = 0;
		for (JComponent cp : comp)
			totalHeight += cp.getHeight();
		BufferedImage bi = new BufferedImage(comp[0].getWidth(), totalHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		g.setBackground(Color.WHITE);
		g.clearRect(0, 0, comp[0].getWidth(), totalHeight);
		int height = 0;
		for (JComponent cp : comp) {
			if (height != 0)
				g.translate(0, height);
			cp.paint(g);
			height = cp.getHeight();
		}
		ImageIO.write(bi, "jpg", new File(filename));
	}
}
