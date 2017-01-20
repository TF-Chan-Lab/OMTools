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


package aldenjava.opticalmapping.statistics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;


public class DataStatistics {

	// total signals
	// signal distribution of molecules
	// min signals
	
	
//	private final LinkedHashMap<String, FragmentNode> fragmentmap;
//	public DataStatistics(LinkedHashMap<String, FragmentNode> fragmentmap)
//	{
//		this.fragmentmap = fragmentmap;
//	}
//	public int getTotalFragment()
//	{
//		return fragmentmap.size();
//	}
//	public int getTotalSignals()
//	{
//		int total = 0;
//		for (FragmentNode fragment : fragmentmap.values())
//			total += fragment.getTotalSegment() - 1;
//		return total;
//	}
//	public double getAverageSignals()
//	{		
//		return getTotalSignals() / (double) getTotalFragment();
//	}
//	public int getMinSignals()
//	{
//		int min = Integer.MAX_VALUE;
//		for (FragmentNode fragment : fragmentmap.values())
//			if (fragment.getTotalSegment() - 1 < min)
//				min = fragment.getTotalSegment() - 1;
//		return min;
//	}
//	public int getMaxSignals()
//	{
//		int max = 0;
//		for (FragmentNode fragment : fragmentmap.values())
//			if (fragment.getTotalSegment() - 1 > max)
//				max = fragment.getTotalSegment() - 1;
//		return max;
//	}
//	public int getTotalSize()
//	{
//		int total = 0;
//		for (FragmentNode fragment : fragmentmap.values())
//			total += fragment.size;
//		return total;
//	}
//	public double getAverageSize()
//	{		
//		return getTotalSize() / (double) getTotalFragment();
//	}
//	public int getMinSize()
//	{
//		int min = Integer.MAX_VALUE;
//		for (FragmentNode fragment : fragmentmap.values())
//			if (fragment.size < min)
//				min = fragment.size;
//		return min;
//	}
//	public int getMaxSize()
//	{
//		int max = 0;
//		for (FragmentNode fragment : fragmentmap.values())
//			if (fragment.size > max)
//				max = fragment.size;
//		return max;
//	}
//	// 0: x < min
//	// 1: min <= x < min + range
//	// ....
//	// n: max <= x
//	public int[] getSizeDistribution(int min, int max, int range)
//	{
//		int[] count = new int[(max - min) / range + 2];
//		for (FragmentNode fragment : fragmentmap.values())
//		{
//			if (fragment.size >= max)
//				count[count.length - 1]++;
//			else
//				if (fragment.size < min)
//					count[0]++;
//				else
//					count[((fragment.size - min) / range) + 1]++;
//		}
//		return count;
//	}
//	public int[] getSignalDistribution(int min, int max, int range)
//	{
//		int[] count = new int[(max - min) / range + 2];
//		for (FragmentNode fragment : fragmentmap.values())
//		{
//			if (fragment.getTotalSegment() - 1 >= max)
//				count[count.length - 1]++;
//			else
//				if (fragment.getTotalSegment() - 1 < min)
//					count[0]++;
//				else
//					count[((fragment.getTotalSegment() - 1 - min) / 2) + 1]++;
//		}
//		return count;
//	}
//	
//	public void outputStandardStatistics(String output) throws IOException
//	{
//		FileWriter fw = new FileWriter(output);
//		fw.write("TotalMolecules\tTotalSize\tAverageSize\tMinSize\tMaxSize\tTotalSignals\tAverageSignals\tMinSignals\tMaxSignals\n");
//		fw.write(String.format("%d\t%d\t%f\t%d\t%d\t%d\t%f\t%d\t%d\n",getTotalFragment(), getTotalSize(), getAverageSize(), getMinSize(), getMaxSize(), getTotalSignals(), getAverageSignals(), getMinSignals(), getMaxSignals()));
//		
//		fw.write("SignalDistribution\n");
//		int range = 2;
//		fw.write("0");
//		for (int i = 1; i < getMaxSignals() / range + 1; i++)
//			fw.write("\t" + Integer.toString(i * range));
//		fw.write("\n");
//		int[] count = getSignalDistribution(0, getMaxSignals(), range);
//		fw.write(Integer.toString(count[0]));
//		for (int i = 1; i < count.length; i++)
//			fw.write(String.format("%d\t", count[i]));
//		fw.write("\n");
//		
//		
//		fw.write("SizeDistribution\n");
//		range = 10000;
//		fw.write("0");
//		for (int i = 1; i < getMaxSize() / range + 1; i++)
//			fw.write("\t" + Integer.toString(i * range));
//		fw.write("\n");
//		count = getSizeDistribution(0, getMaxSize(), range);
//		fw.write(Integer.toString(count[0]));
//		for (int i = 1; i < count.length; i++)
//			fw.write(String.format("%d\t", count[i]));
//		fw.write("\n");
//
//		fw.close();
//	}
//	

	
	
	
	
