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


package aldenjava.opticalmapping.visualizer.viewpanel;

import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;

import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.miscellaneous.InvalidFileFormatException;

public enum ImageSaveFormat {
	SVG (0, "Scalable Vector Graphics (SVG)", "svg"),
	PNG (1, "Portable Network Graphics (PNG)", "png"),
	JPG (2, "JPEG / JPG", "jpg");
	private final int format;
	private final String description;
	private final String extension;

	ImageSaveFormat(int format, String description, String extension) {
		this.format = format;
		this.description = description;
		this.extension = extension;
	}

	public int getFormat() {
		return format;
	}

	public String getDescription() {
		return description;
	}

	public String getExtension() {
		return extension;
	}

	public static final Map<Integer, ImageSaveFormat> lookupmap = new HashMap<Integer, ImageSaveFormat>();
	static {
		for (ImageSaveFormat imageformat : ImageSaveFormat.values())
			lookupmap.put(imageformat.getFormat(), imageformat);
	}
	public static final Map<String, ImageSaveFormat> lookupfileextmap = new HashMap<String, ImageSaveFormat>();
	static {
		for (ImageSaveFormat imageformat : ImageSaveFormat.values())
			lookupfileextmap.put(imageformat.getExtension(), imageformat);
	}

	public static final ImageSaveFormat lookup(String path, int format) {
		if (format == -1)
			return lookupfileext(FilenameUtils.getExtension(path));
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final ImageSaveFormat lookup(int format) {
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final ImageSaveFormat lookupfileext(String extension) {
		if (!lookupfileextmap.containsKey(extension.toLowerCase()))
			throw new InvalidFileFormatException();
		return lookupfileextmap.get(extension.toLowerCase());
	}

	public static String getFormatHelp() {
		StringBuilder formatHelp = new StringBuilder();
		formatHelp.append("[");
		for (ImageSaveFormat format : ImageSaveFormat.values())
			formatHelp.append(format.extension + "; ");
		formatHelp.append("]");
		return formatHelp.toString();
	}

	public static final FileNameExtensionFilter[] getFileNameExtensionFilter() {
		FileNameExtensionFilter[] filters = new FileNameExtensionFilter[ImageSaveFormat.values().length];
		int index = 0;
		for (ImageSaveFormat imageFormat : ImageSaveFormat.values())
			filters[index++] = new FileNameExtensionFilter(imageFormat.description, imageFormat.extension);
		return filters;
	}

}
