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


package aldenjava.opticalmapping.mapper.seeding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.data.data.ReferenceSignal;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.SelectableMode;

/**
 * The SeedDatabase stores reference kmers. It offers a list of reference kmers as seeds upon a query kmer request
 * 
 * @author Alden
 *
 */

public class SeedDatabase implements SelectableMode {

	private int seedingmode;
	private List<List<Kmer>> databaseSeedList;
//	private FastConversionTable table = FastConversionTable.standardTable();
	private FastConversionTable table = FastConversionTable.advancedTable(0.1, 500, 5000, Integer.MAX_VALUE);
//	private FastConversionTable table = FastConversionTable.advancedTable(0.1, 500, 6000, Integer.MAX_VALUE);
	private LinkedHashMap<SeedTableKey, List<Kmer>> fastDatabaseSeedMap;

	private int k;
	private int maxnosignalregion;

	public SeedDatabase(List<List<Kmer>> databaseSeedList) {
		assignSeeds(databaseSeedList);
	}

	public SeedDatabase(List<Kmer> kmerList, int kmerlen) {
		assignSeeds(kmerList, kmerlen);
	}

	@Override
	public void setMode(OptionSet options) {
		setMode((int) options.valueOf("seedingmode"));
	}

	@Override
	public void setMode(int mode) {
		if (!ArrayUtils.contains(new int[]{1, 2, -1}, mode))
			throw new IllegalArgumentException("Seeding mode must be 1, 2, or -1");
		this.seedingmode = mode;
	}

	@Override
	public int getMode() {
		return seedingmode;
	}

	public void setParameters(OptionSet options) {
		setParameters((int) options.valueOf("k"), (int) options.valueOf("maxnosignal"));
	}
	public void setParameters(int k) {
		this.k = k;
	}

	public void setParameters(int k, int maxnosignalregion) {
		this.k = k;
		this.maxnosignalregion = maxnosignalregion;
	}

	public void assignSeeds(List<List<Kmer>> databaseSeedList) {
		this.databaseSeedList = databaseSeedList;
	}
	public void assignSeeds(List<Kmer> kmerList, int kmerlen) {
		this.databaseSeedList = convertKmerList(kmerList, kmerlen);
		this.k = kmerlen;
	}
	
	
	public void buildDatabase() {
		switch (seedingmode) {
			case -1:
				if (k > 10)
					seedingmode = 1;
				else
					seedingmode = 2;
				buildDatabase();
				break;
			case 1:
				buildSortListDatabase();
				break;
			case 2:
				buildBinningDatabase();
				break;
			default:
				System.err.println("Warning! Unknown mode " + Integer.toString(seedingmode));
				buildSortListDatabase();
				break;
		}
	}

	private void buildSortListDatabase() {
		for (int i = 0; i < k; i++)
			Collections.sort(databaseSeedList.get(i), Kmer.comparator(i));
		// Nothing else to do
	}

	private void buildBinningDatabase() {
		processFastAccessDatabase(k);
	}

	private void processFastAccessDatabase(int k) {
		// generate words instead of
		fastDatabaseSeedMap = new LinkedHashMap<SeedTableKey, List<Kmer>>();
		List<Kmer> kmerlist = databaseSeedList.get(0);
		for (Kmer kmer : kmerlist) {
			SeedTableKey key = table.getKey(kmer);
			if (!fastDatabaseSeedMap.containsKey(key))
				fastDatabaseSeedMap.put(key, new ArrayList<Kmer>());
			fastDatabaseSeedMap.get(key).add(kmer);
		}

	}

	private List<List<Kmer>> convertKmerList(List<Kmer> kmerList, int kmerlen) {
		if (kmerList == null)
			return null;
		List<List<Kmer>> kmerlistlist = new ArrayList<List<Kmer>>(); // each sort by kmer pos 1, pos 2, pos 3...

		for (int i = 0; i < kmerlen; i++) {
			List<Kmer> dummyrefkmerlist = new ArrayList<Kmer>(kmerList);
			kmerlistlist.add(dummyrefkmerlist);
		}
		return kmerlistlist;
	}

	public List<Kmer> getKmerListFromBinning(Kmer kmer, double ear, int measure) {
		Kmer smallKmer = kmer.newKmer(1 - ear, measure * -1);
		Kmer largeKmer = kmer.newKmer(1 + ear, measure);
		List<SeedTableKey> keyList = table.getMatchingKeys(smallKmer, largeKmer, fastDatabaseSeedMap.size(), fastDatabaseSeedMap.keySet());
		
		List<Kmer> kmerList = new ArrayList<Kmer>();
		for (SeedTableKey key : keyList) {
			List<Kmer> potentialKmerList = fastDatabaseSeedMap.get(key);
			assert (potentialKmerList != null);
			for (Kmer refkmer : potentialKmerList) {
				boolean wrong = false;
				for (int i = 0; i < refkmer.k(); i++)
					if (refkmer.get(i) < smallKmer.get(i) || refkmer.get(i) > largeKmer.get(i)) {
						wrong = true;
						break;
					}
				if (!wrong) {
					if (refkmer.limitRange(kmer, measure, ear))
						kmerList.add(refkmer);
				}
			}
		}
		
		return kmerList;
	}

