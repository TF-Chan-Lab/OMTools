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


package aldenjava.sequence.fasta;

import java.io.IOException;
import java.util.LinkedHashMap;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class StreamFastaReader extends OMReader<StreamFastaNode> {
	
	public StreamFastaReader(String filename) throws IOException {
		super(filename);
	}

	public StreamFastaReader(OptionSet options) throws IOException {
		this((String) options.valueOf("fastain"));
	}

	@Override
	public StreamFastaNode read() throws IOException {
		String name = "";
		if (nextline == null)
			return null;
		else {
			name = nextline.substring(1);
			StringBuilder seq = new StringBuilder();
			do {
				nextline = br.readLine();
				if (nextline == null)
					break;
				if (nextline.startsWith(">"))
					break;
				seq.append(nextline);
			} while (true);
			return new StreamFastaNode(name, seq.toString());
		}
	}

	public static LinkedHashMap<String, StreamFastaNode> readAllData(String filename) throws IOException {
		StreamFastaReader sfr = new StreamFastaReader(filename);
		LinkedHashMap<String, StreamFastaNode> sfnMap = new LinkedHashMap<String, StreamFastaNode>();
		StreamFastaNode sfn;
		while ((sfn = sfr.read()) != null)
			sfnMap.put(sfn.name, sfn);
		sfr.close();
		return sfnMap;
	}

	public static LinkedHashMap<String, StreamFastaNode> readAllData(OptionSet options) throws IOException {
		return readAllData((String) options.valueOf("fastain"));
	}

	public static void assignOptions(ExtendOptionParser parser) {
		parser.addHeader("Stream Fasta Reader Options", 1);
		parser.accepts("fastain", "fasta input file").withRequiredArg().ofType(String.class).required();
	}

}
