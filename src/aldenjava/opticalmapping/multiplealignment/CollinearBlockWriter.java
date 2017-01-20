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


package aldenjava.opticalmapping.multiplealignment;

import java.io.IOException;
import java.util.LinkedHashMap;

import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.visualizer.utils.VPartialMoleculeInfo;
import joptsimple.OptionSet;

public class CollinearBlockWriter extends OMWriter<CollinearBlock>{
	private String[] queries;
	
	public CollinearBlockWriter(String filename, String[] queries) throws IOException {
		super(filename, false);
		this.queries = queries;
		initializeHeader();
	}
	public CollinearBlockWriter(OptionSet options, String[] queries) throws IOException {
		this((String)options.valueOf("cbout"), queries);
	}

	@Override
	public void initializeHeader() throws IOException {
		for (String query : queries)
			bw.write("\t" + query);
		bw.write("\n");
	}
	
	@Override
	public void write(CollinearBlock block) throws IOException {
		bw.write(block.name);
		for (String query : queries) {
			bw.write("\t");
			if (block.groups.containsKey(query)) {
				VPartialMoleculeInfo vpmi = block.groups.get(query);
				bw.write(vpmi.startSig + "-" + vpmi.stopSig + (vpmi.isReverse()?"R":"F"));
			}
		}
		bw.write("\n");
	}
	public static void writeAll(String filename, String[] queries, LinkedHashMap<String, CollinearBlock> collinearBlocks) throws IOException {
		CollinearBlockWriter cbw = new CollinearBlockWriter(filename, queries);
		cbw.writeAll(collinearBlocks);
		cbw.close();
	}
	public static void writeAll(OptionSet options, String[] queries, LinkedHashMap<String, CollinearBlock> collinearBlocks) throws IOException {
		CollinearBlockWriter cbw = new CollinearBlockWriter(options, queries);
		cbw.writeAll(collinearBlocks);
		cbw.close();
	}

}