	public void testKmer(Kmer kmer, double ear, int measure) {
		table.testKmer(kmer, ear, measure);
	}
	public List<Kmer> getKmerListFromSortList(Kmer kmer, double ear, int measure) {
		List<Kmer> matchedkmerlist = null;
		Kmer smallkmer = kmer.newKmer(1 - ear, measure * -1);
		Kmer largekmer = kmer.newKmer(1 + ear, measure);
		// long smalllen = smallkmer.length();
		// long largelen = largekmer.length();
		// HashSet<Kmer> kmerHash = new HashSet<Kmer>();
		for (int i = 0; i < kmer.k(); i++) {
			List<Kmer> refkmerlist = databaseSeedList.get(i);
			int startpos = Collections.binarySearch(refkmerlist, smallkmer, Kmer.comparator(i));
			if (startpos < 0)
				startpos = (startpos + 1) * -1;
			else {
				while (startpos >= 0) {
					if (smallkmer.compare(refkmerlist.get(startpos), i) > 0)
						break;
					startpos--;
				}
				startpos++;
			}
			int stoppos = Collections.binarySearch(refkmerlist, largekmer, Kmer.comparator(i));
			if (stoppos < 0)
				stoppos = (stoppos + 1) * -1;
			else {
				while (stoppos < refkmerlist.size()) {
					if (largekmer.compare(refkmerlist.get(stoppos), i) < 0)
						break;
					stoppos++;
				}
			}
			stoppos--;

			List<Kmer> sublist = refkmerlist.subList(startpos, stoppos + 1);
			if (matchedkmerlist == null) {
				matchedkmerlist = new ArrayList<Kmer>(sublist);
				// matchedkmerlist = new ArrayList<Kmer>();
				// for (Kmer km : sublist)
				// {
				// if (km.len <= largelen && km.len >= smalllen)
				// matchedkmerlist.add(km);
				// }
			} else {
				List<Kmer> newlist = new ArrayList<Kmer>();
				if (matchedkmerlist.size() > sublist.size()) {
					Set<Kmer> subset = new HashSet<Kmer>(sublist);
					for (Kmer candidatekmer : matchedkmerlist)
						if (subset.contains(candidatekmer))
							newlist.add(candidatekmer);
				} else {
					Set<Kmer> subset = new HashSet<Kmer>(matchedkmerlist);
					for (Kmer candidatekmer : sublist) {
						if (subset.contains(candidatekmer))
							newlist.add(candidatekmer);
					}
				}
				matchedkmerlist = newlist;
			}

		}
		List<Kmer> kmerList = new ArrayList<Kmer>();
		for (Kmer matchedkmer : matchedkmerlist) {
			if (matchedkmer.limitRange(kmer, measure, ear))
				kmerList.add(matchedkmer);
		}
		return kmerList;

	}

	public List<Kmer> getKmerList(Kmer kmer, double ear, int measure) {
		switch (seedingmode) {
			case -1:
				if (k > 10)
					seedingmode = 1;
				else
					seedingmode = 2;
				return getKmerList(kmer, ear, measure);
			case 1:
				return getKmerListFromSortList(kmer, ear, measure);
			case 2:
				return getKmerListFromBinning(kmer, ear, measure);
			default:
				System.err.println("Warning! Unknown mode " + Integer.toString(seedingmode));
				System.exit(0);
				return getKmerListFromSortList(kmer, ear, measure);
		}

	}

	public List<Seed> getSeed(Kmer kmer, double ear, int measure) {

		List<Kmer> matchedkmerlist = getKmerList(kmer, ear, measure);
		List<Seed> seedlist = new ArrayList<Seed>();
		for (Kmer matchedkmer : matchedkmerlist) {

			Seed s = new Seed(matchedkmer, new Kmer(kmer));
			if (s.limitRange(measure, ear))
				seedlist.add(s);
		}
		return seedlist;

	}

	public List<Seed> getJoinedSeed(List<Kmer> kmerList, double ear, int measure) {
		List<Seed> seedList = new ArrayList<>();
		for (Kmer kmer : kmerList)
		  seedList.addAll(getSeed(kmer, ear, measure)); // This can join the seeds at this updated version
		return seedList;
//		return seedJoin2(seedList, k);
	}

