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

import aldenjava.file.ListExtractor;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.OptMapDataWriter;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultWriter;
import aldenjava.opticalmapping.data.mappingresult.ResultsBreaker;
import aldenjava.opticalmapping.data.mappingresult.ResultsBreakingException;
import aldenjava.opticalmapping.mapper.clustermodule.ClusteredResult;
import aldenjava.opticalmapping.mapper.clustermodule.ResultClusterModule;
import aldenjava.opticalmapping.mapper.postmappingmodule.Filter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.MissingInformationException;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * Provides basic functions for filtering and processing of alignment results
 * 
 * @author Alden
 *
 */
public class ResultTools{

	
	public static void main (String[] args) throws IOException
	{
		ExtendOptionParser parser = new ExtendOptionParser(ResultTools.class.getSimpleName(), "Provides basic functions for filtering and processing alignment results");	

		// basic input and output
		OptMapResultReader.assignOptions(parser);
		OptMapResultWriter.assignOptions(parser);
		ReferenceReader.assignOptions(parser);
		OptMapDataReader.assignOptions(parser);

		// Data output
		parser.addHeader("Data Output Options", 1);
		parser.accepts("mapout", "Mapped molecules output").withRequiredArg().ofType(String.class);
		parser.accepts("unmapout", "Unmapped molecules output").withRequiredArg().ofType(String.class);
		parser.accepts("optmapoutformat", DataFormat.getFormatHelp()).withRequiredArg().ofType(Integer.class).defaultsTo(-1);

		// tools option
		parser.addHeader("Result Tools Options", 1);
		parser.accepts("disinvalid","Discard invalid results.").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		parser.accepts("conf","Recalculating result confidence").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<String> odataid = parser.accepts("dataid", "List of Data ID to be extracted").withRequiredArg().ofType(String.class);
		parser.accepts("region", "Region in chrN:start-end or chrN:start format").withRequiredArg().ofType(String.class);
		parser.accepts("refnamemodify", "Modify the reference name according to the target file in format: src\\tTarget").withRequiredArg().ofType(String.class);
		OptionSpec<String> odataremoval = parser.accepts("dataremoval", "Remove result with query names in the file").withRequiredArg().ofType(String.class);
		parser.accepts("joinresult", "Represent partial alignments in one alignment (The gap is filled with extra and missing signals). Only works on partial alignments with indel relationship").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		ResultsBreaker.assignOptions(parser, 2);
		Filter.assignOptions(parser, 2);
		ResultClusterModule.assignOptions(parser, 2);
		LiftOver.assignOptions(parser, 2);
		
		
		parser.addHeader("Simulated Results Analysis Options", 1);
		OptionSpec<String> orocout = parser.accepts("rocout", "Output a table for ROC curve plotting").withRequiredArg().ofType(String.class);
		// Retype required to optional
		parser.addHeader(null, 0);
		parser.accepts("refmapin").withRequiredArg().ofType(String.class);
		parser.accepts("optmapin").withRequiredArg().ofType(String.class);
		parser.accepts("optresout").withRequiredArg().ofType(String.class);
		parser.accepts("minclusterscore", "Min cluster score.").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("minconf", "Minimum confidence").withRequiredArg().ofType(Double.class).defaultsTo(0.0);
		
		// Help
		if (args.length == 0)
		{
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		
		OptMapResultReader omrr = new OptMapResultReader(options);
		OptMapResultWriter omrw = null;
		if (options.has("optresout"))
			omrw = new OptMapResultWriter(options);
		
		GenomicPosNode region = null;
		if (options.has("region"))
			region = new GenomicPosNode((String) options.valueOf("region"));
		List<String> dataid = options.valuesOf(odataid);
		List<String> removeids = null;
		if (options.has(odataremoval))
			removeids = ListExtractor.extractList(options.valueOf(odataremoval));
		LinkedHashMap<String, String> refNameModify = null;
		if (options.has("refnamemodify")) {
			List<List<String>> list = ListExtractor.extractMultiList((String) options.valueOf("refnamemodify"), "\t", "#");
			if (list.size() == 2 && list.get(0).size() == list.get(1).size()) {
				refNameModify = new LinkedHashMap<>();
				for (int i = 0; i < list.get(0).size(); i++)
					refNameModify.put(list.get(0).get(i), list.get(1).get(i));
			}
			else
				System.err.println("Invalid reference modification file");
		}
				
		boolean conf = (Boolean) options.valueOf("conf");
		boolean disinvalid = (Boolean) options.valueOf("disinvalid");
		LinkedHashMap<String, DataNode> fragmentInfo = null;
		if (options.has("optmapin"))
		{
			fragmentInfo = OptMapDataReader.readAllData(options);
			VerbosePrinter.println("Total molecules input: " + Integer.toString(fragmentInfo.size()));
		}
		LinkedHashMap<String, DataNode> referenceInfo = null;
		if (options.has("refmapin"))
		{
			referenceInfo = ReferenceReader.readAllData(options);
			VerbosePrinter.println("Total references input: " + Integer.toString(referenceInfo.size()));
		}
		
		int optmapoutformat = (int) options.valueOf("optmapoutformat");
		OptMapDataWriter omrw_map = null;
		if (options.has("mapout"))
			omrw_map = new OptMapDataWriter((String) options.valueOf("mapout"), optmapoutformat);
		OptMapDataWriter omrw_unmap = null;
		if (options.has("unmapout"))
			omrw_unmap = new OptMapDataWriter((String) options.valueOf("unmapout"), optmapoutformat);

		BufferedWriter rocout = null;
		if (options.has(orocout))
			rocout = new BufferedWriter(new FileWriter(options.valueOf(orocout)));
		
		ResultsBreaker rbreaker = new ResultsBreaker(referenceInfo);
		rbreaker.setMode(options);
		rbreaker.setParameters(options);
		Filter filter = new Filter(referenceInfo);
		filter.setMode(options);
		filter.setParameters(options);
		LiftOver lo = new LiftOver(options);
		ResultClusterModule rcm = new ResultClusterModule(referenceInfo);
		
		rcm.setMode(options);
		rcm.setParameters(options);
		
		omrr.importFragInfo(fragmentInfo);
		omrr.importRefInfo(referenceInfo);
		
		int listcount = 0;
		int count = 0;
		HashSet<String> appearedMolecule = new HashSet<String>();
		List<OptMapResultNode> resultlist;
		int multiCorrect = 0;
		int multiWrong = 0;
		int uniqueCorrect = 0;
		int uniqueWrong = 0;
		while ((resultlist = omrr.readNextList()) != null)
		{
			if (resultlist.get(0).isUsed())
			{
				listcount++;
				count += resultlist.size();
			}
			
			DataNode fragment = resultlist.get(0).parentFrag;
			if (!dataid.isEmpty() && !dataid.contains(fragment.name)) // If users give a list, then only extract those
				continue;
			if (removeids != null && removeids.contains(fragment.name)) // If users provide a list to remove, then remove the data
				continue;
			if (fragmentInfo != null)
				appearedMolecule.add(fragment.name);

			
			// deal with invalid results
			if (disinvalid)
			{
				List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
				for (OptMapResultNode result : resultlist)
				{
					if (result.isUsed())
					{
						boolean pass = true;
						if (referenceInfo != null)
							if (!result.isSubRefInfoValid(referenceInfo))
								pass = false;
						if (result.parentFrag.refp != null)
							if (!result.isSubFragInfoValid())
								pass = false;
						if (pass)
							newresultlist.add(result);
						else
						{
							System.out.println("Invalid result " + result.parentFrag.name + " at " + result.mappedRegion.toString() + " is removed.");
						}
					}
				}
				resultlist = newresultlist;
			}
						
			// deal with regions
			if (region != null)
			{
				List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
				for (OptMapResultNode result : resultlist)
					if (result.isClose(region, 0))
						newresultlist.add(result);
				if (newresultlist.isEmpty())
					newresultlist.add(new OptMapResultNode(OptMapResultNode.newBlankMapNode(fragment)));
				resultlist = newresultlist;
			}
			
			// deal with results breaking
			{
				List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
				try
				{
					for (OptMapResultNode result : resultlist)
						newresultlist.addAll(rbreaker.breakResult(result));
					resultlist = newresultlist; // don't allow it to replace
				}
				catch (MissingInformationException | ResultsBreakingException e)
				{
					System.err.println("Exception occurs in breaking results: ");
					e.printStackTrace();
				}
			}
			// deal with results joining
			{
				if (options.has("joinresult")) {
					boolean join = (boolean) options.valueOf("joinresult");
					if (join) {
//						OptMapResultNode result = OptMapResultNode.newBlankMapNode(fragment);
						try {
							resultlist = rbreaker.joinResult(resultlist, true);
						} catch (MissingInformationException e) {
							e.printStackTrace();
						}
//						resultlist = new ArrayList<OptMapResultNode>();
//						resultlist.add(result);
					}
						
				}
			}			
			// deal with filtering
			{
				List<OptMapResultNode> newresultlist = filter.filter(resultlist);
//				for (OptMapResultNode result : resultlist)
//					if (filter.checkPass(result))
//						newresultlist.add(result);
				if (newresultlist.isEmpty())
					newresultlist.add(new OptMapResultNode(OptMapResultNode.newBlankMapNode(fragment)));
				resultlist = newresultlist;
			}
			// dealing with clustering
			{
				List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
				if (resultlist.get(0).isUsed())	
				{
					List<ClusteredResult> crList = rcm.standardcluster(resultlist, conf);
//					Collections.sort(crList);
//					Collections.reverse(crList);
//					if (conf)
//						rcm.processConfidence(crList);
					newresultlist = new ArrayList<OptMapResultNode>();
					for (ClusteredResult cr : crList)
						newresultlist.addAll(cr.updatedResult);
					boolean testing = true;
					if (newresultlist.size() > 0 && testing)
					{
						boolean mapped = false;
						boolean multi = crList.size() > 1;
						
						// temp only use one
//						while (crList.size() != 1)
//							crList.remove(crList.size() - 1);
						List<OptMapResultNode> testnewresultlist = new ArrayList<OptMapResultNode>();
						for (ClusteredResult cr : crList)
							testnewresultlist.addAll(cr.updatedResult);						
						for (OptMapResultNode result : testnewresultlist)
							if (result.parentFrag.simuInfo != null)
							{
								OptMapResultNode temp = new OptMapResultNode(result);
								lo.lift(temp);
								mapped = temp.correctlyMapped() || mapped;
							}
						int m = 0;
						int fn = 0;
						int fp = 0;
						double fragratio = 0;
						for (OptMapResultNode result : testnewresultlist)
						{
							m += result.getMatch();
							fp += result.getFP();
							fn += result.getFN();
							fragratio += result.getSubFragRatio();
						}
//						System.out.println(fragment.size + "," + fragment.getTotalSignal() + 
//								"," + testnewresultlist.size() + "," + crList.get(0).score + "," + testnewresultlist.get(0).confidence +
//								"," + m + "," + fp + "," + fn + "," + fragratio +
//								"," + mapped);
						if (mapped && multi)
							multiCorrect++;
						if (mapped && !multi)
							uniqueCorrect++;
						if (!mapped && multi)
							multiWrong++;
						if (!mapped && !multi)
							uniqueWrong++;
						
						if (rocout != null) {
							if (!multi)
								rocout.write(crList.get(0).score + "\t" + testnewresultlist.get(0).confidence + "\t" + mapped + "\n");
						}
//						boolean allmapped = mapped;
						// for checking
//						mapped = false;
//						while (crList.size() != 1)
//							crList.remove(crList.size() - 1);
//						testnewresultlist = new ArrayList<OptMapResultNode>();
//						for (ClusteredResult cr : crList)
//							testnewresultlist.addAll(cr.updatedResult);
//						for (OptMapResultNode result : testnewresultlist)
//							if (result.parentFrag.simuRegion != null)
//								mapped = result.correctlyMapped() || mapped;
//						if (mapped && multi)
//							multiCorrect++;
//						if (mapped && !multi)
//							uniqueCorrect++;
//						if (!mapped && multi)
//							multiWrong++;
//						if (!mapped && !multi)
//							uniqueWrong++;

						
//						if (allmapped && !mapped)
//							System.out.println(resultlist.get(0).parentFrag.id);
//						System.out.printf("%s\t%.4f\n", newresultlist.get(0).parentFrag.id, newresultlist.get(0).confidence);
					}
						
				}
//				else
//					newresultlist = resultlist;
				resultlist = newresultlist;
			}
			if (!resultlist.isEmpty())
			{
				for (OptMapResultNode result : resultlist)
					lo.lift(result);
			}
			
			if (refNameModify != null)
				if (!resultlist.isEmpty()) {
					for (OptMapResultNode result : resultlist) {
						String newKey = refNameModify.get(result.mappedRegion.ref);
						if (newKey != null)
							result.mappedRegion = new GenomicPosNode(newKey, result.mappedRegion.start, result.mappedRegion.stop);
					}
				}
			
			if (resultlist.isEmpty())
				resultlist.add(new OptMapResultNode(OptMapResultNode.newBlankMapNode(fragment)));
//			if (!resultlist.get(0).isUsed())
//				System.out.printf("%s\t%.4f\n", resultlist.get(0).parentFrag.id, 0.0);

			if (omrw != null)
				omrw.write(resultlist);
			OptMapResultNode result = resultlist.get(0);
			if (result.isUsed() && omrw_map != null)
				omrw_map.write(fragment);
			if (!result.isUsed() && omrw_unmap != null)
				omrw_unmap.write(fragment);
		}
		if (fragmentInfo != null)
			for (DataNode fragment : fragmentInfo.values())
				if (!appearedMolecule.contains(fragment.name))
				{
					if (omrw_unmap != null)
						omrw_unmap.write(fragment);
					if (omrw != null)
						omrw.write(new OptMapResultNode(OptMapResultNode.newBlankMapNode(fragment)));
//					System.out.printf("%s\t%.4f\n", fragment.id, 0.0);
				}
			
		omrr.close();
		if (omrw != null)
			omrw.close();
		if (omrw_map != null)
			omrw_map.close();
		if (omrw_unmap != null)
			omrw_unmap.close();
		if (rocout != null)
			rocout.close();
//		System.out.println("Total alignment results processed: " + Integer.toString(count));
//		System.out.println("Total aligned molecules processed: " + Integer.toString(listcount));
//		System.out.printf("Unique\t%d\t%d\n", uniqueCorrect, uniqueWrong);
//		System.out.printf("Multi\t%d\t%d\n", multiCorrect, multiWrong);
//		System.out.printf("Total\t%d\t%d\n", uniqueCorrect + multiCorrect, uniqueWrong + multiWrong);
		VerbosePrinter.println((uniqueCorrect + multiCorrect) + "\t" + (uniqueWrong + multiWrong));
	}
	
	
}
