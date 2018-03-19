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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class MAFReader extends OMReader<MAFNode> {

	public MAFReader(String filename) throws IOException {
		super(filename);
	}
	public MAFReader(OptionSet options) throws IOException {
		this((String) options.valueOf("mafin"));
	}
	@Override
	public MAFNode read() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\\s+");
		int score = Integer.parseInt(l[1].substring(6));
		String label = l[2].substring(6);
		int mult = Integer.parseInt(l[3].substring(5));
		Map<String, MAFEntry> entries = new LinkedHashMap<>();
		for (int i = 0; i < mult; i++) {
			proceedNextLine();
			l = nextline.split("\\s+");
			String id = l[1];
			long start = Long.parseLong(l[2]);
			long length = Long.parseLong(l[3]);
			int orientation = l[4].equals("+")?1:-1;
			long genomeSize = Long.parseLong(l[5]); 
			String seq = l[6];
			entries.put(id, new MAFEntry(label, id, (orientation==1?(start):(genomeSize-start-length)) + 1, length, orientation, genomeSize, seq));
		}
		proceedNextLine();
		return new MAFNode(label, score, entries);
	}
	public static LinkedHashMap<String, MAFNode> readAllData(OptionSet options) throws IOException {
		MAFReader mafr = new MAFReader(options);
		MAFNode maf;
		LinkedHashMap<String, MAFNode> map = new LinkedHashMap<>();
		while ((maf = mafr.read()) != null)
			map.put(maf.label, maf);
		mafr.close();
		return map;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("MAF Reader", level);
		parser.accepts("mafin", "Multiple alignment format input.").withRequiredArg().ofType(String.class).required();		
	}

}
