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


package aldenjava.opticalmapping.tools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.ConcatInfoReader;
import aldenjava.opticalmapping.data.data.ConcatInfoWriter;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.OptMapDataWriter;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.mapper.seeding.SeedDatabase;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

/**
 * Provides basic functions for manipulation of optical mapping data
 * @author Alden
 *
 */
public class DataTools {

	public static void main(String[] args) throws IOException
	{
		ExtendOptionParser parser = new ExtendOptionParser(DataTools.class.getSimpleName(), "Provides basic functions for filtering and processing optical mapping data");	

		parser.addHeader("Data Tools Options", 1);
		OptionSpec<String> oidprefix = parser.accepts("idprefix", "Add a prefix to all ids").withRequiredArg().ofType(String.class);
		OptionSpec<Integer> oidmodify = parser.accepts("idmodify", "Convert all ids to x .. x + n - 1 (x: input value, n: number of optical maps in the data file). A negative value disables this function. ").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
		OptionSpec<String> oidmodifylog = parser.accepts("idmodifylog", "Log file containing the id conversions").withRequiredArg().ofType(String.class);
		OptionSpec<Boolean> ofix = parser.accepts("fix", "Fix the data (negative signal-to-signal distance correction and etc.)").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		OptionSpec<Integer> ocondense = parser.accepts("condense", "Merge multiple signals closer than parameter into one signal").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		OptionSpec<Long> oremoveseg = parser.accepts("removeseg", "Remove segments smaller than the parameter").withRequiredArg().ofType(Long.class).defaultsTo(-1L);
		OptionSpec<Integer> ominsize = parser.accepts("minsize", "Data with minimum size to retain").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		OptionSpec<Integer> ominsig = parser.accepts("minsig", "Data with minimum signal to retain").withRequiredArg().ofType(Integer.class).defaultsTo(0);	
		OptionSpec<String> odataid = parser.accepts("dataid", "List of Data ID to be extracted").withRequiredArg().ofType(String.class);
		OptionSpec<String> oregion = parser.accepts("region", "List of regions to be extracted.").withRequiredArg().ofType(String.class);
		OptionSpec<Integer> oshift = parser.accepts("shift", "Shift forward (right) x bp (Assume circular)").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		OptionSpec<Integer> orandomdata = parser.accepts("randdata", "Number of random data to be extracted").withRequiredArg().ofType(Integer.class);
		OptionSpec<Long> oseed = parser.accepts("seed", "Seed used in random data extraction").withRequiredArg().ofType(Long.class);
		OptionSpec<Long> oconcat = parser.accepts("concat", "Concatenate all data entries into single entry. -1: not activated; Non-negative value: space (segment without any signal) between each data entry. Ignore any data modification functions").withRequiredArg().ofType(Long.class).defaultsTo(-1L);
		ConcatInfoReader.assignOptions(parser, 2);
		ConcatInfoWriter.assignOptions(parser, 2);
		parser.accepts("concatin").withRequiredArg();
		parser.accepts("concatout").withRequiredArg();

		parser.addHeader("Low complexity filtering", 2);
		OptionSpec<Integer> olowcom = parser.accepts("lowcom", "Retain/Remove molecules with low complexity -1: Retain Low Complexity; 0: Do nothing; 1: Retain High Complexity").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		OptionSpec<Double> omaxdens = parser.accepts("maxdensity", "Maximum density per 100kbp to filter").withRequiredArg().ofType(Double.class).defaultsTo(25.0);
		OptionSpec<Integer> omaxseed = parser.accepts("maxseed", "Maximum seed to filter").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		
		OptMapDataReader.assignOptions(parser);
		OptMapDataWriter.assignOptions(parser);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}		
		OptionSet options = parser.parse(args);
		if ((options.has(odataid) || options.has(oregion)) && options.has(orandomdata))
		{
			System.err.println("randdata and dataid/region could not co-exist! Program ends.");
			return;
		}
		
		String idprefix = null;
		if (options.has(oidprefix))
			idprefix = oidprefix.value(options);
		
