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


package aldenjava.opticalmapping.clustering;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.Identifiable;
import aldenjava.opticalmapping.mapper.seeding.Kmer;

public class KmerCluster implements Identifiable<Integer> {
	public final int clusterID;
	public final int k;
	public final List<Kmer> kmerList;
	public final List<Boolean> forwardList;
	public final List<Boolean> reverseList;
	private double[] computedConsensus = null;
	public KmerCluster(int clusterID, KmerCluster... kcs) {
		this.clusterID = clusterID;
		this.k = kcs[0].k;
		kmerList = new ArrayList<>();
		forwardList = new ArrayList<>();
		reverseList = new ArrayList<>();
		computedConsensus = new double[k];
		for (KmerCluster kc : kcs) {
			kmerList.addAll(kc.kmerList);
			forwardList.addAll(kc.forwardList);
			reverseList.addAll(kc.reverseList);
			double[] sizes = kc.getConsensusSizes();
			for (int i = 0; i < k; i++)
				computedConsensus[i] += sizes[i] * kc.kmerList.size();
		}
		for (int i = 0; i < k; i++)
			computedConsensus[i] /= kmerList.size();
	}
	
	public KmerCluster(int clusterID, int k) {
		this.clusterID = clusterID;
		this.k = k;
		kmerList = new ArrayList<>();
		forwardList = new ArrayList<>();
		reverseList = new ArrayList<>();
	}
	
	
	public void addKmer(Kmer kmer) {
		addKmer(kmer, true, false); // Assume a forward kmer if not clearly specified
//		kmerList.add(kmer);
//		computedConsensus = null; // Implement later
	}
	public void addKmer(Kmer kmer, boolean forward, boolean reverse) {
		kmerList.add(kmer);
		computedConsensus = null; // Implement later
	}

	public void removeKmer(Kmer kmer) {
		// Slow using list
		int index = kmerList.indexOf(kmer);
		kmerList.remove(index);
		forwardList.remove(index);
		reverseList.remove(index);
		computedConsensus = null;
	}

	public int getCount() {
		return kmerList.size();
	}
	private void computeConsensus() {
		// Average
		double[] sizes = new double[k];
		for (int index = 0; index < kmerList.size(); index++) {
			Kmer kmer = kmerList.get(index);
			// If forward, no matter reverse matches, only count forward
			// If not forward, no matter reverse matches, only count reverse 
			if (forwardList.get(index))
				for (int i = 0; i < k; i++)
					sizes[i] += kmer.get(i);
			else
				for (int i = 0; i < k; i++)
					sizes[k - i - 1] += kmer.get(i);
			
		}
		for (int i = 0; i < k; i++)
			sizes[i] /= kmerList.size();
		computedConsensus = sizes;
	}
	
	public double[] getConsensusSizes() {
		if (computedConsensus == null)
			computeConsensus();
		return computedConsensus;
	}
	@Override
	public int hashCode() {
		return clusterID;
	}
	
	public static Comparator<KmerCluster> kmerCountComparator = new Comparator<KmerCluster>() {
		@Override
		public int compare(KmerCluster kc1, KmerCluster kc2) {
			return Integer.compare(kc1.getCount(), kc2.getCount());
		}
		
	};
	public boolean containMultipleKmersFromSameSource() {
		HashSet<String> sources = new HashSet<>();
		for (Kmer kmer : kmerList)
			if (!sources.add(kmer.source))
				return true;
		return false;
	}
	
	
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

	@Override
	public Integer getIdentifier() {
		return this.clusterID;
	}

	public void initializeConsensus(Kmer kmer) {
		computedConsensus = new double[kmer.k()];
		for (int i = 0; i < kmer.k(); i++)
			computedConsensus[i] = kmer.get(i);
	}

}

