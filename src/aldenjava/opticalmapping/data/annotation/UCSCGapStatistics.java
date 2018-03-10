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


package aldenjava.opticalmapping.data.annotation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.ReferenceReader;

public class UCSCGapStatistics {

	public static void main(String[] args) throws IOException
	{
		String folder = "C:\\Users\\Alden\\Desktop\\Run2\\Mapping\\Alden\\v0.216\\";
		String filename = folder + "hg19-N-Region.txt";
		String reffile = folder + "hg19_condense700.ref";
		String outputfile = folder + "gapInfo.txt";
		List<UCSCGapNode> gapList = UCSCGapTableReader.readAll(filename);
		ReferenceReader rr = new ReferenceReader(reffile, 0);
		LinkedHashMap<String, DataNode> optrefmap = rr.readAllData();
		rr.close();
		LinkedHashMap<String, Long> totalGapLength = new LinkedHashMap<String, Long>(); 
		for (String key : optrefmap.keySet())
			totalGapLength.put(key, 0L);
		for (UCSCGapNode gap : gapList)
			totalGapLength.put(gap.chrom, totalGapLength.get(gap.chrom) + gap.getGenomicPosNode().length());
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
		bw.write("Reference\tLength\tCoverage\n");
		for (String key : optrefmap.keySet())
			bw.write(String.format("%s\t%d\t%.4f\n", key, totalGapLength.get(key), totalGapLength.get(key) / (double) optrefmap.get(key).size));
		bw.close();
		
		
	}
}
