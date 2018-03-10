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


package aldenjava.opticalmapping.multiplealignment;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

public class CollinearBlockWriter extends OMWriter<CollinearBlock> {
	
	public CollinearBlockWriter(String filename) throws IOException {
		super(filename, false);
		initializeHeader();
	}
	public CollinearBlockWriter(OptionSet options) throws IOException {
		this((String)options.valueOf("cblout"));
	}

	@Override
	public void write(CollinearBlock block) throws IOException {
		bw.write(block.name);
		bw.write("\t");
		boolean firstQuery = true;
		for (String query : block.groups.keySet()) {
			if (!firstQuery)
				bw.write(";");
			firstQuery = false;
			BlockInfo vpmi = block.groups.get(query);
			bw.write(query + ":" + vpmi.startSig + "-" + vpmi.stopSig + (vpmi.isReverse()?"R":"F"));			
		}
		bw.write("\n");
	}
	
	public static void writeAll(String filename, List<CollinearBlock> collinearBlocks) throws IOException {
		CollinearBlockWriter cbw = new CollinearBlockWriter(filename);
		cbw.writeAll(collinearBlocks);
		cbw.close();
	}
	public static void writeAll(OptionSet options, List<CollinearBlock> collinearBlocks) throws IOException {
		CollinearBlockWriter cbw = new CollinearBlockWriter(options);
		cbw.writeAll(collinearBlocks);
		cbw.close();
	}

	public static void writeAll(String filename, LinkedHashMap<String, CollinearBlock> collinearBlocks) throws IOException {
		CollinearBlockWriter cbw = new CollinearBlockWriter(filename);
		cbw.writeAll(collinearBlocks);
		cbw.close();
	}
	public static void writeAll(OptionSet options, LinkedHashMap<String, CollinearBlock> collinearBlocks) throws IOException {
		CollinearBlockWriter cbw = new CollinearBlockWriter(options);
		cbw.writeAll(collinearBlocks);
		cbw.close();
	}
	
	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Collinear Block Writer Options", level);
		parser.accepts("cblout", "Multiple alignment collinear blocks output.").withRequiredArg().ofType(String.class);		

	}

}
