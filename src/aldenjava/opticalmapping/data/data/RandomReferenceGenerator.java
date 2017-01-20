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


package aldenjava.opticalmapping.data.data;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.data.ReferenceWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class RandomReferenceGenerator {

	public static DataNode generateRandomReference(DataNode ref) {
		long[] newrefl = new long[ref.refp.length + 1];
		for (int i = 0; i < ref.refp.length + 1; i++)
			newrefl[i] = ref.getRefl(i);
		List<Long> newrefllist = Arrays.asList(ArrayUtils.toObject(newrefl));
		Collections.shuffle(newrefllist);
		newrefl = ArrayUtils.toPrimitive(newrefllist.toArray(new Long[newrefllist.size()]));
		return new DataNode(ref.name, newrefl);
	}

	public static LinkedHashMap<String, DataNode> generateRandomReference(LinkedHashMap<String, DataNode> optrefmap) {
		LinkedHashMap<String, DataNode> newoptrefmap = new LinkedHashMap<String, DataNode>();
		for (DataNode ref : optrefmap.values())
			newoptrefmap.put(ref.name, generateRandomReference(ref));
		return newoptrefmap;
	}

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(RandomReferenceGenerator.class.getSimpleName(), "Generates random reference maps by shuffling the order of segments in the input reference maps.");
		ReferenceReader.assignOptions(parser);
		ReferenceWriter.assignOptions(parser);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		ReferenceWriter rw = new ReferenceWriter(options);
		rw.writeAll(RandomReferenceGenerator.generateRandomReference(ReferenceReader.readAllData(options)));
		rw.close();
	}
}
