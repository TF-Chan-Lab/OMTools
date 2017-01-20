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


package aldenjava.opticalmapping.data.data;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;
import joptsimple.OptionSet;

/**
 * Data Generator
 * @author Alden
 *
 */
public class OptMapDataGenerator {

	private final Map<String, ReferenceClusterNode> optclusmap;
	private long totalreferencesize;
	private int totalGeneratedDataEntries = 0;
	private long totalGeneratedDataSize = 0;		

	private int degeneracy;
	private int measurementError;

	private int averageSize;
	private int fubound;
	private int flbound;

	private double scaleMedian;
	private double scaleSD;
	private double subound;
	private double slbound;
	
	private double fprate;
	private double fnrate;

	private RealDistribution sizeDistribution;
	private RealDistribution scaleDistribution;
	private Random random;

	private int indelSize;
	private int inversionMode = 0;

	private double coverage;
	private int targetMoleNo;
	
	/**
	 * Creates the OptMapDataGenerator with the default references
	 * @param optclusmap the reference cluster map
	 */
	public OptMapDataGenerator(Map<String, ReferenceClusterNode> optclusmap) {
		this.optclusmap = optclusmap;
		updateReferenceSize();
	}
	
	public void setParameters(OptionSet options) {
		this.setParameters((int) options.valueOf("rsln"), 
				(int) options.valueOf("meas"), 
				(int) options.valueOf("fsize"),
				(int) options.valueOf("fubound"),
				(int) options.valueOf("flbound"),
				(double) options.valueOf("median"),
				(double) options.valueOf("scalesd"),
				(double) options.valueOf("subound"),
				(double) options.valueOf("slbound"),
				(double) options.valueOf("fpr"), 
				(double) options.valueOf("fnr"),
				(int) options.valueOf("indelsize"),
				(int) options.valueOf("inversionmode"),
				(int) options.valueOf("moleno"),
				(double) options.valueOf("cov"));
		if (options.has("seed"))
			this.setSeed((long) options.valueOf("seed"));
	}

	public void setParameters(int deg, int meas, int averageSize, int fubound, int flbound, double scaleMedian, double scaleSD, double subound, double slbound, double fprate, double fnrate, int indelSize, int inversionMode, int targetMoleNo, double coverage) {
		this.degeneracy = deg;
		this.measurementError = meas;
		
		if (averageSize <= 0)
			throw new IllegalArgumentException();
		if (fubound < flbound)
			throw new IllegalArgumentException();
		if (subound < slbound)
			throw new IllegalArgumentException();
		this.averageSize = averageSize;
		this.fubound = fubound;
		this.flbound = flbound;
		this.sizeDistribution = new ExponentialDistribution(averageSize / 2.0);
		
		this.scaleMedian = scaleMedian;
		this.scaleSD = scaleSD;
		if (scaleSD <= 0)
			this.scaleDistribution = null;
		else
			this.scaleDistribution = new CauchyDistribution(scaleMedian, scaleSD);
		this.subound = subound;
		this.slbound = slbound;
		
		this.fprate = fprate;
		this.fnrate = fnrate;

		this.indelSize = indelSize;
		this.inversionMode = inversionMode;
		
		random = new Random();

		this.targetMoleNo = targetMoleNo;
		this.coverage = coverage;
		if (targetMoleNo > 0)
			VerbosePrinter.println("Override \"cov\" option with \"moleno\" option. ");
	}
	public void setSeed(long seed) {
		this.sizeDistribution.reseedRandomGenerator(seed);
		if (scaleDistribution != null)
			this.scaleDistribution.reseedRandomGenerator(seed);
		random.setSeed(seed);
	}
	
	/* Core */
	private void updateReferenceSize() {
		this.totalreferencesize = 0;
		for (ReferenceClusterNode optclus : optclusmap.values())
			this.totalreferencesize += DataNode.getTotalSize(optclus.optrefmap);
	}