	/**
	 * A method to concatenate close seeds into one seed. Saves time for extension but decreases accuracy
	 * @param pooledSeedList
	 * @param k
	 * @return
	 */
	public List<Seed> seedJoin2(List<Seed> pooledSeedList, int k) {
		LinkedHashMap<ReferenceSignal, LinkedHashMap<ReferenceSignal, Seed>> kmerPosMap = new LinkedHashMap<>();
		for (Seed seed : pooledSeedList) {
			ReferenceSignal refSig = new ReferenceSignal(seed.source, seed.pos);
			ReferenceSignal kmerRefSig = new ReferenceSignal(seed.kmerpointer.source, seed.kmerpointer.pos);
			LinkedHashMap<ReferenceSignal, Seed> posMap = kmerPosMap.get(kmerRefSig);
			if (posMap == null) {
				posMap = new LinkedHashMap<ReferenceSignal, Seed>();
				kmerPosMap.put(kmerRefSig, posMap);
			}
			posMap.put(refSig, seed);
		}
		List<ReferenceSignal> kmerRefSigList = new ArrayList<>(kmerPosMap.keySet()); 
		Collections.sort(kmerRefSigList);
		List<Seed> joinedSeedList = new ArrayList<>();
		for (ReferenceSignal kmerRefSig : kmerRefSigList) {
			
			for (ReferenceSignal refSig : new ArrayList<>(kmerPosMap.get(kmerRefSig).keySet())) {
				List<Seed> seedToJoin = new ArrayList<>();
				
				ReferenceSignal nextKmerRefSig = new ReferenceSignal(kmerRefSig.ref, kmerRefSig.refpPos);
				ReferenceSignal nextRefSig = new ReferenceSignal(refSig.ref, refSig.refpPos);
				while (kmerPosMap.containsKey(nextKmerRefSig) && kmerPosMap.get(nextKmerRefSig).containsKey(nextRefSig)) { 
					seedToJoin.add(kmerPosMap.get(nextKmerRefSig).remove(nextRefSig));
					nextKmerRefSig = new ReferenceSignal(nextKmerRefSig.ref, nextKmerRefSig.refpPos + 1);
					nextRefSig = new ReferenceSignal(nextRefSig.ref, nextRefSig.refpPos + 1);
				}
				List<Long> seedSize = new ArrayList<>();
				List<Long> kmerSize = new ArrayList<>();
				int pter = 0;
				for (Seed seed : seedToJoin) {
					for (int i = 0; i < k; i++)
						if (pter + i >= seedSize.size()) {
							seedSize.add(seed.get(i));
							kmerSize.add(seed.kmerpointer.get(i));
						}
						else {
							seedSize.set(pter + i, seed.get(i));
							kmerSize.set(pter + i, seed.kmerpointer.get(i));
						}
							
					pter++;
				}
				joinedSeedList.add(new Seed(new Kmer(seedToJoin.get(0).source, seedToJoin.get(0).pos, seedSize), new Kmer(seedToJoin.get(0).kmerpointer.source, seedToJoin.get(0).kmerpointer.pos, kmerSize)));
			}
		}
		return joinedSeedList;
	}
	
	/*
	public List<Seed> seedJoin(List<Seed> pooledseedlist) {
		Collections.sort(pooledseedlist, Kmer.comparatorSourcePos());
		List<Seed> joinedseedlist = new ArrayList<Seed>();
		for (int i = pooledseedlist.size() - 1; i >= 0; i--) {
			int j = i;
			while ((pooledseedlist.get(j).pos - pooledseedlist.get(i).pos <= 2) && (pooledseedlist.get(i).source.equals(pooledseedlist.get(j).source))) {
				Seed previousseed = pooledseedlist.get(j);
				Seed recentseed = pooledseedlist.get(i);
				if (previousseed.pos - recentseed.pos == 1) {
					if (previousseed.kmerpointer.pos - recentseed.kmerpointer.pos == 1) {
						// new
						double ubound = recentseed.rangeUBound;
						double lbound = recentseed.rangeLBound;
						if (ubound > previousseed.rangeUBound)
							ubound = previousseed.rangeUBound;
						if (lbound < previousseed.rangeLBound)
							lbound = previousseed.rangeLBound;
						if (ubound >= lbound) {
							recentseed.sizelist.addAll(previousseed.sizelist.subList(recentseed.k() - 1, previousseed.k()));
							recentseed.kmerpointer.sizelist.addAll(previousseed.kmerpointer.sizelist.subList(recentseed.kmerpointer.k() - 1, previousseed.kmerpointer.k()));
							recentseed.rangeLBound = lbound;
							recentseed.rangeUBound = ubound;
							pooledseedlist.set(j, null);

							break; // only one can be joined, must be only one
							// joined;
						}
					}
				}
				do {
					j++;
					if (j == pooledseedlist.size())
						break;
				} while (pooledseedlist.get(j) == null);
				if (j == pooledseedlist.size())
					break;

			}
		}
		for (Seed seed : pooledseedlist)
			if (seed != null)
				joinedseedlist.add(seed);
		return joinedseedlist;

	}
	*/
	// filter those similar kmers
	public List<Kmer> filter(List<Kmer> fragmentkmerlist, double ear, int measure, int maxSeedNumber, int maxSignalConsidered) {
		List<Kmer> filteredKmerList = new ArrayList<Kmer>();
		if (maxSignalConsidered == -1)
			maxSignalConsidered = Integer.MAX_VALUE;
		for (Kmer kmer : fragmentkmerlist) {
			List<Seed> seedList = this.getSeed(kmer, ear, measure);
			int r = 0;
			for (Seed seed : seedList)
				if (kmer.source.equalsIgnoreCase(seed.source))
					if (kmer.pos > seed.pos) {
						if (kmer.pos - seed.pos - seed.k() <= maxSignalConsidered)
							r++;
					} else if (seed.pos - kmer.pos - kmer.k() <= maxSignalConsidered)
						r++;

			r++;
			if (r <= maxSeedNumber)
				filteredKmerList.add(kmer);
		}
		return filteredKmerList;
	}

	public SeedDatabase copy() {
		SeedDatabase seedDatabase = new SeedDatabase(databaseSeedList);
		seedDatabase.setMode(seedingmode);
		seedDatabase.setParameters(k, maxnosignalregion);
		seedDatabase.fastDatabaseSeedMap = this.fastDatabaseSeedMap;
		seedDatabase.table = this.table;
		return seedDatabase;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Seeding Options", level);
		parser.accepts("seedingmode", "Seeding mode: 1: Optimized for long k-mer (usually for k larger than 10); 2: Optimized for short k-mer (usually for k smaller than or equal to 10); -1: Auto-selection. ").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
		parser.accepts("k", "Kmer length.").withRequiredArg().ofType(Integer.class).defaultsTo(3);
		parser.accepts("maxnosignal", "Maximum no signal region between signals for seeding.").withRequiredArg().ofType(Integer.class).defaultsTo(10000000);

	}
}
class SeedTableKey {
	public final int[] key;

