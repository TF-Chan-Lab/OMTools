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


package aldenjava.opticalmapping.data.data;

import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import aldenjava.opticalmapping.miscellaneous.InvalidFileFormatException;

public enum SegmentIdentifierFormat {
	SI (0, "Segment Identifier Format (SI)", "si");

	private final int format;
	private final String description;
	private final String extension;

	SegmentIdentifierFormat(int format, String description, String extension) {
		this.format = format;
		this.description = description;
		this.extension = extension;
	}

	public int getFormat() {
		return format;
	}

	public String getExtension() {
		return extension;
	}

	public static String[] getExtensions() {
		String[] extensions = new String[SegmentIdentifierFormat.values().length];
		int index = 0;
		for (SegmentIdentifierFormat rformat : SegmentIdentifierFormat.values())
			extensions[index++] = rformat.getExtension();
		return extensions;
	}

	public static final Map<Integer, SegmentIdentifierFormat> lookupmap = new HashMap<Integer, SegmentIdentifierFormat>();
	static {
		for (SegmentIdentifierFormat dformat : SegmentIdentifierFormat.values())
			lookupmap.put(dformat.getFormat(), dformat);
	}
	public static final Map<String, SegmentIdentifierFormat> lookupfileextmap = new HashMap<String, SegmentIdentifierFormat>();
	static {
		for (SegmentIdentifierFormat dformat : SegmentIdentifierFormat.values())
			lookupfileextmap.put(dformat.getExtension(), dformat);
	}

	public static final SegmentIdentifierFormat lookup(String path, int format) {
		if (format == -1)
			return lookupfileext(FilenameUtils.getExtension(path));
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final SegmentIdentifierFormat lookup(int format) {
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final SegmentIdentifierFormat lookupfileext(String extension) {
		if (!lookupfileextmap.containsKey(extension.toLowerCase()))
			throw new InvalidFileFormatException();
		return lookupfileextmap.get(extension.toLowerCase());
	}

	public static String getFormatHelp() {
		StringBuilder dataFormatHelp = new StringBuilder();
		dataFormatHelp.append(String.format("%d: %s; ", -1, "Auto-detected from file extension"));
		for (SegmentIdentifierFormat dformat : SegmentIdentifierFormat.values())
			dataFormatHelp.append(String.format("%d: %s; ", dformat.format, dformat.description));
		return dataFormatHelp.toString();
	}

	public static final FileNameExtensionFilter[] getFileNameExtensionFilter() {
		FileNameExtensionFilter[] filters = new FileNameExtensionFilter[SegmentIdentifierFormat.values().length + 1];
		int index = 0;
		filters[index++] = new FileNameExtensionFilter("All supported format", SegmentIdentifierFormat.getExtensions());
		for (SegmentIdentifierFormat dformat : SegmentIdentifierFormat.values())
			filters[index++] = new FileNameExtensionFilter(dformat.description, dformat.extension);
		return filters;
	}

	public static boolean isValidFormat(String extension) {
		return lookupfileextmap.containsKey(extension.toLowerCase());
	}



}