	private ReferenceClusterNode getRandomReferenceSource() {
		double total = 0;
		double target = random.nextDouble();
		for (ReferenceClusterNode optclus : optclusmap.values())
		{
			total += optclus.ratio;
			if (target < total)
				return optclus;
		}
		return null;
	}
	private String getRandomReferenceName(LinkedHashMap<String, DataNode> optrefmap) {
		long size = DataNode.getTotalSize(optrefmap);
		long position = (long) (random.nextDouble() * size);
		for (DataNode ref : optrefmap.values()) {
			position -= ref.size;
			if (position <= 0)
				return ref.name;
		}
		return null;
	}
	
	private GenomicPosNode getSimulationRegion(DataNode ref) {
		long size;
		do {
			size = (long) (sizeDistribution.sample() + averageSize / 2.0);
		} while (size < flbound || size > fubound);
		long finalSize;
		long pos;
		do {
			finalSize = size - indelSize;
			pos = (long) (random.nextDouble() * ref.size) + 1;
			// try to have balanced number of fragments at left genome, unless size must be constant
			if (random.nextDouble() >= 0.5) {
				pos = ref.size - pos + 1 - finalSize + 1;
				if (pos <= 0) {
					finalSize = finalSize - (1 - pos) ;
					pos = 1;
				}
			}
			if (pos + finalSize - 1 > ref.size)
				finalSize = ref.size - pos + 1;
		} while (finalSize < flbound - indelSize);
		return new GenomicPosNode(ref.name, pos, pos + finalSize - 1);
	}
	