	public SeedTableKey(int[] key) {
		this.key = key;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(key);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof SeedTableKey))
			return false;
		return Arrays.equals(this.key, ((SeedTableKey) obj).key);
	}
}

class FastConversionTable {
	private List<Integer> sizelist; // final size must be Integer.max
	private List<Integer> rclist;

	public FastConversionTable(List<Integer> sizelist, List<Integer> rclist) {
		this.sizelist = sizelist;
		this.rclist = rclist;
		if (rclist.size() - sizelist.size() != 0)
			System.err.println("Table corrupted. List Size Inappropriate. Proceed anyway.");
	}

	public SeedTableKey getKey(Kmer kmer) {
		int[] kmerKey = new int[kmer.k()];
		for (int i = 0; i < kmer.k(); i++) {
			long possize = kmer.get(i);
			int pt = 0;
			while (possize > sizelist.get(pt))
				pt++;
			kmerKey[i] = rclist.get(pt);
		}
		return new SeedTableKey(kmerKey);
	}

	private int calcKeyCombinations(int[] smallKey, int[] largeKey) {
		long product = 1;
		for (int i = 0; i < smallKey.length; i++) {
			assert largeKey[i] >= smallKey[i];
			product *= (largeKey[i] - smallKey[i] + 1);
			if (product > Integer.MAX_VALUE)
				return Integer.MAX_VALUE;
		}
		return (int) product;
	}
	private void assignCombinedKeys(int[] smallKey, int[] largeKey, int[] current, int pos, List<SeedTableKey> combinedKeys) {
		for (int i = smallKey[pos]; i <= largeKey[pos]; i++) {
			current[pos] = i;
			if (pos == smallKey.length - 1) {
				int[] newKey = new int[current.length]; 
				System.arraycopy(current, 0, newKey, 0, current.length);
				combinedKeys.add(new SeedTableKey(newKey));
			}
			else
				assignCombinedKeys(smallKey, largeKey, current, pos + 1, combinedKeys);
		}
	}
	/**
	 * Check if the targetKey is within the range of smallKey and largeKey
	 * @param targetKey
	 * @param smallKey
	 * @param largeKey
	 * @return <code>true</code> if the targetKey is within the range
	 */
	private boolean withinRange(int[] targetKey, int[] smallKey, int[] largeKey) {
		assert targetKey.length == smallKey.length;
		assert targetKey.length == largeKey.length;
		for (int i = 0; i < targetKey.length; i++) {
			if (targetKey[i] < smallKey[i])
				return false;
			if (targetKey[i] > largeKey[i])
				return false;
		}
		return true;
	}
	
	public List<SeedTableKey> getKeys(Kmer smallKmer, Kmer largeKmer, int maxCombinations) throws TooManyKeyCombinationsException {
		assert smallKmer.k() > 0;
		assert smallKmer.k() == largeKmer.k();
		int[] smallKey = getKey(smallKmer).key;
		int[] largeKey = getKey(largeKmer).key;
		assert validateSmallAndLargeKeys(smallKey, largeKey);
		int combinations = calcKeyCombinations(smallKey, largeKey);
		if (combinations > maxCombinations)
			throw new TooManyKeyCombinationsException();
		List<SeedTableKey> combinedKeys = new ArrayList<>(combinations);
		assignCombinedKeys(smallKey, largeKey, new int[smallKmer.k()], 0, combinedKeys);
		assert validateCombinedKeys(combinedKeys, smallKey, largeKey) : "Incorrect key combinations.";
		return combinedKeys;
	}
	public List<SeedTableKey> getMatchingKeys(Kmer smallKmer, Kmer largeKmer, int maxCombinations, Set<SeedTableKey> referenceSet) {
		List<SeedTableKey> matchingKeys = new ArrayList<>();
		try {
			List<SeedTableKey> combinedKeys = getKeys(smallKmer, largeKmer, maxCombinations);
			for (SeedTableKey key : combinedKeys)
				if (referenceSet.contains(key))
					matchingKeys.add(key);
		} catch (TooManyKeyCombinationsException e) {
			// Escape routine when key combinations exceed the max combinations
			// Use linear search for all matching keys 
			int[] smallKey = getKey(smallKmer).key;
			int[] largeKey = getKey(largeKmer).key;
			for (SeedTableKey targetKey : referenceSet)
				if (withinRange(targetKey.key, smallKey, largeKey))
					matchingKeys.add(targetKey);
		}
		assert referenceSet.containsAll(matchingKeys);
		return matchingKeys;
	}
	
	private boolean validateSmallAndLargeKeys(int[] smallKey, int[] largeKey) {
		if (smallKey.length != largeKey.length)
			return false;
		for (int i = 0; i < smallKey.length; i++)
			if (largeKey[i] < smallKey[i])
				return false;
		return true;
	}
	private boolean validateCombinedKeys(List<SeedTableKey> combinedKeys, int[] smallKey, int[] largeKey) {
		// Total no. of keys equal to the no. of combinations
		if (calcKeyCombinations(smallKey, largeKey) != combinedKeys.size())
			return false;
		// Each key must be unique
		HashSet<SeedTableKey> set = new HashSet<>();
		for (SeedTableKey key : combinedKeys)
			if (!set.add(key))
				return false;
		// Each key is within the range of smallKey and largeKey
		for (SeedTableKey key : combinedKeys)
			if (!withinRange(key.key, smallKey, largeKey))
					return false;
		return true;
	}
	
