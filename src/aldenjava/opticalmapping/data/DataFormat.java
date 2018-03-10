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

import java.util.HashMap;
import java.util.Map;

import javax.swing.filechooser.FileNameExtensionFilter;

import aldenjava.file.CompressedFilenameUtils;
import aldenjava.file.FileNameCompressionExtensionFilter;
import aldenjava.opticalmapping.miscellaneous.InvalidFileFormatException;

/**
 * Optical map data format that can be parsed
 * @author Alden
 *
 */
public enum DataFormat {
	REF (0, "Reference Standard Format (REF) (Equivalent to SILICO format)", "ref"),
	FA01 (1, "fasta-01 format (FA01)", "fa01"),
	SPOTS (2, "Spots File Format (SPOTS)", "spots"),
	DATA (3, "Molecule Standard Format (DATA)", "data"),
	SDATA (4, "Molecule Simulation Format (SDATA)", "sdata"),
	BNX (5, "BNX File Format (BNX)", "bnx"),
	CMAP (6, "CMAP File Format (CMAP)", "cmap"),
	OPT (7, "SOMA opt format (OPT)", "opt"),
	SILICO (8, "SOMA silico format (SILICO) (Equivalent to REF format)", "silico"),
	XML (9, "OpGen XML Format", "xml"),
	VALDATA (10, "Valouev data format", "valdata"),
	MAPS (11, "Maligner maps format", "maps");

	private final int format;
	private final String description;
	private final String extension;

	DataFormat(int format, String description, String extension) {
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
		String[] extensions = new String[DataFormat.values().length];
		int index = 0;
		for (DataFormat rformat : DataFormat.values())
			extensions[index++] = rformat.getExtension();
		return extensions;
	}

	public static final Map<Integer, DataFormat> lookupmap = new HashMap<Integer, DataFormat>();
	static {
		for (DataFormat dformat : DataFormat.values())
			lookupmap.put(dformat.getFormat(), dformat);
	}
	public static final Map<String, DataFormat> lookupfileextmap = new HashMap<String, DataFormat>();
	static {
		for (DataFormat dformat : DataFormat.values())
			lookupfileextmap.put(dformat.getExtension(), dformat);
	}

	
	public static final DataFormat lookup(String path, int format) {
		if (format == -1)
			return lookupfileext(CompressedFilenameUtils.getDecompressedExtension(path));
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final DataFormat lookup(int format) {
		if (!lookupmap.containsKey(format))
			throw new InvalidFileFormatException();
		return lookupmap.get(format);
	}

	public static final DataFormat lookupfileext(String extension) {
		if (!lookupfileextmap.containsKey(extension.toLowerCase()))
			throw new InvalidFileFormatException();
		return lookupfileextmap.get(extension.toLowerCase());
	}

	public static String getFormatHelp() {
		StringBuilder dataFormatHelp = new StringBuilder();
		dataFormatHelp.append(String.format("%d: %s; ", -1, "Auto-detected from file extension"));
		for (DataFormat dformat : DataFormat.values())
			dataFormatHelp.append(String.format("%d: %s; ", dformat.format, dformat.description));
		return dataFormatHelp.toString();
	}

	public static final FileNameCompressionExtensionFilter[] getFileNameExtensionFilter() {
		FileNameCompressionExtensionFilter[] filters = new FileNameCompressionExtensionFilter[DataFormat.values().length + 1];
		int index = 0;
		filters[index++] = new FileNameCompressionExtensionFilter("All supported format", DataFormat.getExtensions());
		for (DataFormat dformat : DataFormat.values()) {
			filters[index++] = new FileNameCompressionExtensionFilter(dformat.description, dformat.extension);
//			filters[index++] = new FileNameExtensionFilter(dformat.description, CompressionFormat.expandAcceptedExtensions(dformat.extension));
		}
		return filters;
	}

	public static boolean isValidFormat(String extension) {
		return lookupfileextmap.containsKey(extension.toLowerCase());
	}

}
