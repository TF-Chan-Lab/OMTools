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


package aldenjava.opticalmapping.data;

import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import aldenjava.opticalmapping.miscellaneous.InvalidFileFormatException;

public enum MultipleAlignmentFormat {
	CBL (0, "Optical maps collinear blocks format  (CBL)", "cbl"),
	CBO (1, "Optical maps collinear blocks ordering format  (CBO)", "cbo"),
	CBC (2, "Optical maps collinear blocks coloring format  (CBC)", "cbc");

	private final int format;
	private final String description;
	private final String extension;

	MultipleAlignmentFormat(int format, String description, String extension) {
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
	public String getDescription() {
		return description;
	}

	public static String[] getExtensions() {
		String[] extensions = new String[MultipleAlignmentFormat.values().length];
		int index = 0;
		for (MultipleAlignmentFormat format : MultipleAlignmentFormat.values())
			extensions[index++] = format.getExtension();
		return extensions;
	}

	public static final Map<Integer, MultipleAlignmentFormat> lookupmap = new HashMap<Integer, MultipleAlignmentFormat>();
	static {
		for (MultipleAlignmentFormat format : MultipleAlignmentFormat.values())
			lookupmap.put(format.getFormat(), format);
	}
	public static final Map<String, MultipleAlignmentFormat> lookupfileextmap = new HashMap<String, MultipleAlignmentFormat>();
	static {
		for (MultipleAlignmentFormat format : MultipleAlignmentFormat.values())
			lookupfileextmap.put(format.getExtension(), format);
	}

	public static final MultipleAlignmentFormat lookup(String path, int format) {
		if (format == -1)
			return lookupfileext(FilenameUtils.getExtension(path));
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final MultipleAlignmentFormat lookup(int format) {
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final MultipleAlignmentFormat lookupfileext(String extension) {
		if (!lookupfileextmap.containsKey(extension.toLowerCase()))
			throw new InvalidFileFormatException();
		return lookupfileextmap.get(extension.toLowerCase());
	}

	public static String getFormatHelp() {
		StringBuilder formatHelp = new StringBuilder();
		formatHelp.append(String.format("%d: %s; ", -1, "Auto-detected from file extension"));
		for (MultipleAlignmentFormat format : MultipleAlignmentFormat.values())
			formatHelp.append(String.format("%d: %s; ", format.format, format.description));
		return formatHelp.toString();
	}

	public static final FileNameExtensionFilter[] getFileNameExtensionFilter(boolean allowAllSupport) {
		FileNameExtensionFilter[] filters = new FileNameExtensionFilter[MultipleAlignmentFormat.values().length + (allowAllSupport ? 1 : 0)];
		int index = 0;
		if (allowAllSupport)
			filters[index++] = new FileNameExtensionFilter("All supported format", MultipleAlignmentFormat.getExtensions());
		for (MultipleAlignmentFormat format : MultipleAlignmentFormat.values())
			filters[index++] = new FileNameExtensionFilter(format.description, format.extension);
		return filters;
	}

	public static boolean isValidFormat(String extension) {
		return lookupfileextmap.containsKey(extension.toLowerCase());
	}

}