	public void testKmer(Kmer kmer, double ear, int measure) {
		Kmer smallkmer = kmer.newKmer(1 - ear, measure * -1);
		Kmer largekmer = kmer.newKmer(1 + ear, measure);
		int[] smallkey = getKey(smallkmer).key;
		int[] largekey = getKey(largekmer).key; 
		
		int combinations[] = new int[kmer.k()]; 
		for (int i = 0; i < smallkey.length; i++) {
			combinations[i] = largekey[i] - smallkey[i] + 1;
		}
		int product = 1;
		for (int i = 0; i < smallkey.length; i++) {
			product *= combinations[i];
		}
		if (product > 100000)
			System.out.println(product + "\t" + Arrays.toString(combinations) + "\t" + kmer.toString());
	}

	public static FastConversionTable standardTable() {
		int gap = 5000;
		List<Integer> clist = new ArrayList<>();
		List<Integer> sizelist = new ArrayList<>();
		int recent = 0;
		int next = 1;
		for (int i = 1; i < 255; i++) {
			recent += gap;
			sizelist.add(recent);
			clist.add(i);
			next++;
		}
		sizelist.add(Integer.MAX_VALUE);
		clist.add(next);
		return new FastConversionTable(sizelist, clist);
	}
	
	public static FastConversionTable advancedTable(double ear, int measure, int minSize, int maxSize) {
		List<Integer> clist = new ArrayList<>();
		List<Integer> sizelist = new ArrayList<>();
		int recent = 0;
		int next = 1;
		int gap = minSize;
		while (true) {
			if ((recent * ear + measure) * 2 > gap) {
				gap = (int) ((recent * ear + measure) * 2);
			}
			if ((long) gap + (long) recent > maxSize)
				break;
			recent += gap;
			sizelist.add(recent);
			clist.add(next);
			next++;
		}
		sizelist.add(Integer.MAX_VALUE);
		clist.add(next);
		return new FastConversionTable(sizelist, clist);
	}
}
class TooManyKeyCombinationsException extends Exception {

	public TooManyKeyCombinationsException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public TooManyKeyCombinationsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

