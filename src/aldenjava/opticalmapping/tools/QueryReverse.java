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


package aldenjava.opticalmapping.tools;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.OptMapDataWriter;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

public class QueryReverse {

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(QueryReverse.class.getSimpleName(), "Reverses the query according to the alignment results so that they have the same strand as the reference. Only one reference and one strand is allowed in the alignment results for each query (or an exception will be thrown). Inversions are not supported.");

		OptMapResultReader.assignOptions(parser);
		OptMapDataReader.assignOptions(parser);
		OptMapDataWriter.assignOptions(parser);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);

		LinkedHashMap<String, DataNode> datamap = OptMapDataReader.readAllData(options);
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = OptMapResultReader.readAllDataInList(options);

		for (DataNode data : datamap.values()) {
			String ref = null;
			int strand = 0;
			for (OptMapResultNode result : resultlistmap.get(data.name)) {
				if(ref == null)
					ref = result.mappedRegion.ref;
				else
					if (!ref.equals(result.mappedRegion.ref))
						throw new RuntimeException("More than one references detected for " + data.name);
				if (strand == 0)
					strand = result.mappedstrand;
				else
					if (strand != result.mappedstrand)
						throw new RuntimeException("More than one alignment strands detected for " + data.name);
			}
			if (strand == -1)
				data.reverse();
		}
		OptMapDataWriter.writeAll(options, datamap);
	}

}
