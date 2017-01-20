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


package aldenjava.opticalmapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.assembler.debruijn.KmerConstructor;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class KmerReader extends OMReader<Kmer> {

	private LinkedHashMap<String, DataNode> mapInfo = null;
	private int k = 0;
	
	public KmerReader(String filename) throws IOException {
		super(filename);
	}
	public KmerReader(OptionSet options) throws IOException {
		this((String) options.valueOf("kmerin"));
	}

	public void importMapInfo(LinkedHashMap<String, DataNode> mapInfo, int k) {
		this.mapInfo = mapInfo;
	}

	@Override
	public Kmer read() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\t");
		proceedNextLine();
		String source = l[0];
		int pos = Integer.parseInt(l[1]);
		if (mapInfo != null && k > 0) {
			return mapInfo.get(source).getKmer(k, pos);
		}
		else {
			List<Long> sizelist = new ArrayList<>();
			if (l.length <= 2)
				throw new RuntimeException("No data inforamtion available.");
			String[] sizes = l[2].split(";");
			for (String size : sizes)
				sizelist.add(Long.parseLong(size));
			return new Kmer(source, pos, sizelist);
		}
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Kmer input option", level);
		parser.accepts("kmerin", "Kmer file input").withRequiredArg().ofType(String.class);
		KmerSetting.assignOptions(parser, level + 1);
		OptMapDataReader.assignOptions(parser, level + 1);
		
	}

	public static List<Kmer> readAll(OptionSet options) throws IOException {
		if (options.valueOf("kmerin") == null && (options.valueOf("optmapin") != null && options.valueOf("k") != null && options.valueOf("maxnosignal") != null))
			return KmerConstructor.constructKmers(options); // attempt to construct kmers
		KmerReader kr = new KmerReader(options);
		List<Kmer> kmerList = kr.readAll();
		kr.close();
		return kmerList;
	}
	public static List<Kmer> readAll(String filename) throws IOException {
		KmerReader kr = new KmerReader(filename);
		List<Kmer> kmerList = kr.readAll();
		kr.close();
		return kmerList;
	}

}
