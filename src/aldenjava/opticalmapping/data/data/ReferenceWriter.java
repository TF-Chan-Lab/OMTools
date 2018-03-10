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

import java.io.IOException;
import java.util.LinkedHashMap;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

/**
 * A wrapper class redirecting to <code>OptMapDataWriter</code> with options changed to refmapout
 * 
 * @author Alden
 *
 */
public class ReferenceWriter extends OptMapDataWriter {
	public ReferenceWriter(OptionSet options) throws IOException {
		this((String) options.valueOf("refmapout"), (int) options.valueOf("refmapoutformat"));
	}

	public ReferenceWriter(String filename, int format) throws IOException {
		super(filename, DataFormat.lookup(filename, format));
	}

	public ReferenceWriter(String filename) throws IOException {
		this(filename, -1);
	}

	public ReferenceWriter(String filename, DataFormat dformat) throws IOException {
		super(filename, dformat);
	}

	public static void writeAllData(OptionSet options, LinkedHashMap<String, DataNode> optrefmap) throws IOException {
		writeAllData((String) options.valueOf("refmapout"), (int) options.valueOf("refmapoutformat"), optrefmap);
	}

	public static void writeAllData(String filename, LinkedHashMap<String, DataNode> optrefmap) throws IOException {
		writeAllData(filename, -1, optrefmap);
	}

	public static void writeAllData(String filename, int format, LinkedHashMap<String, DataNode> optrefmap) throws IOException {
		ReferenceWriter rw = new ReferenceWriter(filename, format);
		rw.writeAll(optrefmap);
		rw.close();
	}

	public static void writeAllData(OptionSet options, DataNode ref) throws IOException {
		writeAllData((String) options.valueOf("refmapout"), (int) options.valueOf("refmapoutformat"), ref);
	}

	public static void writeAllData(String filename, DataNode ref) throws IOException {
		writeAllData(filename, -1, ref);
	}

	public static void writeAllData(String filename, int format, DataNode ref) throws IOException {
		ReferenceWriter rw = new ReferenceWriter(filename, format);
		rw.write(ref);
		rw.close();
	}

	public static void assignOptions(ExtendOptionParser parser) {
		parser.addHeader("Reference Writer Options", 1);
		parser.accepts("refmapout", "Output reference map file").withRequiredArg().ofType(String.class).required();
		parser.accepts("refmapoutformat", DataFormat.getFormatHelp()).withRequiredArg().ofType(Integer.class).defaultsTo(-1);
	}

}