	private SimuDataNode getSimuData(DataNode ref, GenomicPosNode region) {
		
		List<ReferenceSignal> rsList = ref.getReferenceSignals(region);
		List<Long> refp = new ArrayList<>();
		List<VirtualSignal> vsList = new ArrayList<>();
		for (ReferenceSignal rs : rsList) {
			vsList.add(new VirtualSignal(rs));
			refp.add(ref.refp[rs.refpPos] - region.start + 1);
		}
		return new SimuDataNode(refp, vsList);
	}
	/**
	 * Induce Insertions / Deletions in the middle of data
	 * @param size
	 * @param svSize
	 * @return
	 */
	private void induceIndel(LinkedHashMap<String, DataNode> optrefmap, long size, SimuDataNode simuData) {
		if (indelSize != 0) {
			// Insertion
			if (indelSize > 0) {
				SimuDataNode ins = SimuDataNode.createEmptyNode();
				induceRandomSignals(ins, indelSize, DataNode.getDensity(optrefmap));
				long start = (size / 2 + 1);
				boolean added = false;
				List<Long> newrefp = new ArrayList<>();
				List<VirtualSignal> newvsList = new ArrayList<>();
				for (int i = 0; i < simuData.getTotalSignal(); i++) {
					if (!added && simuData.refp.get(i) >= start) {
						newrefp.addAll(ins.refp);
						newvsList.addAll(ins.vsList);
						added = true;
					}
					newrefp.add(simuData.refp.get(i) + (added ? indelSize : 0));
					newvsList.add(simuData.vsList.get(i));
				}
				simuData.refp = newrefp;
				simuData.vsList = newvsList;
			}
			// Deletion
			if (indelSize < 0) {
				if (size < indelSize) 
					throw new IllegalArgumentException("svSize > data length");
				List<Long> newrefp = new ArrayList<>();
				List<VirtualSignal> newvsList = new ArrayList<>();
				long start = size / 2 - (-1 * indelSize) / 2 + 1;
				long stop = start + (-1 * indelSize) - 1;
				for (int i = 0; i < simuData.getTotalSignal(); i++) {
					if (simuData.refp.get(i) < start) {
						
						newvsList.add(simuData.vsList.get(i));
					}
					else
						if (simuData.refp.get(i) > stop) {
							newrefp.add(simuData.refp.get(i) + indelSize); // indelSize is negative here
							newvsList.add(simuData.vsList.get(i));
						}
				}
				
				simuData.refp = newrefp;
				simuData.vsList = newvsList;
			}
		}
	}
	/**
	 * Induces inversion at second half of a molecule
	 * @param size
	 * @param fragmentpos
	 * @return
	 */
	private void induceInversion(long size, SimuDataNode simuData) {
		switch (inversionMode) {
			case 0:
				break;	
			case 1:
				List<Long> newrefp = new ArrayList<>();
				List<VirtualSignal> newvsList = new ArrayList<>();
				List<Long> revrefp = new ArrayList<>();
				List<VirtualSignal> revvsList = new ArrayList<>();
				long start = size / 2 + 1;
				for (int i = 0; i < simuData.getTotalSignal(); i++) {
					if (simuData.refp.get(i) < start) {
						newrefp.add(simuData.refp.get(i));
						newvsList.add(simuData.vsList.get(i));
					}
					else {
						revrefp.add(simuData.refp.get(i));
						revvsList.add(simuData.vsList.get(i));
					}
				}
				
				
				Collections.reverse(revrefp);
				for (int i = 0; i < revrefp.size(); i++)
//					revrefp.set(i, start + (size - start + 1) - (simuData.refp.get(i) - start + 1));
					revrefp.set(i, start + size - simuData.refp.get(i));
				Collections.reverse(revvsList);

				newrefp.addAll(revrefp);
				newvsList.addAll(revvsList);
				simuData.refp = newrefp;
				simuData.vsList = newvsList;
				break;
			default:
				break;
		}
	}
	/**
	 * Induces false negative signal according to <code>fnrate</code>. Probability of missing is independent in all signals. 
	 * @param fragmentpos
	 * @return fragmentpos after removing false negative signals
	 */
	private void induceFN(SimuDataNode simuData) {
		List<Long> newrefp = new ArrayList<>();
		List<VirtualSignal> newvsList = new ArrayList<>();
		for (int i = 0; i < simuData.getTotalSignal(); i++)
			if (random.nextDouble() >= fnrate) {
				newrefp.add(simuData.refp.get(i));
				newvsList.add(simuData.vsList.get(i));
			}
		simuData.refp = newrefp;
		simuData.vsList = newvsList;
	}
	/**
	 * Induces random signals according to <code>rate</code>. Number of random signals is following the Poisson Distribution.
	 * @param fragmentpos
	 * @param size
	 * @param rate
	 * @return
	 */
	private void induceRandomSignals(SimuDataNode simuData, long size, double rate) {
		IntegerDistribution fpDistribution = new PoissonDistribution(size * rate);
		fpDistribution.reseedRandomGenerator(random.nextLong());
		int fpno = (fpDistribution).sample();
		HashSet<Long> stored = new HashSet<Long>();
		for (Long p : simuData.refp)
			stored.add(p);
		
		for (int i = 0; i < fpno; i++) {
			if (i + simuData.getTotalSignal() >= size)
				break; // Don't do it when fpno + current signal > no. of signal on the reference
			long pos;
			do {
				pos = ((long) (random.nextDouble() * size)) + 1;
			} while (stored.contains(pos));
			stored.add(pos);
			simuData.refp.add(pos);
			simuData.vsList.add(new VirtualSignal(null, null));
		}
		simuData.sort();
	}
	private void induceFP(SimuDataNode simuData, long size) {
		if (fprate > 0)
			induceRandomSignals(simuData, size, fprate);
	}
	private double getScalingFactor() {
		if (scaleSD <= 0)
			return scaleMedian;
		double scale = -1;
		do {
			scale = scaleDistribution.sample();
		} while (scale > subound || scale < slbound);
		return scale;
	}
	private void induceSpecificSE(SimuDataNode simuData, double scalingfactor) {
		for (int i = 0; i < simuData.getTotalSignal(); i++)
			simuData.refp.set(i, (long) (simuData.refp.get(i) * scalingfactor));
	}
	private void induceResolutionError(SimuDataNode simuData) {
		if (simuData.getTotalSignal() == 0)
			return;
		int totalGroups = 0;
		int[] groupID = new int[simuData.getTotalSignal()];
		for (int forward = 0; forward < simuData.getTotalSignal(); forward++) {
			if (groupID[forward] == 0)
				groupID[forward] = ++totalGroups;
			for (int reverse = simuData.getTotalSignal() - 1; reverse > forward; reverse--) {
				double minMergeSignalValue = 1 / (1 + Math.exp(-0.01 * ((simuData.refp.get(reverse) - simuData.refp.get(forward)) - degeneracy)));
				if (random.nextDouble() > minMergeSignalValue || simuData.refp.get(reverse) == simuData.refp.get(forward)) { // If two signals are overlapping, they should be from the same spot
					for (int k = reverse; k > forward; k--)
						groupID[k] = groupID[forward];
				}
			}
		}

		List<Long> newrefp = new ArrayList<Long>();
		List<VirtualSignal> newvsList = new ArrayList<>();
		
		int prevGroupID = groupID[0]; // first group is always group 1
		long sum = 0;
		int count = 0;
		List<VirtualSignal> tmpvsList = new ArrayList<>();
		for (int i = 0; i < simuData.getTotalSignal(); i++) {
			if (prevGroupID != groupID[i]) {
				newrefp.add((long) (sum / (double) count + 0.5));
				if (count == 1)
					newvsList.add(tmpvsList.get(0)); // No merging, just use the original
				else
					newvsList.add(new VirtualSignal(tmpvsList));
				sum = 0;
				count = 0;
				tmpvsList = new ArrayList<>();
			}
			count++;
			sum += simuData.refp.get(i);
			tmpvsList.add(simuData.vsList.get(i));
			prevGroupID = groupID[i];
		}
		if (count > 0) {
			newrefp.add((long) (sum / (double) count + 0.5));
			newvsList.add(new VirtualSignal(tmpvsList));
		}
		
		simuData.refp = newrefp;
		simuData.vsList = newvsList;
		
		assert(simuData.getTotalSignal() == totalGroups);
	}
	
