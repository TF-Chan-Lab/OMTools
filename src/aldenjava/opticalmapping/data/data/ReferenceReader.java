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
import java.io.InputStream;
import java.util.LinkedHashMap;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
/**
 * A wrapper class redirecting to <code>OptMapDataReader</code> with options changed to refmapin
 * @author Alden
 *
 */
public class ReferenceReader extends OptMapDataReader {

	public ReferenceReader(OptionSet options) throws IOException {
		this((String) options.valueOf("refmapin"), (int) options.valueOf("refmapinformat"));
	}

	public ReferenceReader(String filename, int format) throws IOException {
		super(filename, format);
	}

	public ReferenceReader(String filename, DataFormat dformat) throws IOException {
		super(filename, dformat);
	}

	public ReferenceReader(InputStream stream, DataFormat dformat) throws IOException {
		super(stream, dformat);
	}

	public static LinkedHashMap<String, DataNode> readAllData(String filename) throws IOException {
		return readAllData(filename, -1);
	}

	public static LinkedHashMap<String, DataNode> readAllData(String filename, int format) throws IOException {
		ReferenceReader rr = new ReferenceReader(filename, format);
		LinkedHashMap<String, DataNode> optrefmap = rr.readAllData();
		rr.close();
		return optrefmap;
	}

	public static LinkedHashMap<String, DataNode> readAllData(OptionSet options) throws IOException {
		ReferenceReader rr = new ReferenceReader(options);
		LinkedHashMap<String, DataNode> optrefmap = rr.readAllData();
		rr.close();
		return optrefmap;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Reference Reader Options", 1);
		parser.accepts("refmapin", "Input reference map file").withRequiredArg().ofType(String.class).required();
		parser.accepts("refmapinformat", DataFormat.getFormatHelp()).withRequiredArg().ofType(Integer.class).defaultsTo(-1);
	}
	
	public static void assignOptions(ExtendOptionParser parser) {
		assignOptions(parser, 1);
	}

}
