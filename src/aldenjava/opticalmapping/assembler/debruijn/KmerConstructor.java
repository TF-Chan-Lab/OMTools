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


package aldenjava.opticalmapping.assembler.debruijn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.KmerSetting;
import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class KmerConstructor {

	public static List<Kmer> constructKmers(OptionSet options) throws IOException {
		List<Kmer> kmerList = new ArrayList<>();
		int k = (int) options.valueOf("k");
		int maxnosignal = (int) options.valueOf("maxnosignal");
		for (DataNode data : OptMapDataReader.readAllData(options).values()) {
			kmerList.addAll(data.getKmerWord(k, maxnosignal));
		}
		return kmerList;
	}
	
	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser("KmerConstructor");
		
		OptMapDataReader.assignOptions(parser);
		KmerWriter.assignOptions(parser, 1);
		KmerSetting.assignOptions(parser, 1);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		KmerWriter kw = new KmerWriter(options);
		int k = (int) options.valueOf("k");
		int maxnosignal = (int) options.valueOf("maxnosignal");
		for (DataNode data : OptMapDataReader.readAllData(options).values()) {
			List<Kmer> kmerlist = data.getKmerWord(k, maxnosignal);
			for (Kmer kmer : kmerlist)
				kw.write(kmer);
		}
		kw.close();
			

	}

}

class KmerWriter extends OMWriter<Kmer> {

	
	public KmerWriter(String filename) throws IOException {
		super(filename);
	}
	public KmerWriter(OptionSet options) throws IOException {
		this((String) options.valueOf("kmerout"));
	}
	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Kmer Writer Options", level);
		parser.accepts("kmerout", "Output Kmer file").withRequiredArg().ofType(String.class);		

	}

	@Override
	public void initializeHeader() {
		// Don't write a header
	}
	
	@Override
	public void write(Kmer kmer) throws IOException {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < kmer.k(); i++) {
			if (i != 0)
				s.append(";");
			s.append(Long.toString(kmer.get(i)));
		}
		bw.write(kmer.source + "\t" + kmer.pos + "\t" + s.toString() + "\n");

	}
	
}