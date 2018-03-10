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


package aldenjava.opticalmapping.mapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import joptsimple.OptionSet;
import aldenjava.common.TimeCounter;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultReader;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultWriter;
import aldenjava.opticalmapping.mapper.clustermodule.ClusteredResult;
import aldenjava.opticalmapping.mapper.clustermodule.ResultClusterModule;
import aldenjava.opticalmapping.mapper.multithread.*;
import aldenjava.opticalmapping.mapper.omblastmapper.OMBlastMapper;
import aldenjava.opticalmapping.mapper.omfmmapper.OMFMMapper;
import aldenjava.opticalmapping.mapper.omhamapper.OMHAMapper;
import aldenjava.opticalmapping.mapper.postmappingmodule.Filter;
import aldenjava.opticalmapping.mapper.postmappingmodule.PostMapJoining;
import aldenjava.opticalmapping.miscellaneous.Copyable;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

/**
 * Mapper is the abstract base class for all OM Mappers. It provides all post-mapping modules.
 * 
 * @author Alden Leung
 */
public abstract class Mapper implements Callable<List<OptMapResultNode>>, Copyable<Mapper> {
	/**
	 * The reference.
	 */
	protected final LinkedHashMap<String, DataNode> optrefmap;

	/**
	 * For each <code>data</code> to be aligned, a list of regions is provided to restrict the alignment regions. If no list is provided for the <code>data</code>, there should be no restriction for the alignment regions
	 */
	private LinkedHashMap<String, List<GenomicPosNode>> targetRegionMap;

	/**
	 * Module used to resolve overlapping partial alignments
	 * 
	 * @see PostMapJoining
	 */
	private PostMapJoining pmj;
	/**
	 * Module used to filter unused
	 * 
	 * @see Filter
	 */
	private Filter filter;
	/**
	 * Module used to resolve join partial alignments
	 * 
	 * @see ResultClusterModule
	 */
	private ResultClusterModule rcm;

	/**
	 * Minimum signal of the data to be aligned
	 */
	private int minSignal = 5;
	/**
	 * Minimum size of the data to be aligned
	 */
	private int minSize = 0;
	/**
	 * Allow exact match. If not allowed, any alignment result showing <code>data</code> aligns to itself as a whole is removed. Note that the result is not removed if only part of <code>data</code> is aligned. Exact match should be set false for self-alignment and pairwise alignment.
	 */
	private boolean exactmatch = true;

	public TimeCounter tc = new TimeCounter(3, "Alignment Time", "Result PostProcessing Time", "Result Clustering Time");

	/**
	 * The data to be aligned. <code>data</code> is set before alignment
	 * 
	 * @see #setData(DataNode)
	 * @see #getData()
	 */
	private DataNode data = null;

	public Mapper(LinkedHashMap<String, DataNode> optrefmap) {
		this.optrefmap = optrefmap;
	}

	public void setParameters(OptionSet options) throws IOException {
		this.setParameters((int) options.valueOf("minsig"), (int) options.valueOf("minsize"), (boolean) options.valueOf("exactmatch"));
		pmj = new PostMapJoining(optrefmap);
		pmj.setMode(options);
		pmj.setParameters(options);

		filter = new Filter(optrefmap);
		filter.setMode(options);
		filter.setParameters(options);

		rcm = new ResultClusterModule(optrefmap);
		rcm.setMode(options);
		rcm.setParameters(options);

		if (options.has("optresin"))
			targetRegionMap = OptMapResultNode.getPotentiallyMappedRegion(optrefmap, OptMapResultReader.readAllDataInList(options));
		else
			targetRegionMap = null;
	}

	public void setParameters(int minSignal, int minSize, boolean exactmatch) {
		this.minSignal = minSignal;
		this.minSize = minSize;
		this.exactmatch = exactmatch;
	}

