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


package aldenjava.opticalmapping.data.annotation;

import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import aldenjava.opticalmapping.miscellaneous.InvalidFileFormatException;

public enum AnnotationFormat {
	BED (0, "BED File Format (BED)", "bed"),
	GVF (1, "Genome Variation Format (GVF)", "gvf"),
	GTF (2, "General Transfer Format (GTF)", "gtf"),
	GFF (3, "General Feature Format (GFF)", "gff"),
	OSV (4, "Optical Mapping SV Format (OSV)", "osv"),
	AGP (5, "AGP Format (AGP)", "agp");

	private final int format;
	private final String description;
	private final String extension;

	AnnotationFormat(int format, String description, String extension) {
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
		String[] extensions = new String[AnnotationFormat.values().length];
		int index = 0;
		for (AnnotationFormat rformat : AnnotationFormat.values())
			extensions[index++] = rformat.getExtension();
		return extensions;
	}

	public static final Map<Integer, AnnotationFormat> lookupmap = new HashMap<Integer, AnnotationFormat>();
	static {
		for (AnnotationFormat dformat : AnnotationFormat.values())
			lookupmap.put(dformat.getFormat(), dformat);
	}
	public static final Map<String, AnnotationFormat> lookupfileextmap = new HashMap<String, AnnotationFormat>();
	static {
		for (AnnotationFormat dformat : AnnotationFormat.values())
			lookupfileextmap.put(dformat.getExtension(), dformat);
	}

	public static final AnnotationFormat lookup(int format) {
		return lookupmap.get(format);
	}

	public static final AnnotationFormat lookupfileext(String extension) {
		if (!lookupfileextmap.containsKey(extension.toLowerCase()))
			throw new InvalidFileFormatException();
		return lookupfileextmap.get(extension.toLowerCase());
	}

	public static String getFormatHelp() {
		StringBuilder AnnotationFormatHelp = new StringBuilder();
		for (AnnotationFormat dformat : AnnotationFormat.values())
			AnnotationFormatHelp.append(String.format("%d: %s; ", dformat.format, dformat.description));
		return AnnotationFormatHelp.toString();
	}

	public static final FileNameExtensionFilter[] getFileNameExtensionFilter() {
		FileNameExtensionFilter[] filters = new FileNameExtensionFilter[AnnotationFormat.values().length + 1];
		int index = 0;
		filters[index++] = new FileNameExtensionFilter("All supported format", AnnotationFormat.getExtensions());
		for (AnnotationFormat dformat : AnnotationFormat.values())
			filters[index++] = new FileNameExtensionFilter(dformat.description, dformat.extension);
		return filters;
	}

	public static boolean isValidFormat(String extension) {
		return lookupfileextmap.containsKey(extension.toLowerCase());
	}

}
