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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;
/**
 * Abstract class for reading data of any type from a file or input stream using a buffered reader. The only method that a subclass must implement is read(). Subclasses can override commentReader() and proceedNextLine() based on the file format 
 * 
 * @param <T>	the type of elements to be read
 * 
 * @see	OMWriter<T>
 * 
 * @author Alden
 *
 * 
 */
public abstract class OMReader<T> implements Closeable {

	protected final BufferedReader br;
	protected String nextline;
	
	/**
	 * Creates a new reader on a file
	 * @param stream
	 * @throws IOException
	 */
	public OMReader(String filename) throws IOException {
		br = new BufferedReader(new InputStreamReader(OMReader.getFileStream(filename)));
		commentReader();
	}
	/**
	 * Creates a new reader on an <code>InputStream</code>
	 * @param stream
	 * @throws IOException
	 */
	public OMReader(InputStream stream) throws IOException {
		br = new BufferedReader(new InputStreamReader(stream));
		commentReader();
	}
	/**
	 * Attempts to skip the headers in the data file. Subclasses can override this method depending on file format  
	 * @throws IOException
	 */
	protected void commentReader() throws IOException {
		do {
			nextline = br.readLine();
		}
		while (nextline != null && (nextline.startsWith("#") || nextline.isEmpty()));
	}
	/**
	 * Attempts to proceed to next line to read data. It is recommended to override and use this method to proceed to next line 
	 * @throws IOException	if an I/O error occurs
	 */
	protected void proceedNextLine() throws IOException	{
		do {
			nextline = br.readLine();
		}
		while (nextline != null && (nextline.startsWith("#") || nextline.isEmpty()));
	}
	/**
	 * Attempts to read for next data entry.  
	 * @return	the data of type <code>T</code> or <code>null</code> if the end of file is reached
	 * @throws IOException	if an I/O error occurs
	 */
	public abstract T read() throws IOException;
	/**
	 * Attempts to read for all data and output as list.   
	 * @return	the list of data of type <code>T</code> or an empty list if the there is no entry
	 * @throws IOException	if an I/O error occurs
	 */
	public List<T> readAll() throws IOException {
		List<T> tList = new ArrayList<T>();
		T t;
		while ((t = read()) != null)
			tList.add(t);
		return tList;
	}

	/**
	 * Attempts to read for all data and output according to the collector.   
	 * @return	the collected elements
	 * @throws IOException	if an I/O error occurs
	 */
	public <R> R readAll(Collector<T,?,R> collector) throws IOException {
		return getStream().collect(collector);		
	}
	
	/**
	 * Returns a <code>Stream</code> of the elements    
	 * @return a {@code Stream<T>} 
	 * @throws IOException	if an I/O error occurs
	 */
	public Stream<T> getStream() throws IOException {
		Stream.Builder<T> builder = Stream.builder();
		T t;
		while ((t = read()) != null)
			builder.accept(t);
		return builder.build();
	}
	
	@Override
	public void close() throws IOException {
		br.close();
	}

	/**
	 * Returns a {@code FileInputStream} for non-compressed file, or an {@code InflaterInputStream} if the file is in supported compression format, defined in {@code CompressionFormat}
	 * @param filename
	 * @return an {@code InputStream} 
	 * @throws IOException
	 */
	public static InputStream getFileStream(String filename) throws IOException {
		InputStream stream = new BufferedInputStream(new FileInputStream(filename));
		String extension = FilenameUtils.getExtension(filename);
		if (CompressionFormat.isValidFormat(extension)) {
			CompressionFormat cformat = CompressionFormat.lookupfileext(extension);
			switch (cformat) {
				case GZIP:
					stream = new GZIPInputStream(stream);
					break;
			}
		}
		return stream;
	}
}