	public void setPostAlignmentProcess(PostMapJoining pmj, Filter filter, ResultClusterModule rcm) {
		this.pmj = pmj;
		this.filter = filter;
		this.rcm = rcm;
	}
	/**
	 * Set <code>data</code> for alignment
	 * 
	 * @param data
	 * @see #getData
	 */
	public void setData(DataNode data) {
		this.data = data;
	}

	/**
	 * Return <code>data</code> being aligned
	 * 
	 * @return <code>data</code>
	 * @see #setData
	 */
	public DataNode getData() {
		return this.data;
	}

	/**
	 * Get partial alignment results from alignment of <code>data</code> onto the reference, where the alignment is restricted by <code>regionList</code>.
	 * 
	 * @param data
	 *            <code>data</code> to be aligned
	 * @param regionList
	 *            Restricted regions. An empty list represent the <code>data</code> can be aligned on nowhere. <code>null</code> represented no such information is provided and <code>data</code> can be aligned anywhere on the reference
	 * @return Partial alignment results
	 */
	public abstract List<OptMapResultNode> getResult(DataNode data, List<GenomicPosNode> regionList);

	public List<OptMapResultNode> processClusterAndConfidence(List<OptMapResultNode> fragmentmaplist) {
		if (fragmentmaplist == null)
			return null;
		if (fragmentmaplist.isEmpty())
			return new ArrayList<OptMapResultNode>();
		List<OptMapResultNode> finalResult = new ArrayList<OptMapResultNode>();
		List<ClusteredResult> crList = rcm.standardcluster(fragmentmaplist, true);
		// rcm.processConfidence(crList);

		for (ClusteredResult cr : crList)
			finalResult.addAll(cr.updatedResult);
		return finalResult;
	}

	/**
	 * Three main steps:
	 * <ol>
	 * <li>getResult implemented by individual mapper, for the first wave alignment</li>
	 * <li>filter and post map joining module</li>
	 * <li>clustering module</li>
	 * </ol>
	 * 
	 * @return Final results.
	 */
	@Override
	public List<OptMapResultNode> call() {
		if (this.data == null)
			return null;

		if (data.getTotalSignal() < minSignal || data.size < minSize)
			return null;

		tc.start(0);
		
		List<OptMapResultNode> alignmentList;
		if (targetRegionMap == null)
			alignmentList = getResult(data, null);
		else
			if (targetRegionMap.containsKey(data.name)) 
				alignmentList = getResult(data, targetRegionMap.get(data.name));
			else
				alignmentList = getResult(data, new ArrayList<GenomicPosNode>());
		
		tc.end(0);
		if (alignmentList == null)
			return null;

		if (!exactmatch) {
			for (int i = alignmentList.size() - 1; i >= 0; i--) {
				OptMapResultNode result = alignmentList.get(i);
				if (result.parentFrag.name.equals(result.mappedRegion.ref) && result.mappedstrand == 1 && result.parentFrag.getTotalSignal() == result.getMatch())
					alignmentList.remove(i);
			}
		}

		tc.start(1);
		if (pmj != null)
			alignmentList = pmj.join(alignmentList);
		if (filter != null)
			alignmentList = filter.filter(alignmentList);
		tc.end(1);

		tc.start(2);
		if (rcm != null)		
			alignmentList = processClusterAndConfidence(alignmentList);
		tc.stop(2);
		return alignmentList;

	}

	public void setCopyMapperParameters(Mapper mapper) {
		// mapper.setParameters(minsubfragment, scorecutoff, maxabovescoreitem,
		// maxitem);
		mapper.setParameters(minSignal, minSize, exactmatch);
		if (this.pmj != null)
			mapper.pmj = this.pmj.copy();
		else
			mapper.pmj = null;
		if (this.filter != null)
			mapper.filter = this.filter.copy();
		else
			mapper.filter = null;
		if (this.rcm != null)
			mapper.rcm = this.rcm.copy();
		else
			mapper.rcm = null;
		mapper.targetRegionMap = this.targetRegionMap;
	}

