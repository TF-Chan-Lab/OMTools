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


package aldenjava.opticalmapping.mapper.omblastmapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.mapper.ExtensionResult;
import aldenjava.opticalmapping.mapper.seeding.Kmer;
import aldenjava.opticalmapping.mapper.seeding.Seed;
import aldenjava.opticalmapping.mapper.seeding.SeedDatabase;

/**
 * The core module for <code>OMBlastMapper</code>
 * @author Alden
 *
 */
public class OMBlastCore {

	private LinkedHashMap<String, DataNode> optrefmap;

	private SeedExtension seedextensionmodule;
	private SeedDatabase seeddatabase;
	private int measure;
	private double ear;
	private int kmerlen;
	private int maxnosignalregion;
	private int maxSeedNumber;
	private boolean allowEqualRefQuery;
	private boolean allowDiffRefQuery;

	public OMBlastCore(LinkedHashMap<String, DataNode> optrefmap, SeedExtension seedextensionmodule, SeedDatabase seeddatabase, int measure, double ear, int kmerlen,
			int maxnosignalregion, int maxSeedNumber, boolean allowEqualRefQuery, boolean allowDiffRefQuery) {
		this.optrefmap = optrefmap;
		this.seedextensionmodule = seedextensionmodule;
		this.seeddatabase = seeddatabase;
		this.measure = measure;
		this.ear = ear;
		this.kmerlen = kmerlen;
		this.maxnosignalregion = maxnosignalregion;
		this.maxSeedNumber = maxSeedNumber;
		this.allowEqualRefQuery = allowEqualRefQuery;
		this.allowDiffRefQuery = allowDiffRefQuery;
	}

	public OMBlastCore(LinkedHashMap<String, DataNode> optrefmap) {
		this.optrefmap = optrefmap;
	}

	public void setParameters(int seedingmode, int kmerlen, int maxnosignalregion, boolean allowLocalAlignment, int measure, double ear, int matchscore, int falseppenalty, int falsenpenalty,
			int falselimit, int maxSeedNumber, boolean allowEqualRefQuery, boolean allowDiffRefQuery) {
		this.kmerlen = kmerlen;
		this.maxnosignalregion = maxnosignalregion;
		this.measure = measure;
		this.ear = ear;
		this.maxSeedNumber = maxSeedNumber;
		seeddatabase = new SeedDatabase(DataNode.getKmerWord(optrefmap, kmerlen, maxnosignalregion), kmerlen);
		seeddatabase.setMode(seedingmode);
		seeddatabase.setParameters(kmerlen, maxnosignalregion);
		seeddatabase.buildDatabase();
		this.seedextensionmodule = new SeedExtension(optrefmap);
		this.seedextensionmodule.setParameters(measure, ear, matchscore, falseppenalty, falsenpenalty, falselimit, allowLocalAlignment);
		this.allowEqualRefQuery = allowEqualRefQuery;
		this.allowDiffRefQuery = allowDiffRefQuery;
	}

	/**
	 * Restricts the regions for alignment. This method rebuilds the
	 * <code>seedDatabase</code>. The database is not rebuilt if regionList
	 * remains <code>null</code> for previous <code>data</code> and this
	 * <code>data</code>.
	 * 
	 * @param regionList
	 */
	public void restrictRegion(List<GenomicPosNode> regionList) {
		seeddatabase.assignSeeds(DataNode.getKmerWord(optrefmap, kmerlen, maxnosignalregion, regionList), kmerlen);
		seeddatabase.buildDatabase();
	}

	/**
	 * Perform seed and extend on the data in forward direction only.
	 * 
	 * @param data <code>data</code> for seed-and-extend
	 * @return Extension results
	 */
	private List<ExtensionResult> seedAndExtend(DataNode data) {
		List<ExtensionResult> extensionresultlist = new ArrayList<ExtensionResult>();
		List<Kmer> dataKmerList = data.getKmerWord(kmerlen, maxnosignalregion);
		
		// Remove high-density regions
		SeedDatabase tDatabase = new SeedDatabase(dataKmerList, kmerlen);
		tDatabase.setMode(1);
		tDatabase.setParameters(kmerlen, maxnosignalregion);
		dataKmerList = tDatabase.filter(dataKmerList, ear, measure, maxSeedNumber, 100);

//		List<Seed> pooledseedlist = new ArrayList<Seed>();
//		for (Kmer fragmentkmer : dataKmerList) {
//			List<Seed> seedlist = seeddatabase.getJoinedSeed(fragmentkmer, ear, measure);
//			pooledseedlist.addAll(seedlist);
//		}
		List<Seed> pooledseedlist = seeddatabase.getJoinedSeed(dataKmerList, ear, measure);
		// Extension
		for (Seed seed : pooledseedlist) {
			if (!allowEqualRefQuery && seed.source.equals(seed.kmerpointer.source))
				continue;
			if (!allowDiffRefQuery && !seed.source.equals(seed.kmerpointer.source)) // The implementation here isn't good. Should build a database for its own
				continue;
			ExtensionResult tmpresult = seedextensionmodule.extension(data, seed);
			if (tmpresult != null)
				extensionresultlist.add(tmpresult);
		}

		return extensionresultlist;
	}

	/**
	 * Performs alignments by seed-and-extending forward and reverse data.
	 * 
	 * @param data
	 *            <code>data</code> to be aligned
	 * @return Partial alignment results
	 * @see #seedAndExtend(DataNode)
	 */
	public List<OptMapResultNode> getResult(DataNode data) {
		if (data.getTotalSegment() - 2 < kmerlen)
			return null;
		List<OptMapResultNode> fragmentmaplist = new ArrayList<OptMapResultNode>();

		// forward
		List<ExtensionResult> forwardlist = seedAndExtend(data);
		for (ExtensionResult extensionresult : forwardlist)
			fragmentmaplist.add(extensionresult.toAlignment(data, optrefmap, 1));

		// reverse
		DataNode reversedfragment = data.getReverse();
		List<ExtensionResult> reverselist = seedAndExtend(reversedfragment);
		for (ExtensionResult extensionresult : reverselist)
			fragmentmaplist.add(extensionresult.toAlignment(data, optrefmap, -1));
		return fragmentmaplist;
	}

	/**
	 * Creates and returns a copy of this object.
	 * 
	 * @return a copy of this instance
	 */
	public OMBlastCore copy() {
		return new OMBlastCore(optrefmap, seedextensionmodule.copy(), seeddatabase.copy(), measure, ear, kmerlen, maxnosignalregion, maxSeedNumber, allowEqualRefQuery, allowDiffRefQuery);
	}
}