		int idmodify = oidmodify.value(options);
		BufferedWriter bw = null;
		if (options.has(oidmodifylog))
		{
			bw = new BufferedWriter(new FileWriter(oidmodifylog.value(options)));
			if (idmodify >= 0)				
				bw.write("#OriginalID\tNewID\n");
			else
				bw.write("#No ID modification was performed.");
		}
		int shift = oshift.value(options);
		HashSet<Integer> selectedRandomData = null;
		if (options.has(orandomdata))
		{
			
			int count = OptMapDataReader.countData((String) options.valueOf("optmapin"));
			int randomdata = orandomdata.value(options);
			
			if (randomdata < count)
			{
				int[] selectedDataArray = new int[count];
				for (int i = 0; i < count; i++)
					selectedDataArray[i] = i + 1;
				Random random = new Random();
				if (options.has(oseed))
					random.setSeed(options.valueOf(oseed));
				// Fisher-Yates shuffle
				for(int i = selectedDataArray.length - 1; i > 0; i--)
				{
		            int rand = (int) (random.nextDouble()*i);
		            int temp = selectedDataArray[i];
		            selectedDataArray[i] = selectedDataArray[rand];
		            selectedDataArray[rand] = temp;
				}
				selectedRandomData = new HashSet<Integer>();
				for (int i = 0; i < randomdata; i++)
					selectedRandomData.add(selectedDataArray[i]);
				
				
			}
		}
//		LinkedHashMap<String, List<GenomicPosNode>> targetRegionMap = null;
		LinkedHashMap<String, GenomicPosNode> targetRegionMap = null;
		if (options.has(oregion))
		{
//			targetRegionMap = new LinkedHashMap<String, List<GenomicPosNode>>();
			targetRegionMap = new LinkedHashMap<String, GenomicPosNode>();
			List<String> sList = options.valuesOf(oregion);
			for (String s : sList)
			{
				GenomicPosNode region = new GenomicPosNode(s);
				if (targetRegionMap.containsKey(region.ref))
					VerbosePrinter.println("No duplicated region-ref-id could be used. Region " + targetRegionMap.get(region.ref).toString() + " is overriden.");
				targetRegionMap.put(region.ref, region);
//				List<GenomicPosNode> regionList = targetRegionMap.get(region.ref);
//				if (regionList == null)
//				{
//					regionList = new ArrayList<GenomicPosNode>();
//					targetRegionMap.put(region.ref, regionList);
//				}
//				regionList.add(region);
			}
		}
		boolean fix = ofix.value(options);
		int condense = ocondense.value(options);
		long removeseg = oremoveseg.value(options);
		int minsize = ominsize.value(options);
		int minsig = ominsig.value(options);
		int lowcom = olowcom.value(options);
		long concat = oconcat.value(options); 
		HashSet<String> idlist = new HashSet<String>(odataid.values(options));
		OptMapDataReader omdr = new OptMapDataReader(options);
		OptMapDataWriter omdw = new OptMapDataWriter(options);
		if (concat >= 0) {
			List<DataNode> list = omdr.readAll();
			List<DataNode> nlist = new ArrayList<DataNode>();
			if (concat > 0) {
				for (DataNode d : list) {
					if (d != list.get(0))
						nlist.add(new DataNode("dummy", concat));
					nlist.add(d);
				}
				
				list = nlist;
			}
			DataNode[] dataEntries = list.toArray(new DataNode[list.size()]);
			DataNode d = new DataNode("1", 0);
			d.join(dataEntries);
			omdw.write(d);
		} 
		else {
			DataNode data;
			int count = 1;
			while ((data = omdr.read()) != null)			
				if (idlist.isEmpty() || idlist.contains(data.name))
					if (targetRegionMap == null || targetRegionMap.containsKey(data.name))
					{
						if (selectedRandomData == null || selectedRandomData.contains(count))
						{
							if (idmodify >= 0 || idprefix != null)
							{
								String oldid = data.name;
								String newid = oldid;
								if (idmodify >= 0)
									newid = Integer.toString(count + idmodify);
								if (idprefix != null)
									newid = idprefix + newid;
								if (options.has(oidmodifylog))	
									bw.write(oldid + "\t" + newid + "\n");
								data.name = newid;
							}
							if (fix)
							{
								if (data.fix())
									VerbosePrinter.println(data.name + " has been fixed.");
							}
							if (condense > 0)
								data.degenerate(condense);
							if (removeseg >= 0)
								data.removeSmallSegments(removeseg);
							if (targetRegionMap != null)
								data = data.subRefNode(targetRegionMap.get(data.name));
							if (shift != 0 && data.size != 0) {
								shift %= data.size;
								if (shift < 0)
									shift += data.size;
								DataNode cut = data.remove(data.size - shift + 1, shift);
								data.insert(1, cut);
							}
							
							if (data.refp.length >= minsig && data.size >= minsize) {
								boolean retain = true;
								if (lowcom != 0) {
									long acrossRegion = 100000;
									double densityThreshold = omaxdens.value(options) / (double) acrossRegion; // 100 or 1000?
									// Method1: Find high density region
									for (double density : data.getSignalDensityAcrossRegion(acrossRegion)) {
										if (density > densityThreshold)
											retain = false;
									}
									
									// Method2: Find repeat region
									if (retain) {
										int kmerlen = 3;
										int maxnosignalregion = 1000000;
										double ear = 0.05;
										int measure = 500;
										int maxSeedNumber = omaxseed.value(options);
										int threshold = 1;
										List<Kmer> fragmentkmerlist = data.getKmerWord(kmerlen, maxnosignalregion);
										SeedDatabase tDatabase = new SeedDatabase(fragmentkmerlist, kmerlen);
										tDatabase.setMode(1);
										tDatabase.setParameters(kmerlen, maxnosignalregion);
										if (fragmentkmerlist.size() - tDatabase.filter(fragmentkmerlist, ear, measure, maxSeedNumber, 100).size() > threshold)
											retain = false;
									}
									
									
									if (lowcom == -1)
										retain = !retain; // retain low complexity...
								}
								if (retain)
									omdw.write(data);
							}
							
						}
						count++;
					}
		}
		omdr.close();
		omdw.close();
		
		if (options.has(oidmodifylog))
		{
			bw.close();
		}
	}
}