	protected static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Common Mapper Options", level);
		parser.accepts("minsig", "Minimum signal of the query to align.").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		parser.accepts("minsize", "Minimum size of the query to align.").withRequiredArg().ofType(Integer.class).defaultsTo(50000);
		parser.accepts("exactmatch", "Enable exact match of query to reference. Disable this option when performing self-alignment.").withRequiredArg().ofType(Boolean.class).defaultsTo(true);

		PostMapJoining.assignOptions(parser, level + 1);
		Filter.assignOptions(parser, level + 1);
		ResultClusterModule.assignOptions(parser, level + 1);

		OptMapResultReader.assignOptions(parser, level + 1);

		// optresin is optional
		parser.addHeader(null, 0);
		parser.accepts("optresin", "Input alignment result file for re-alignment").withRequiredArg().ofType(String.class);
	}

	public static void standardMapperProcedure(String[] args, Class<? extends Mapper> mapperclass) throws IOException, MapperConstructionException {
		TimeCounter tc = new TimeCounter(5, "Initialization Time", "Alignment Time", "Result PostProcessing Time", "Result Clustering Time", "Real World Time");
		tc.start(4);
		tc.start(0);
		String explanation = "Performs alignment of optical mapping data. ";
		if (mapperclass == OMBlastMapper.class)
			explanation += "OMBlast algorithm employs a seed-and-extend approach to align optical maps.";
		if (mapperclass == OMHAMapper.class)
			explanation += "OMHA algorithm employs a heuristic approach to align optical maps.";
		if (mapperclass == OMFMMapper.class)
			explanation += "OMFM algorithm employs an indexing approach to align optical maps.";
		ExtendOptionParser parser = new ExtendOptionParser(mapperclass.getSimpleName(), explanation);

		try {
			mapperclass.getMethod("assignOptions", ExtendOptionParser.class, int.class).invoke(null, parser, 1);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new MapperConstructionException(e);
		}
		MultiThreadMapper.assignOptions(parser, 1);
		ReferenceReader.assignOptions(parser, 1);
		OptMapDataReader.assignOptions(parser, 1);
		OptMapResultWriter.assignOptions(parser, 1);
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		OptMapDataReader omdr = new OptMapDataReader(options);
		ReferenceReader refreader = new ReferenceReader(options);
		LinkedHashMap<String, DataNode> optrefmap = refreader.readAllData();
		MultiThreadMapper multi = new MultiThreadMapper(mapperclass, optrefmap);
		multi.setParameters(options);
		OptMapResultWriter omrw = new OptMapResultWriter(options);

		tc.end(0);
		DataNode fragment;
		try {
			while ((fragment = omdr.read()) != null) {
				while (!multi.startNext(fragment)) {
					MultiThreadResultNode multinode = multi.getNextResult();
					List<OptMapResultNode> resultlist = multinode.alignmentResults;
					if (resultlist == null || resultlist.size() == 0) {
						resultlist = new ArrayList<OptMapResultNode>();
						resultlist.add(OptMapResultNode.newBlankMapNode(multinode.data));
					}
					omrw.write(resultlist);
				}

			}
			while (multi.getStatus() != -1) {

				MultiThreadResultNode multinode = multi.getNextResult();
				List<OptMapResultNode> resultlist = multinode.alignmentResults;
				if (resultlist == null || resultlist.size() == 0) {
					resultlist = new ArrayList<OptMapResultNode>();
					resultlist.add(OptMapResultNode.newBlankMapNode(multinode.data));
				}
				omrw.write(resultlist);
			}

		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			// Unknown reason for interruption, but should continue to handle
			// the result.
		}
		omdr.close();
		omrw.close();
		TimeCounter mappertc = multi.getMappingTime();
		multi.close();
		tc.set(1, mappertc.get(0));
		tc.set(2, mappertc.get(1));
		tc.set(3, mappertc.get(2));
		tc.end(4);
		tc.outputtime();

	}
}