	public TooManyKeyCombinationsException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public TooManyKeyCombinationsException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public TooManyKeyCombinationsException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}
	
}
/*







public class SeedDatabase implements SelectableMode {

	private int seedingmode;
	private List<List<Kmer>> databaseSeedList;
	private FastConversionTable table = FastConversionTable.standardTable();
//	private FastConversionTable table = FastConversionTable.advancedTable(0.1, 500);
	private LinkedHashMap<String, List<Kmer>> fastDatabaseSeedMap;

	private int k;
	private int maxnosignalregion;

	public SeedDatabase(List<List<Kmer>> databaseSeedList) {
		this.databaseSeedList = databaseSeedList;
	}

	public SeedDatabase(List<Kmer> kmerList, int kmerlen) {
		this.databaseSeedList = convertKmerList(kmerList, kmerlen);
		this.k = kmerlen;
	}

	@Override
	public void setMode(OptionSet options) {
		setMode((int) options.valueOf("seedingmode"));
	}

	@Override
	public void setMode(int mode) {
		this.seedingmode = mode;
	}

	@Override
	public int getMode() {
		return seedingmode;
	}

	public void setParameters(OptionSet options) {
		setParameters((int) options.valueOf("k"), (int) options.valueOf("maxnosignal"));
	}
	public void setParameters(int k) {
		this.k = k;
	}

	public void setParameters(int k, int maxnosignalregion) {
		this.k = k;
		this.maxnosignalregion = maxnosignalregion;
	}

	public void buildDatabase() {
		switch (seedingmode) {
			case -1:
				if (k > 10)
					seedingmode = 1;
				else
					seedingmode = 2;
				buildDatabase();
				break;
			case 1:
				buildSortListDatabase();
				break;
			case 2:
				buildBinningDatabase();
				break;
			default:
				System.err.println("Warning! Unknown mode " + Integer.toString(seedingmode));
				buildSortListDatabase();
				break;
		}
	}

	private void buildSortListDatabase() {
		for (int i = 0; i < k; i++)
			Collections.sort(databaseSeedList.get(i), Kmer.comparator(i));
		// Nothing to do
	}

	private void buildBinningDatabase() {
		processFastAccessDatabase(k);
	}

	private void processFastAccessDatabase(int k) {
		// generate words instead of
		fastDatabaseSeedMap = new LinkedHashMap<String, List<Kmer>>();
		List<Kmer> kmerlist = databaseSeedList.get(0);
		for (Kmer kmer : kmerlist) {
			String key = table.convertKmer(kmer);
			if (!fastDatabaseSeedMap.containsKey(key))
				fastDatabaseSeedMap.put(key, new ArrayList<Kmer>());
			fastDatabaseSeedMap.get(key).add(kmer);
		}

	}

	private List<List<Kmer>> convertKmerList(List<Kmer> kmerList, int kmerlen) {
		if (kmerList == null)
			return null;
		List<List<Kmer>> kmerlistlist = new ArrayList<List<Kmer>>(); // each sort by kmer pos 1, pos 2, pos 3...

		for (int i = 0; i < kmerlen; i++) {
			List<Kmer> dummyrefkmerlist = new ArrayList<Kmer>(kmerList);
			kmerlistlist.add(dummyrefkmerlist);
		}
		return kmerlistlist;
	}

	public List<Kmer> getKmerListFromBinning(Kmer kmer, double ear, int measure) {
		List<Kmer> kmerList = new ArrayList<Kmer>();
		Kmer smallKmer = kmer.newKmer(1 - ear, measure * -1);
		Kmer largeKmer = kmer.newKmer(1 + ear, measure);
		List<String> keylist;
		try {
			keylist = table.convertKmer(smallKmer, largeKmer, fastDatabaseSeedMap.size());
		}
		catch (TooManyKeyCombinationsException e) {
			keylist = new ArrayList<>(fastDatabaseSeedMap.keySet());
		}
		for (String key : keylist) {
			List<Kmer> kmerlist = fastDatabaseSeedMap.get(key);
			if (kmerlist != null)
				for (Kmer refkmer : kmerlist) {
					boolean wrong = false;
					for (int i = 0; i < refkmer.k(); i++)
						if (refkmer.get(i) < smallKmer.get(i) || refkmer.get(i) > largeKmer.get(i)) {
							wrong = true;
							break;
						}
					if (!wrong) {
						if (refkmer.limitRange(kmer, measure, ear))
							kmerList.add(refkmer);
					}
				}
		}
		return kmerList;

	}

	public void testKmer(Kmer kmer, double ear, int measure) {
		table.testKmer(kmer, ear, measure);
	}
	public List<Kmer> getKmerListFromSortList(Kmer kmer, double ear, int measure) {
		List<Kmer> matchedkmerlist = null;
		Kmer smallkmer = kmer.newKmer(1 - ear, measure * -1);
		Kmer largekmer = kmer.newKmer(1 + ear, measure);
		// long smalllen = smallkmer.length();
		// long largelen = largekmer.length();
		// HashSet<Kmer> kmerHash = new HashSet<Kmer>();
		for (int i = 0; i < kmer.k(); i++) {
			List<Kmer> refkmerlist = databaseSeedList.get(i);
			int startpos = Collections.binarySearch(refkmerlist, smallkmer, Kmer.comparator(i));
			if (startpos < 0)
				startpos = (startpos + 1) * -1;
			else {
				while (startpos >= 0) {
					if (smallkmer.compare(refkmerlist.get(startpos), i) > 0)
						break;
					startpos--;
				}
				startpos++;
			}
			int stoppos = Collections.binarySearch(refkmerlist, largekmer, Kmer.comparator(i));
			if (stoppos < 0)
				stoppos = (stoppos + 1) * -1;
			else {
				while (stoppos < refkmerlist.size()) {
					if (largekmer.compare(refkmerlist.get(stoppos), i) < 0)
						break;
					stoppos++;
				}
			}
			stoppos--;

			List<Kmer> sublist = refkmerlist.subList(startpos, stoppos + 1);
			if (matchedkmerlist == null) {
				matchedkmerlist = new ArrayList<Kmer>(sublist);
				// matchedkmerlist = new ArrayList<Kmer>();
				// for (Kmer km : sublist)
				// {
				// if (km.len <= largelen && km.len >= smalllen)
				// matchedkmerlist.add(km);
				// }
			} else {
				List<Kmer> newlist = new ArrayList<Kmer>();
				if (matchedkmerlist.size() > sublist.size()) {
					Set<Kmer> subset = new HashSet<Kmer>(sublist);
					for (Kmer candidatekmer : matchedkmerlist)
						if (subset.contains(candidatekmer))
							newlist.add(candidatekmer);
				} else {
					Set<Kmer> subset = new HashSet<Kmer>(matchedkmerlist);
					for (Kmer candidatekmer : sublist) {
						if (subset.contains(candidatekmer))
							newlist.add(candidatekmer);
					}
				}
				matchedkmerlist = newlist;
			}

		}
		List<Kmer> kmerList = new ArrayList<Kmer>();
		for (Kmer matchedkmer : matchedkmerlist) {
			if (matchedkmer.limitRange(kmer, measure, ear))
				kmerList.add(matchedkmer);
		}
		return kmerList;

	}

	public List<Kmer> getKmerList(Kmer kmer, double ear, int measure) {
		switch (seedingmode) {
			case -1:
				if (k > 10)
					seedingmode = 1;
				else
					seedingmode = 2;
				return getKmerList(kmer, ear, measure);
			case 1:
				return getKmerListFromSortList(kmer, ear, measure);
			case 2:
				return getKmerListFromBinning(kmer, ear, measure);
			default:
				System.err.println("Warning! Unknown mode " + Integer.toString(seedingmode));
				return getKmerListFromSortList(kmer, ear, measure);
		}

	}

	public List<Seed> getSeed(Kmer kmer, double ear, int measure) {

		List<Kmer> matchedkmerlist = getKmerList(kmer, ear, measure);
		List<Seed> seedlist = new ArrayList<Seed>();
		for (Kmer matchedkmer : matchedkmerlist) {

			Seed s = new Seed(matchedkmer, new Kmer(kmer));
			if (s.limitRange(measure, ear))
				seedlist.add(s);
		}
		return seedlist;

	}

	public List<Seed> getJoinedSeed(Kmer kmer, double ear, int measure) {
		List<Seed> seedlist = getSeed(kmer, ear, measure); // This never join the seed. Wait for later analysis
		return seedJoin(seedlist);
	}

	public List<Seed> seedJoin2(List<Seed> pooledSeedList, int k) {
		LinkedHashMap<ReferenceSignal, LinkedHashMap<ReferenceSignal, Seed>> kmerPosMap = new LinkedHashMap<>();
		for (Seed seed : pooledSeedList) {
			ReferenceSignal refSig = new ReferenceSignal(seed.source, seed.pos);
			ReferenceSignal kmerRefSig = new ReferenceSignal(seed.kmerpointer.source, seed.kmerpointer.pos);
			LinkedHashMap<ReferenceSignal, Seed> posMap = kmerPosMap.get(kmerRefSig);
			if (posMap == null) {
				posMap = new LinkedHashMap<ReferenceSignal, Seed>();
				kmerPosMap.put(kmerRefSig, posMap);
			}
			posMap.put(refSig, seed);
		}
		List<ReferenceSignal> kmerRefSigList = new ArrayList<>(kmerPosMap.keySet()); 
		Collections.sort(kmerRefSigList);
		List<Seed> joinedSeedList = new ArrayList<>();
		for (ReferenceSignal kmerRefSig : kmerRefSigList) {
			
			for (ReferenceSignal refSig : new ArrayList<>(kmerPosMap.get(kmerRefSig).keySet())) {
				List<Seed> seedToJoin = new ArrayList<>();
				
				ReferenceSignal nextKmerRefSig = new ReferenceSignal(kmerRefSig.ref, kmerRefSig.refpPos);
				ReferenceSignal nextRefSig = new ReferenceSignal(refSig.ref, refSig.refpPos);
				while (kmerPosMap.containsKey(nextKmerRefSig) && kmerPosMap.get(nextKmerRefSig).containsKey(nextRefSig)) { 
					seedToJoin.add(kmerPosMap.get(nextKmerRefSig).remove(nextRefSig));
					nextKmerRefSig = new ReferenceSignal(nextKmerRefSig.ref, nextKmerRefSig.refpPos + 1);
					nextRefSig = new ReferenceSignal(nextRefSig.ref, nextRefSig.refpPos + 1);
				}
				List<Long> seedSize = new ArrayList<>();
				List<Long> kmerSize = new ArrayList<>();
				int pter = 0;
				for (Seed seed : seedToJoin) {
					for (int i = 0; i < k; i++)
						if (pter + i >= seedSize.size()) {
							seedSize.add(seed.sizelist.get(i));
							kmerSize.add(seed.kmerpointer.sizelist.get(i));
						}
						else {
							seedSize.set(pter + i, seed.sizelist.get(i));
							kmerSize.set(pter + i, seed.kmerpointer.sizelist.get(i));
						}
							
					pter++;
				}
				joinedSeedList.add(new Seed(new Kmer(seedToJoin.get(0).source, seedToJoin.get(0).pos, seedSize), new Kmer(seedToJoin.get(0).kmerpointer.source, seedToJoin.get(0).kmerpointer.pos, kmerSize)));
			}
		}
		return joinedSeedList;
	}
	public List<Seed> seedJoin(List<Seed> pooledseedlist) {
		Collections.sort(pooledseedlist, Kmer.comparatorSourcePos());
		List<Seed> joinedseedlist = new ArrayList<Seed>();
		for (int i = pooledseedlist.size() - 1; i >= 0; i--) {
			int j = i;
			while ((pooledseedlist.get(j).pos - pooledseedlist.get(i).pos <= 2) && (pooledseedlist.get(i).source.equalsIgnoreCase(pooledseedlist.get(j).source))) {
				Seed previousseed = pooledseedlist.get(j);
				Seed recentseed = pooledseedlist.get(i);
				if (previousseed.pos - recentseed.pos == 1) {
					if (previousseed.kmerpointer.pos - recentseed.kmerpointer.pos == 1) {
						// new
						double ubound = recentseed.rangeUBound;
						double lbound = recentseed.rangeLBound;
						if (ubound > previousseed.rangeUBound)
							ubound = previousseed.rangeUBound;
						if (lbound < previousseed.rangeLBound)
							lbound = previousseed.rangeLBound;
						if (ubound >= lbound) {
							recentseed.sizelist.addAll(previousseed.sizelist.subList(recentseed.k() - 1, previousseed.k()));
							recentseed.kmerpointer.sizelist.addAll(previousseed.kmerpointer.sizelist.subList(recentseed.kmerpointer.k() - 1, previousseed.kmerpointer.k()));
							recentseed.rangeLBound = lbound;
							recentseed.rangeUBound = ubound;
							pooledseedlist.set(j, null);

							break; // only one can be joined, must be only one
							// joined;
						}
					}
				}
				do {
					j++;
					if (j == pooledseedlist.size())
						break;
				} while (pooledseedlist.get(j) == null);
				if (j == pooledseedlist.size())
					break;

			}
		}
		for (Seed seed : pooledseedlist)
			if (seed != null)
				joinedseedlist.add(seed);
		return joinedseedlist;

	}

	// filter those similar kmers
	public List<Kmer> filter(List<Kmer> fragmentkmerlist, double ear, int measure, int maxSeedNumber, int maxSignalConsidered) {
		List<Kmer> filteredKmerList = new ArrayList<Kmer>();
		if (maxSignalConsidered == -1)
			maxSignalConsidered = Integer.MAX_VALUE;
		for (Kmer kmer : fragmentkmerlist) {
			List<Seed> seedList = this.getSeed(kmer, ear, measure);
			int r = 0;
			for (Seed seed : seedList)
				if (kmer.source.equalsIgnoreCase(seed.source))
					if (kmer.pos > seed.pos) {
						if (kmer.pos - seed.pos - seed.k() <= maxSignalConsidered)
							r++;
					} else if (seed.pos - kmer.pos - kmer.k() <= maxSignalConsidered)
						r++;

			r++;
			if (r <= maxSeedNumber)
				filteredKmerList.add(kmer);
		}
		return filteredKmerList;
	}

	public SeedDatabase copy() {
		SeedDatabase seedDatabase = new SeedDatabase(databaseSeedList);
		seedDatabase.setMode(seedingmode);
		seedDatabase.setParameters(k, maxnosignalregion);
		seedDatabase.fastDatabaseSeedMap = this.fastDatabaseSeedMap;
		seedDatabase.table = this.table;
		return seedDatabase;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Seeding Options", level);
		parser.accepts("seedingmode", "Seeding mode: 1: Opt for long k-mer; 2: Opt for short k-mer; -1: Auto-selection").withRequiredArg().ofType(Integer.class).defaultsTo(-1);
		parser.accepts("k", "Kmer length.").withOptionalArg().ofType(Integer.class).defaultsTo(3);
		parser.accepts("maxnosignal", "Maximum no signal region between signals for seeding.").withOptionalArg().ofType(Integer.class).defaultsTo(10000000);

	}
}

class FastConversionTable {
	private List<Integer> sizelist; // final size must be Integer.max
	private List<Character> rclist;

	public FastConversionTable(List<Integer> sizelist, List<Character> rclist) {
		this.sizelist = sizelist;
		this.rclist = rclist;
		if (rclist.size() - sizelist.size() != 0)
			System.err.println("Table corrupted. List Size Inappropriate. Proceed anyway.");
	}

	public String convertKmer(Kmer kmer) {
		StringBuilder s = new StringBuilder();
		for (long possize : kmer.sizelist) {
			int pt = 0;
			while (possize > sizelist.get(pt))
				pt++;
			s.append(rclist.get(pt));
		}
		return s.toString();
	}

	private List<String> join(List<String> slist, List<Character> clist) {
		// if (slist.size() != clist.size())
		// return null;

		List<String> newslist = new ArrayList<String>();
		for (String s : slist)
			for (char c : clist)
				newslist.add(s + c);
		return newslist;
	}

	public List<String> convertKmer(Kmer smallKmer, Kmer largeKmer, int maxCombinations) throws TooManyKeyCombinationsException {
//		Kmer smallKmer = kmer.newKmer(1 - ear, measure * -1);
//		Kmer largeKmer = kmer.newKmer(1 + ear, measure);
		String smallkey = convertKmer(smallKmer);
		String largekey = convertKmer(largeKmer);
		List<String> recentList = new ArrayList<String>();
		int combinations[] = new int[smallKmer.k()]; 
		for (int i = 0; i < smallkey.length(); i++) {
			combinations[i] = largekey.charAt(i) - smallkey.charAt(i) + 1;
		}
		int product = 1;
		for (int i = 0; i < smallkey.length(); i++) {
			product *= combinations[i];
		}
		if (product > maxCombinations)
			throw new TooManyKeyCombinationsException();
		recentList.add("");
		for (int i = 0; i < smallkey.length(); i++) {
			List<Character> clist = new ArrayList<Character>();
			for (char c = smallkey.charAt(i); c <= largekey.charAt(i); c++)
				clist.add(c);
			recentList = join(recentList, clist);
		}
		return recentList;
	}
	public void testKmer(Kmer kmer, double ear, int measure) {
		Kmer smallkmer = kmer.newKmer(1 - ear, measure * -1);
		Kmer largekmer = kmer.newKmer(1 + ear, measure);
		String smallkey = convertKmer(smallkmer);
		String largekey = convertKmer(largekmer); 
		
		int combinations[] = new int[kmer.k()]; 
		for (int i = 0; i < smallkey.length(); i++) {
			combinations[i] = largekey.charAt(i) - smallkey.charAt(i) + 1;
		}
		int product = 1;
		for (int i = 0; i < smallkey.length(); i++) {
			product *= combinations[i];
		}
		if (product > 100000)
			System.out.println(product + "\t" + Arrays.toString(combinations) + "\t" + kmer.toString());
	}

	public static FastConversionTable standardTable() {
		int gap = 5000;
		List<Character> clist = new ArrayList<Character>();
		List<Integer> sizelist = new ArrayList<Integer>();
		int recent = 0;
		char next = 1;
		for (int i = 1; i < 255; i++) {
			recent += gap;
			sizelist.add(recent);
			clist.add(next);
			next++;
		}
		sizelist.add(Integer.MAX_VALUE);
		clist.add(next);
		return new FastConversionTable(sizelist, clist);
	}
	
	public static FastConversionTable advancedTable(double ear, int measure) {
		int gap = 5000;
		List<Character> clist = new ArrayList<Character>();
		List<Integer> sizelist = new ArrayList<Integer>();
		int recent = 0;
		char next = 1;
		for (int i = 1; i < 255; i++) {
			if ((recent * ear + measure) * 2 > gap) {
				gap = (int) ((recent * ear + measure) * 2);
			}
			if ((long) gap + (long) recent > Integer.MAX_VALUE)
				break;
			recent += gap;
			sizelist.add(recent);
			clist.add(next);
			next++;
		}
		sizelist.add(Integer.MAX_VALUE);
		clist.add(next);
		return new FastConversionTable(sizelist, clist);
	}
}


*/