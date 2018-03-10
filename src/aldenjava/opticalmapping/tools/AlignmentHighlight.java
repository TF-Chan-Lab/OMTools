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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import aldenjava.opticalmapping.data.annotation.BEDNode;
import aldenjava.opticalmapping.data.annotation.BEDWriter;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.SegmentIdentifier;
import aldenjava.opticalmapping.data.data.SegmentIdentifierWriter;
import aldenjava.opticalmapping.data.mappingresult.MatchingSignalPair;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class AlignmentHighlight {
	public static void main(String[] args) throws IOException {		
		ExtendOptionParser parser = new ExtendOptionParser(AlignmentHighlight.class.getSimpleName(), "Highlights segments used in alignments as segment identifiers.");
		OptMapDataReader.assignOptions(parser, 1);
		OptMapResultReader.assignOptions(parser, 1);
		SegmentIdentifierWriter.assignOptions(parser, 1);
		BEDWriter.assignOptions(parser, 1);
		parser.addHeader("Alignment Highlight Options", 1);
		OptionSpec<Integer> omode = parser.accepts("mode", "Highlight segments in - 1: Reference only; 2: Query only; 3: Both query and reference").withRequiredArg().ofType(Integer.class).defaultsTo(3);
		OptionSpec<Boolean> ooverlap = parser.accepts("overlap", "Require overlapping query and reference (with at least one signal)").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<Boolean> ostrict = parser.accepts("strict", "Highlight segments only in consecutive signal match (without extra or missing signals in-between)").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		parser.addHeader(null, -1);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		
		

		Set<SegmentIdentifier> segmentIdentifiers = new HashSet<>();
		List<BEDNode> beds = new ArrayList<>();
		int mode = options.valueOf(omode);
		boolean strict = options.valueOf(ostrict);
		boolean requireOverlap = options.valueOf(ooverlap);
		OptMapResultReader omrr = new OptMapResultReader(options);
		LinkedHashMap<String, DataNode> dataInfo = OptMapDataReader.readAllData(options);
		omrr.importFragInfo(dataInfo);
		for (OptMapResultNode result : omrr.readAll()) {
			if (!result.isUsed())
				continue;
			if (requireOverlap) {
				if (!result.mappedRegion.overlap(result.getMoleMappedRegion()))
					continue;
			}
			if (strict) {
				String refName = result.mappedRegion.ref;
				String queryName = result.parentFrag.name;
				int orientation = result.mappedstrand;
				result.updateMSP();
				MatchingSignalPair lastMsp = null;
				for (MatchingSignalPair msp : result.mspsR) {
					if (lastMsp != null)
						if (lastMsp.rpos + 1 == msp.rpos && lastMsp.qpos + orientation == msp.qpos) {
							int querySegment = orientation == 1 ? msp.qpos : lastMsp.qpos;
							int refSegment = msp.rpos;
							SegmentIdentifier sir = new SegmentIdentifier(refName, refSegment);
							SegmentIdentifier siq = new SegmentIdentifier(queryName, querySegment);
							if (mode == 1 || mode == 3)
								segmentIdentifiers.add(sir);
							if (mode == 2 || mode == 3)
								segmentIdentifiers.add(siq);
						}
					lastMsp = msp;
				}
			}
			else {
				if (mode == 1 || mode == 3) {
					String name = result.mappedRegion.ref;
					for (int i = result.subrefstart; i <= result.subrefstop; i++)
						segmentIdentifiers.add(new SegmentIdentifier(name, i));
				}
				if (mode == 2 || mode == 3) {
					String name = result.parentFrag.name;
					if (result.mappedstrand == 1)
						for (int i = result.subfragstart; i <= result.subfragstop; i++)
							segmentIdentifiers.add(new SegmentIdentifier(name, i));
					else
						for (int i = result.subfragstop; i >= result.subfragstart; i--)
							segmentIdentifiers.add(new SegmentIdentifier(name, i));
				}
			}
			if (mode == 1 || mode == 3)
				beds.add(new BEDNode(result.mappedRegion));
			if (mode == 2 || mode == 3)
				beds.add(new BEDNode(result.getMoleMappedRegion()));
		}
		omrr.close();
		
		if (options.has("siout"))
			try (SegmentIdentifierWriter siw = new SegmentIdentifierWriter(options)) {
				siw.writeAll(segmentIdentifiers);
			}
		if (options.has("bedout"))
			try (BEDWriter bw = new BEDWriter(options)) {
				bw.writeAll(beds);
			}
	}
}