	private int totalMolecules;

	private long totalSize;
	private long minSize;
	private long maxSize;
	int[] sizeCount; // sizeCount[0]: [0,50k)

	private int totalSignals;
	private int minSignals;
	private int maxSignals;
	private long closestSignalDistance;
	int[] signalCount; // signalCount[499]: for signals >= 500

	private LinkedHashMap<String, DataNode> optrefmap = null;
	
	
	long windowSize = 25000L;
	/*
	int windowSignal = 1;
	int minSignalCount = 0;
	int minSizeCount = 0;
	int maxSignalCount = 0;
	int maxSizeCount = 0;
	*/
	
	public DataStatistics()
	{
		this.reset();
	}
	public DataStatistics(LinkedHashMap<String, DataNode> optrefmap)
	{
		this();
		this.optrefmap = optrefmap;
	}
	/*
	public void setParameters() {
		
	}
	public void setParameters(OptionSet options) {
		
	}
	*/
	
	public void reset()
	{
		this.totalMolecules = 0;
		
		this.totalSize = 0;
		this.minSize = Integer.MAX_VALUE;
		this.maxSize = Integer.MIN_VALUE;
		this.closestSignalDistance = Long.MAX_VALUE;
		this.sizeCount = new int[40];
		
		this.totalSignals = 0;
		this.minSignals = Integer.MAX_VALUE;
		this.maxSignals = Integer.MIN_VALUE;
		this.signalCount = new int[500];
				
	}
	
	public void update(DataNode fragment)
	{
		totalMolecules++;
		
		// Molecule size
		totalSize += fragment.length();
		if (fragment.length() / windowSize >= sizeCount.length)
			sizeCount[sizeCount.length - 1]++;
		else
			sizeCount[(int) (fragment.size / windowSize)]++;
		if (fragment.length() < minSize)
			minSize = fragment.length();
		if (fragment.length() > maxSize)
			maxSize = fragment.length();

		// Molecule Signals
		totalSignals += fragment.getTotalSegment() - 1;
		if (fragment.getTotalSegment() - 1 >= signalCount.length)
			signalCount[signalCount.length - 1]++;
		else
			signalCount[fragment.getTotalSegment() - 1]++;
		if (fragment.getTotalSegment() - 1 < minSignals)
			minSignals = fragment.getTotalSegment() - 1;
		if (fragment.getTotalSegment() - 1 > maxSignals)
			maxSignals = fragment.getTotalSegment() - 1;
		
		for (int i = 1; i < fragment.refp.length; i++) // First and last refl is not counted
			if (fragment.getRefl(i) < closestSignalDistance)
				closestSignalDistance = fragment.getRefl(i);
	}
	
