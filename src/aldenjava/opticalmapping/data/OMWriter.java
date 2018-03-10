/**************************************************************************
**  OMTools
**  A software package for processing and analyzing optical mapping data
**  
**  Version 1.4 -- March 10, 2018
**  
**  Copyright (C) 2018 by Alden Leung, Ting-Fung Chan, All rights reserved.
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


package aldenjava.opticalmapping.data;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FilenameUtils;

/**
 * @author Alden
 * Base class for all Optical Mapping project writer.
 * Wrapping a buffered writer
 * @param <T>
 */
public abstract class OMWriter<T> implements Closeable{
	// Format handling is bad: initialize header fails in constructor
	private static String commands = null;
	protected static boolean enableHeader = true;
	protected BufferedWriter bw;
	public OMWriter(String filename) throws IOException {
		this(filename, true);
	}	

	public OMWriter(String filename, boolean autoInitializeHeader) throws IOException {
		OutputStream stream = new BufferedOutputStream(new FileOutputStream(filename));
		String extension = FilenameUtils.getExtension(filename).toLowerCase();
		if (CompressionFormat.isValidFormat(extension)) {
			CompressionFormat cformat = CompressionFormat.lookupfileext(extension);
			switch (cformat) {
				case GZIP:
					stream = new GZIPOutputStream(stream);
					break;
			}
		}
		bw = new BufferedWriter(new OutputStreamWriter(stream));
		if (autoInitializeHeader)
			initializeHeader();
	}
	
	protected void initializeHeader() throws IOException {
		// Unimplemented in any classes
		if (OMWriter.commands != null)
			bw.write("#" + OMWriter.commands + "\n");
		// Plan: Move this into a new method to be called before initialize header
		// Plan A: Use implement interface to state if a subclass support writing command (e.g. fasta writer does not support)
		// Plan B: Use an abstract method returning boolean to check if a subclass support
	}
	public abstract void write(T t) throws IOException;

	public void writeAll(Iterable<T> iterable) throws IOException {
		if (iterable == null)
			throw new NullPointerException("Cannot write null iterable");
		for (T t : iterable)
			write(t);
	}
	public void writeAll(Map<?, T> tMap) throws IOException
	{
		for (T t : tMap.values())
			write(t);
	}
	
	@Override
	public void close() throws IOException
	{
		bw.close();
	}
	public static void setCommands(String s)
	{
		OMWriter.commands = s;
	}

	public static void setHeader(boolean enableHeader) {
		OMWriter.enableHeader = enableHeader;
	}
}