	private void induceMeasurementError(SimuDataNode simuData, long size) {
		if (measurementError <= 0)
			return;
		for (int i = 0; i < simuData.getTotalSignal(); i++) {
			long newpos = simuData.refp.get(i) + random.nextInt(measurementError * 2) - measurementError; // can have repeat
			if (newpos <= 0)
				newpos = 1;
			if (newpos > size)
				newpos = size;
			simuData.refp.set(i, newpos);
		}
		simuData.sort();
		simuData.merge();
	}
	
	private int getStrand() {
		if (random.nextDouble() >= 0.5)
			return 1;
		else
			return -1;
	}
	private void induceReverse(SimuDataNode simuData, long size, int strand) {
		if (strand == -1) {
			Collections.reverse(simuData.refp);
			for (int i = 0; i < simuData.getTotalSignal(); i++)
				simuData.refp.set(i, size - simuData.refp.get(i) + 1);
			Collections.reverse(simuData.vsList);
		}
	}
	
	private boolean finishGeneration() {
		if (targetMoleNo >= 0)
			return totalGeneratedDataEntries >= targetMoleNo;
		else
			return totalGeneratedDataSize >= (long) totalreferencesize * coverage;
	}
	
	private DataNode generateNextSingleColorFragment() {
		ReferenceClusterNode optclus = getRandomReferenceSource();
		LinkedHashMap<String, DataNode> optrefmap = optclus.optrefmap;
		String refname = getRandomReferenceName(optrefmap);
		GenomicPosNode region = getSimulationRegion(optrefmap.get(refname));
		SimuDataNode simuData = getSimuData(optrefmap.get(refname), region);
		// Not yet support vsList for indel and inversion
		induceIndel(optrefmap, region.length(), simuData);
		induceInversion(region.length(), simuData);
		induceFN(simuData);
		induceFP(simuData, region.length() + indelSize);
		double factor = getScalingFactor();
		induceSpecificSE(simuData, factor);
		long size = (long) ((region.length() + indelSize) * factor);
		induceResolutionError(simuData);
		induceMeasurementError(simuData, size);
		int strand = getStrand();
		induceReverse(simuData, size, strand);
		DataNode data = new DataNode(Integer.toString(totalGeneratedDataEntries + 1), size, ArrayUtils.toPrimitive(simuData.refp.toArray(new Long[simuData.getTotalSignal()])));
		data.importSimulationInfo(new SimulationInfo(region, strand, simuData.vsList));
		// OLD: There should be only two cases to be fixed: 1. negative signals; 2. overlapped signals
		// Now there should be no data to fix
		data.fix();
		totalGeneratedDataSize += data.length();
		totalGeneratedDataEntries++;
		return data;
	}
	/* generate */
	public DataNode generateNextFragment() {
		if (!finishGeneration())
			return generateNextSingleColorFragment();
		else
			return null;
	}
	public List<DataNode> generateSlidingWindowFragment(int leapsize, int windowsize, int startid)
	{
		totalGeneratedDataEntries = 0;
		List<DataNode> dataList = new ArrayList<DataNode>();
		for (ReferenceClusterNode optclus : optclusmap.values()) {
			LinkedHashMap<String, DataNode> optrefmap = optclus.optrefmap;
			for (DataNode ref : optrefmap.values())
				for (int startpos = 0; startpos + windowsize < ref.size; startpos += leapsize) {
					String name = Integer.toString(totalGeneratedDataEntries + startid);
					totalGeneratedDataEntries++;
					
					GenomicPosNode region = new GenomicPosNode(ref.name, startpos + 1, startpos + windowsize);
					SimuDataNode simuData = getSimuData(ref, region);
		
					
				 	DataNode data = new DataNode(name, region.length(), ArrayUtils.toPrimitive(simuData.refp.toArray(new Long[simuData.getTotalSignal()])));
					data.importSimulationInfo(new SimulationInfo(region, 1, simuData.vsList));
					
				 	dataList.add(data);
					
				}
		}
		return dataList;
	}
	public int getTotalFragment() {
		return totalGeneratedDataEntries;
	}
	public void reset()	{
		totalGeneratedDataEntries = 0;
		totalGeneratedDataSize = 0;		
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Data Generator Options", level);
		parser.accepts("rsln", "Resolution error").withRequiredArg().ofType(Integer.class).defaultsTo(1200);
		parser.accepts("meas", "Measurement error").withRequiredArg().ofType(Integer.class).defaultsTo(500);
		
		parser.accepts("fsize", "Average fragment size").withRequiredArg().ofType(Integer.class).defaultsTo(200000);
		parser.accepts("fubound", "Size upper boundary, inclusive").withRequiredArg().ofType(Integer.class).defaultsTo(1000000);
		parser.accepts("flbound", "Size lower boundary, inclusive").withRequiredArg().ofType(Integer.class).defaultsTo(100000);
		parser.accepts("median", "Median for scale").withRequiredArg().ofType(Double.class).defaultsTo(1.00);
		parser.accepts("scalesd", "SD for scale").withRequiredArg().ofType(Double.class).defaultsTo(0.04);
		parser.accepts("subound", "Scale upper boundary, inclusive").withRequiredArg().ofType(Double.class).defaultsTo(1.3);
		parser.accepts("slbound", "Scale lower boundary, inclusive").withRequiredArg().ofType(Double.class).defaultsTo(0.7);
		parser.accepts("fpr", "false positive rate").withRequiredArg().ofType(Double.class).defaultsTo(0.00001);
		parser.accepts("fnr", "false negative rate").withRequiredArg().ofType(Double.class).defaultsTo(0.1);
		parser.accepts("seed", "Random seed").withRequiredArg().ofType(Long.class);
		
		parser.accepts("indelsize", "Random Insertion/Deletion size").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("inversionmode", "Inversion mode. 0: no inversion. 1: inversion of second half").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		
		parser.accepts("cov", "Coverage of data output").withRequiredArg().ofType(Double.class).defaultsTo(10.0);
		parser.accepts("moleno", "Number of molecules to be generated. Overriding coverage option if set to a positive number").withRequiredArg().ofType(Integer.class).defaultsTo(-1);

	}
	public static void main(String[] args) throws IOException {
		// ratiolist
		ExtendOptionParser parser = new ExtendOptionParser(OptMapDataGenerator.class.getSimpleName(), "Generates simulated data from the reference.");
		MultipleReferenceReader.assignOptions(parser, 1);
		OptMapDataGenerator.assignOptions(parser, 1);
		OptMapDataWriter.assignOptions(parser, 1);
		
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		
		OptionSet options = parser.parse(args);
		MultipleReferenceReader multireader = new MultipleReferenceReader(options);
		LinkedHashMap<String, ReferenceClusterNode> optclusmap = multireader.readAllData();
		
		OptMapDataGenerator omdg = new OptMapDataGenerator(optclusmap);
		omdg.setParameters(options);
		OptMapDataWriter omdw = new OptMapDataWriter(options);
		DataNode fragment;
		while ((fragment = omdg.generateNextFragment()) != null)
			omdw.write(fragment);
		omdw.close();
		VerbosePrinter.println("Total molecules generated: " + omdg.getTotalFragment());
	}
}