	public void outputStatistics(String outputfile) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
		bw.write(String.format("Total Molecules:\t%d\n", totalMolecules));
		bw.write(String.format("Total Molecule Size:\t%d\n", totalSize));
		bw.write(String.format("Average Molecule Size:\t%f\n", totalSize / (double) totalMolecules));
		bw.write(String.format("Min Molecule Size:\t%d\n", minSize));
		bw.write(String.format("Max Molecule Size:\t%d\n", maxSize));
		bw.write(String.format("Molecule Size Distribution\n"));
		// 5% of molecules are brought together
		int maxSizeCount = 0;
		int moleculeNoOversize = 0;
		for (int i = sizeCount.length - 1; i >= 0; i--)
		{
			moleculeNoOversize += sizeCount[i];
			if (moleculeNoOversize / (double) totalMolecules >= 0.005)
			{
				maxSizeCount = i;
				break;
			}
		}
		for (int i = 0; i < maxSizeCount; i++)
			bw.write(String.format("%d-%d\t", i * windowSize, (i + 1) * windowSize - 1));
		bw.write(String.format(">=%d\n", maxSizeCount * windowSize));
		for (int i = 0; i < maxSizeCount; i++)
			bw.write(String.format("%d\t", sizeCount[i]));
		bw.write(String.format("%d\n", moleculeNoOversize));
		bw.newLine();
		
