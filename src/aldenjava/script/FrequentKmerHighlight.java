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


package aldenjava.script;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.SegmentIdentifier;
import aldenjava.opticalmapping.data.data.SegmentIdentifierWriter;
import aldenjava.opticalmapping.mapper.AlignmentOptions;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.mapper.seeding.SeedDatabase;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

public class FrequentKmerHighlight {

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(FrequentKmerHighlight.class.getSimpleName(), "Returns segment identifiers in kmers with more than minimum number of hit at the same query.");
		OptMapDataReader.assignOptions(parser, 1);
		SegmentIdentifierWriter.assignOptions(parser, 1);
		SeedDatabase.assignOptions(parser, 1);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		parser.addHeader("Kmer Frequency Highlight Options", 1);
		parser.accepts("minhit", "Minimum number of hit").withRequiredArg().ofType(Integer.class).defaultsTo(1);
		AlignmentOptions.assignErrorToleranceOptions(parser);
		
		
		OptionSet options = parser.parse(args);
		LinkedHashMap<String, DataNode> dataMap = OptMapDataReader.readAllData(options);
		int k = (int) options.valueOf("k");
		double ear = (double) options.valueOf("ear");
		int measure = (int) options.valueOf("meas");
		int minHit = (int) options.valueOf("minhit");
		SegmentIdentifierWriter sw = new SegmentIdentifierWriter(options);
		for (DataNode data : dataMap.values()) {
			Set<SegmentIdentifier> sis = new HashSet<>();
			List<Kmer> kmerList = data.getKmerWord(k, (int) options.valueOf("maxnosignal"));
			SeedDatabase seedDatabase = new SeedDatabase(kmerList, k);
			seedDatabase.setMode(2);
			seedDatabase.buildDatabase();
			

			for (Kmer queryKmer : kmerList) {
				for (int strand = -1; strand <= 1; strand += 2) {
					List<Kmer> hitKmers;
					if (strand == 1)
						hitKmers = seedDatabase.getKmerList(queryKmer, ear, measure);
					else
						hitKmers = seedDatabase.getKmerList(queryKmer.getReverse(), ear, measure);
					if (hitKmers.size() - (strand == 1 ? 1 : 0) >= minHit) {
						for (Kmer hitKmer : hitKmers)
							for (int i = 0; i < k; i++)
								sis.add(new SegmentIdentifier(data.name, hitKmer.pos + i));
						for (int i = 0; i < k; i++)
							sis.add(new SegmentIdentifier(data.name, queryKmer.pos + i));
					}
				}
			}
			for (SegmentIdentifier si : sis)
				sw.write(si);
		}
		sw.close();
		
	}
}