/**
 * A class for storing temporary data during generation 
 */
class SimuDataNode {
	List<Long> refp;
	List<VirtualSignal> vsList;
	public SimuDataNode(List<Long> refp, List<VirtualSignal> vsList) {
		this.refp = refp;
		this.vsList = vsList;
	}
	public static SimuDataNode createEmptyNode() {
		return new SimuDataNode(new ArrayList<Long>(), new ArrayList<VirtualSignal>());
	}
	public int getTotalSignal() {
		return refp.size();
	}
	public void sort() {
		for (int i = 0; i < getTotalSignal(); i++)
			for (int j = 0; j < getTotalSignal() - i - 1; j++) {
				if (refp.get(j) > refp.get(j + 1)) {
					Long tmp = refp.get(j);
					refp.set(j, refp.get(j + 1));
					refp.set(j + 1, tmp);
					VirtualSignal tmpv = vsList.get(j);
					vsList.set(j, vsList.get(j + 1));
					vsList.set(j + 1, tmpv);
				}
			}
	}
	public void merge() {
		if (getTotalSignal() == 0)
			return;
		List<Long> newrefp = new ArrayList<>(); 
		List<VirtualSignal> newvsList = new ArrayList<>();
		long lastSig = -1;
		int start = -1;
		for (int i = 0; i < getTotalSignal() + 1; i++) {
			if (i == getTotalSignal() || lastSig != refp.get(i)) {
				if (start != -1) {
					newrefp.add(lastSig);
					if (start == i - 1) // only one element
						newvsList.add(vsList.get(start));
					else {
						List<VirtualSignal> tmpvsList = new ArrayList<>();
						for (int j = start; j <= i - 1; j++) {
							if (vsList.get(j).sources == null)
								tmpvsList.add(vsList.get(j));
							else
								tmpvsList.addAll(vsList.get(j).sources);
						}
						newvsList.add(new VirtualSignal(tmpvsList));
					}
				}
				start = i;
				if (i < getTotalSignal())
					lastSig = refp.get(i);
			}
				
		}
		this.refp = newrefp;
		this.vsList = newvsList;
	}

}