		bw.write(String.format("Total Signals:\t%d\n", totalSignals));
		bw.write(String.format("Average Signal per Molecule:\t%f\n", totalSignals / (double) totalMolecules));
		bw.write(String.format("Min Signal of Molecule:\t%d\n", minSignals));
		bw.write(String.format("Max Signal of Molecule:\t%d\n", maxSignals));
		bw.write(String.format("Average Signal per kb:\t%f\n", totalSignals / (double) totalSize * 1000));	
		bw.write(String.format("Signal Distribution:\t%d\n", totalSignals));
		// 5% of molecules are brought together
		int maxSignalCount = 0;
		int moleculeNoOversignal = 0;
		for (int i = signalCount.length - 1; i >= 0; i--)
		{
			moleculeNoOversignal += signalCount[i];
			if (moleculeNoOversignal / (double) totalMolecules >= 0.005)
			{
				maxSignalCount = i;
				break;
			}
		}
		for (int i = 0; i < maxSignalCount; i++)
			bw.write(String.format("%d\t", i));
		bw.write(String.format(">=%d\n", maxSignalCount));
		for (int i = 0; i < maxSignalCount; i++)
			bw.write(String.format("%d\t", signalCount[i]));
		bw.write(String.format("%d\n", moleculeNoOversignal));
		bw.close();
		
	}
	
	private String getCov()
	{
		if (optrefmap == null)
			return "N/A";
		else
			return "" + totalSize/ (double) DataNode.getTotalSize(optrefmap);
	}
	private String getString()
	{
		return totalMolecules + "\t" + getCov() + "\t"
				+ totalSignals + "\t" + (totalSignals / (double) totalMolecules) + "\t" + minSignals + "\t" + maxSignals + "\t"
				+ totalSize + "\t" + (totalSize / (double) totalMolecules) + "\t" + minSize + "\t" + maxSize + "\t"
				+ (totalSignals / (double) totalSize * 1000) + "\t" + closestSignalDistance;
	}
	
	private static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("DataStatistics Options", level);
		/*
		parser.accepts("minsize", "Minimum size").withOptionalArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("winsize", "Window size for molecule size").withOptionalArg().ofType(Long.class).defaultsTo(25000L);
		parser.accepts("maxsize", "Maximum size").withOptionalArg().ofType(Integer.class).defaultsTo(1000000);
		parser.accepts("minsignal", "Minimum signal").withOptionalArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("winsignal", "Window size for molecule signal").withOptionalArg().ofType(Long.class).defaultsTo(25000L);
		parser.accepts("maxsignal", "Maximum signal").withOptionalArg().ofType(Integer.class).defaultsTo(500);
		*/
		parser.accepts("statout", "Statistics output").withRequiredArg().ofType(String.class).required();
	}

	
	public static void main(String[] args) throws IOException
	{
		ExtendOptionParser parser = new ExtendOptionParser(DataStatistics.class.getSimpleName(), "Generates statistics of the data file.");
		DataStatistics.assignOptions(parser, 1);
		
		OptMapDataReader.assignOptions(parser, 1);
		ReferenceReader.assignOptions(parser, 1);
		parser.addHeader(null, 0); // Reset the default parameters
		parser.accepts("refmapin", "Input reference map file").withRequiredArg().ofType(String.class);
		
		if (args.length == 0)
		{
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		LinkedHashMap<String, DataNode> optrefmap = null;
		if (options.has("refmapin"))
			optrefmap = ReferenceReader.readAllData(options);
		if (options.has("optmapin"))
			if (options.valuesOf("optmapin").size() >= 1)
			{
				List<String> mapList = (List<String>) options.valuesOf("optmapin");
				int optmapinformat = (int) options.valueOf("optmapinformat");
				String statout = (String) options.valueOf("statout");
				BufferedWriter bw = new BufferedWriter(new FileWriter(statout));
				int[][] sizeCounts = new int[mapList.size()][];
				int[][] signalCounts = new int[mapList.size()][];
				int[] totalMole = new int[mapList.size()];
				// Basic Statistics
				int count = 0;
				bw.write("#Basic Statistics\n");
				bw.write("#Filename\tTotal molecules\tCoverage\tTotal signals\tAverage signals\tMin Signals\tMax Signals\tTotal size\tAverage size\tMin size\tMax size\tSignal Density (kb)\tClosest Signal Distance\n");
				for (String filename : mapList)
				{
					DataStatistics datastat = new DataStatistics(optrefmap);
					OptMapDataReader omdr = new OptMapDataReader(filename, optmapinformat);
					DataNode fragment;
					while ((fragment = omdr.read()) != null)
						datastat.update(fragment);
					bw.write(filename + "\t" + datastat.getString() + "\n");
					omdr.close();
					sizeCounts[count] = datastat.sizeCount;
					signalCounts[count] = datastat.signalCount;
					totalMole[count] = datastat.totalMolecules;
					count++;
				}
				
				// Size and signal dist
				int maxSizeCount = 0;
				int maxSignalCount = 0;
				for (int k = 0; k < mapList.size(); k++) {
					int moleculeNoOversize = 0;
					for (int i = sizeCounts[k].length - 1; i >= 0; i--)
					{
						moleculeNoOversize += sizeCounts[k][i];
						if (moleculeNoOversize / (double) totalMole[k] >= 0.005)
						{
							if (i > maxSizeCount)
								maxSizeCount = i;
							break;
						}
					}
					
					int moleculeNoOversignal = 0;
					for (int i = signalCounts[k].length - 1; i >= 0; i--)
					{
						moleculeNoOversignal += signalCounts[k][i];
						if (moleculeNoOversignal / (double) totalMole[k] >= 0.005)
						{
							if (i > maxSignalCount)
								maxSignalCount = i;
							break;
						}
					}
				}
				
				bw.write("\n");
				bw.write("#Size Distribution\n");
				bw.write("#Filename\t");
				for (int i = 0; i < maxSizeCount; i++)
					bw.write(String.format("%d-%d\t", i * 25000, (i + 1) * 25000 - 1));
				bw.write(String.format(">=%d\n", maxSizeCount * 25000));
				for (int k = 0; k < mapList.size(); k++) {
					bw.write(mapList.get(k) + "\t");
					int sum = 0;
					for (int i = 0; i < maxSizeCount; i++)
					{
						sum += sizeCounts[k][i];
						bw.write((sizeCounts[k][i] + "\t"));
					}
					bw.write(String.format("%d\n", totalMole[k] - sum));
				}
				bw.write("#Size Distribution (Percentage)\n");
				bw.write("#Filename\t");
				for (int i = 0; i < maxSizeCount; i++)
					bw.write(String.format("%d-%d\t", i * 25000, (i + 1) * 25000 - 1));
				bw.write(String.format(">=%d\n", maxSizeCount * 25000));
				for (int k = 0; k < mapList.size(); k++) {
					bw.write(mapList.get(k) + "\t");
					int sum = 0;
					for (int i = 0; i < maxSizeCount; i++)
					{
						sum += sizeCounts[k][i];
						bw.write((sizeCounts[k][i] / (double) totalMole[k] + "\t"));
					}
					bw.write(String.format("%f\n", (totalMole[k] - sum) / (double) totalMole[k]));
				}
				
				bw.write("\n");
				bw.write("#Signal Distribution\n");
				bw.write("#Filename\t");
				for (int i = 0; i < maxSignalCount; i++)
					bw.write(String.format("%d\t", i));
				bw.write(String.format(">=%d\n", maxSignalCount));
				for (int k = 0; k < mapList.size(); k++) {
					bw.write(mapList.get(k) + "\t");
					int sum = 0;
					for (int i = 0; i < maxSignalCount; i++) 
					{
						sum += signalCounts[k][i];
						bw.write(signalCounts[k][i] + "\t");
					}
					bw.write(String.format("%d\n", totalMole[k] - sum));
				}
				bw.write("#Signal Distribution (Percentage)\n");
				bw.write("#Filename\t");
				for (int i = 0; i < maxSignalCount; i++)
					bw.write(String.format("%d\t", i));
				bw.write(String.format(">=%d\n", maxSignalCount));
				for (int k = 0; k < mapList.size(); k++) {
					bw.write(mapList.get(k) + "\t");
					int sum = 0;
					for (int i = 0; i < maxSignalCount; i++) 
					{
						sum += signalCounts[k][i];
						bw.write(signalCounts[k][i] / (double) totalMole[k] + "\t");
					}
					bw.write(String.format("%f\n", (totalMole[k] - sum) / (double) totalMole[k]));
				}

				bw.close();
			}
//			else
//			{
//				String statout = (String) options.valueOf("statout");
//				DataStatistics datastat = new DataStatistics();
//				OptMapDataReader omdr = new OptMapDataReader(options);
//				DataNode fragment;
//				while ((fragment = omdr.read()) != null)
//					datastat.update(fragment);
//				datastat.outputStatistics(statout);
//				omdr.close();
//			}

//		String folder = "C:\\Users\\Alden\\Desktop\\Run3\\";
//		String inputfile = folder + "NA12891b_20120000_0003_df_2.cmap";
//		String output = folder + "Stat.tsv";
//		int format = 6;
//		DataStatistics datastat = new DataStatistics(OptMapDataReader.readAllData(inputfile, format));
//		datastat.outputStandardStatistics(output);
//		
//		System.out.println("Molecules");
		
//		int format = 0;
//		DataStatistics datastat = new DataStatistics(OptMapDataReader.readAllData(inputfile, format));
//		
//		int range = 2;
//		int[] signaldistributionyint = datastat.getSignalDistribution(0, datastat.getMaxSignals(), range);
//		double[] signaldistributiony = new double[signaldistributionyint.length];
//		for (int i = 0; i < signaldistributiony.length; i++)
//			signaldistributiony[i] = signaldistributionyint[i];
//		double[] signaldistributionx = new double[signaldistributiony.length];
//		signaldistributionx[0] = 0;
//		for (int i = 1; i < signaldistributionx.length; i++)
//			signaldistributionx[i] = signaldistributionx[i - 1] + range;
//		double[][] data = {signaldistributionx, signaldistributiony};
//		DefaultXYDataset xy = new DefaultXYDataset();
//		xy.addSeries("D", data);
//		NumberAxis axisx = new NumberAxis();
//		axisx.setRange(0, 100);
//		NumberAxis axisy = new NumberAxis();
//		axisy.setRange(0, 500);
//		XYPlot xyplot = new XYPlot(xy, axisx, axisy, new DefaultXYItemRenderer());
//		JFreeChart chart = new JFreeChart(xyplot);
//        
//        ChartUtilities.saveChartAsJPEG(new File(folder + "out.jpg"), chart, 1000, 1000);		
//		

	}
}