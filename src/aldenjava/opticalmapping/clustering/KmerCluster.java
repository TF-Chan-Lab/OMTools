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


package aldenjava.opticalmapping.clustering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aldenjava.opticalmapping.data.Identifiable;
import aldenjava.opticalmapping.mapper.seeding.Kmer;

/**
 * A class to store a cluster of kmers.  
 * @author Alden
 *
 */
public class KmerCluster implements Identifiable<Integer> {
	public final int clusterID;
	public final int k;
	public final List<Kmer> kmerList;
	public final Map<Kmer, Integer> kmerMap;
	private double[] computedConsensus = null;
	public KmerCluster(int clusterID, int k, KmerCluster... kcs) {
		this.clusterID = clusterID;
		this.k = kcs[0].k;
		kmerList = new ArrayList<>();
		kmerMap = new HashMap<>();
		
		computedConsensus = new double[k];
		for (KmerCluster kc : kcs)
			this.addKmerCluster(kc, 1);
	}
	
	public KmerCluster(int clusterID, int k) {
		this.clusterID = clusterID;
		this.k = k;
		kmerMap = new HashMap<>();
		kmerList = null;
		
	}
	
	
	public void addKmer(Kmer kmer) {
		// Assume a forward kmer in the cluster if not clearly specified
		addKmer(kmer, 1);
	}

	public void addKmer(Kmer kmer, int strand) {
		if (kmerMap.containsKey(kmer)) {
			if (kmerMap.get(kmer) != strand)
				this.setAllKmerStrandsToZero();
		}
		else
			kmerMap.put(kmer, strand);
		computedConsensus = null;
	}

	public void removeKmer(Kmer kmer) {
		kmerMap.remove(kmer);
		computedConsensus = null;
	}

	public int getCount() {
		return kmerMap.size();
	}
	private void computeConsensus() {
		double[] finalSizes = new double[k];
		for (Kmer kmer : kmerMap.keySet()) {
			int strand = kmerMap.get(kmer);
			long[] sizes;
			switch (strand) {
				case 1: sizes = kmer.getForwardSizes(); break;
				case -1: sizes = kmer.getReverseSizes(); break;
				case 0: sizes = kmer.getAverageSizes(); break;
				default: throw new IllegalArgumentException("Invalid strand information for " + kmer.source + ": " + kmer.pos + " - " + strand);
			}
			for (int i = 0; i < k; i++)
				finalSizes[i] += sizes[i];
		}
		computedConsensus = finalSizes;
	}
	
	public double[] getConsensusSizes() {
		if (computedConsensus == null)
			computeConsensus();
		return computedConsensus;
	}
	@Override
	public boolean equals(Object o) {
		if (o instanceof KmerCluster)
			return (((KmerCluster) o).clusterID == this.clusterID);
		return false;
	}
	@Override
	public int hashCode() {
		return clusterID;
	}	
	@Override
	public Integer getIdentifier() {
		return this.clusterID;
	}

	public void initializeConsensus(Kmer kmer) {
		computedConsensus = new double[kmer.k()];
		for (int i = 0; i < kmer.k(); i++)
			computedConsensus[i] = kmer.get(i);
	}

	public void addKmerCluster(KmerCluster kc, int strand) {
		for (Kmer kmer : kc.kmerMap.keySet())
			this.addKmer(kmer, kc.kmerMap.get(kmer) * strand);
	}

	public boolean hasKmer(Kmer kmer) {
		return kmerMap.containsKey(kmer);
	}
	public int getKmerStrand(Kmer kmer) {
		return kmerMap.get(kmer);
	}

	private void setAllKmerStrandsToZero() {
		List<Kmer> kmerList = new ArrayList<>(kmerMap.keySet());
		for (Kmer kmer : kmerList)
			kmerMap.put(kmer, 0);
	}

	public boolean containMultipleKmersFromSameSource() {
		HashSet<String> sources = new HashSet<>();
		for (Kmer kmer : kmerList)
			if (!sources.add(kmer.source))
				return true;
		return false;
	}
	
	public static Comparator<KmerCluster> kmerCountComparator = new Comparator<KmerCluster>() {
		@Override
		public int compare(KmerCluster kc1, KmerCluster kc2) {
			return Integer.compare(kc1.getCount(), kc2.getCount());
		}
		
	};
	public static LinkedHashMap<Integer, KmerCluster> createKmerClusters(List<Integer> clusterList, List<Kmer> kmerList, int k) {
		assert clusterList.size() == kmerList.size();
		LinkedHashMap<Integer, KmerCluster> kmerClusters = new LinkedHashMap<>();
		for (int i = 0; i < clusterList.size(); i++) {
			int clusterID = clusterList.get(i);
			KmerCluster cluster = kmerClusters.get(clusterID);
			if (cluster == null) {
				cluster = new KmerCluster(clusterID, k);
				kmerClusters.put(clusterID, cluster);
			}
			cluster.addKmer(kmerList.get(i));
		}
		return kmerClusters;
	}

}

