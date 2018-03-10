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


package aldenjava.opticalmapping.data.mappingresult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.Cigar;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.SimulationInfo;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

/**
 * Alignment Result Writer
 * 
 * @author Alden
 * 
 */
public class OptMapResultReader extends OMReader<OptMapResultNode> {

	private ResultFormat rformat;

	private Map<String, DataNode> optrefmap = null;
	private Map<String, DataNode> fragmentInfo = null;

	public OptMapResultReader(OptionSet options) throws IOException {
		this((String) options.valueOf("optresin"), (int) options.valueOf("optresinformat"));
	}

	public OptMapResultReader(String filename) throws IOException {
		this(filename, -1);
	}

	public OptMapResultReader(String filename, int format) throws IOException {
		this(filename, ResultFormat.lookup(filename, format));
	}

	public OptMapResultReader(String filename, ResultFormat rformat) throws IOException {
		super(filename);
		if (rformat == null)
			throw new IOException("Unknown format.");
		this.rformat = rformat;
		if (rformat == ResultFormat.ALN)
			proceedNextLine();
	}

	public OptMapResultReader(InputStream stream, ResultFormat rformat) throws IOException {
		super(stream);
		if (rformat == null)
			throw new IOException("Unknown format.");
		this.rformat = rformat;
		if (rformat == ResultFormat.ALN)
			proceedNextLine();
	}

	public void importRefInfo(LinkedHashMap<String, DataNode> optrefmap) {
		this.optrefmap = optrefmap;
	}

	public void importFragInfo(Map<String, DataNode> fragmentInfo) {
		this.fragmentInfo = fragmentInfo;
	}

	@Override
	public OptMapResultNode read() throws IOException {
		if (nextline == null)
			return null;
		else
			switch (rformat) {
				case OMA:
					return parseOMAResult();
				case OMD:
					return parseOMDResult();
				case XMAP:
					return parseXMAPResult();
				case VAL:
					return parseVALResult();
				case SOMA:
					return parseSOMAv2Result();
				case PSL:
					return parseTwinPSLResult();
				case ALN:
					return parseMalignerALNResult();
				default:
					return null;
			}
	}

	OptMapResultNode previousResult = null;

	public List<OptMapResultNode> readNextList() throws IOException {
		List<OptMapResultNode> resultlist = new ArrayList<OptMapResultNode>();
		OptMapResultNode result;
		while ((result = read()) != null) {
			if (previousResult == null) {
				previousResult = result;
			} else if (result.parentFrag.name.equalsIgnoreCase(previousResult.parentFrag.name))
				resultlist.add(result);
			else {
				resultlist.add(0, previousResult);
				previousResult = result;
				break;
			}
		}
		if (result == null)
			if (previousResult != null) {
				resultlist.add(0, previousResult);
				previousResult = null;
			}

		if (resultlist.isEmpty())
			return null;
		else
			return resultlist;
	}

	private OptMapResultNode parseOMAResult() throws IOException {

		String s = this.nextline;
		s = s.trim();
		String[] l = s.split("\t");

		String id = l[0];
		DataNode f = null;
		int totalSegment = Integer.parseInt(l[1]);
		String refl = l[2];
		if (fragmentInfo != null && fragmentInfo.containsKey(id))
			f = fragmentInfo.get(id);
		else if (!refl.isEmpty()) {
			f = new DataNode(id, DataNode.parseReflInString(refl, ";"));
			if (f.getTotalSegment() != totalSegment) {
				System.err.println("Warning: Inconsistent total segments and fragment details");
			}
			if (fragmentInfo == null)
				fragmentInfo = new HashMap<>();
			fragmentInfo.put(f.name, f);
		}
		else {
			System.err.println("Warning: no available molecule info: " + id);
			System.err.println("Resume.");
		}

		String mappedref = l[3];

		long alignmentstart = -1;
		long alignmentstop = -1;
		int mappedstrand = 0;
		int subrefstart = -1;
		int subrefstop = -1;
		int subfragstart = -1;
		int subfragstop = -1;
		double best_score = Double.NEGATIVE_INFINITY;
		Cigar cigar = new Cigar();
		double confidence = -1;
		GenomicPosNode mappedRegion = null;
		if (!((mappedref.equalsIgnoreCase("Unmapped")) || (mappedref.equalsIgnoreCase("Discarded")) || (mappedref.isEmpty()))) {

			mappedstrand = (l[4].equalsIgnoreCase("forward") || l[4].equals("+")) ? 1 : (l[4].equalsIgnoreCase("reverse") || l[4].equals("-")) ? -1 : 0;
			best_score = Double.parseDouble(l[5]);
			if (l.length == 14) // backward compatible
			{
				confidence = Double.parseDouble(l[6]);
				subrefstart = Integer.parseInt(l[7]);
				subrefstop = Integer.parseInt(l[8]);
				subfragstart = Integer.parseInt(l[9]);
				subfragstop = Integer.parseInt(l[10]);
				alignmentstart = Long.parseLong(l[11]);
				alignmentstop = Long.parseLong(l[12]);
				if (l[13].equalsIgnoreCase("null") || l[13].isEmpty())
					cigar = null;
				else
					cigar.importCigar(l[13]);
			} else {
				subrefstart = Integer.parseInt(l[6]);
				subrefstop = Integer.parseInt(l[7]);
				subfragstart = Integer.parseInt(l[8]);
				subfragstop = Integer.parseInt(l[9]);
				alignmentstart = Long.parseLong(l[10]);
				alignmentstop = Long.parseLong(l[11]);
				if (l[12].equalsIgnoreCase("null") || l[12].isEmpty())
					cigar = null;
				else
					cigar.importCigar(l[12]);
			}
			mappedRegion = new GenomicPosNode(mappedref, alignmentstart, alignmentstop);
		}

		OptMapResultNode result = new OptMapResultNode(f, mappedRegion, mappedstrand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, best_score, confidence);
		proceedNextLine();
		return result;

	}

	private OptMapResultNode parseOMDResult() throws IOException {

		String s = this.nextline;
		s = s.trim();

		String[] l = s.split("\t");

		String id = l[0];
		String fromref = l[1];
		int strand = (l[2].equalsIgnoreCase("forward") || l[2].equals("+")) ? 1 : (l[2].equalsIgnoreCase("reverse") || l[2].equals("-")) ? -1 : 0;
		long genomestart = Long.parseLong(l[3]);
		long genomestop = Long.parseLong(l[4]);
		// int size = Integer.parseInt(l[5]);
		// l[6]: no of labels
		// String[] fragmentstring = l[7].split(";");

		// int[] fragment;
		// if (l[7].isEmpty())
		// fragment = new int[0];
		// else
		// {
		// fragment = new int[fragmentstring.length];
		// for (int i = 0; i < fragmentstring.length; i++)
		// fragment[i] = Integer.parseInt(fragmentstring[i]);
		// }

		DataNode f = null;
		if (!l[7].isEmpty()) {
			if (fragmentInfo != null && fragmentInfo.containsKey(id))
				f = fragmentInfo.get(id);
			else {
				f = new DataNode(id, DataNode.parseReflInString(l[7], ";"));
				if (fragmentInfo == null)
					fragmentInfo = new HashMap<>();
				fragmentInfo.put(f.name, f);
			}
			if (SimulationInfo.checkInfoValid(fromref, genomestart, genomestop, strand))
				f.importSimulationInfo(new SimulationInfo(new GenomicPosNode(fromref, genomestart, genomestop), strand));
		} else if (fragmentInfo != null) {
			if (fragmentInfo.containsKey(id))
				f = fragmentInfo.get(id);
			else {
				System.err.println("Error, data file does not contain molecule: " + id);
				System.err.println("Resume.");
			}
		}
		 if (fragmentInfo != null) {
				if (fragmentInfo.containsKey(id))
					f = fragmentInfo.get(id);
		 }

		String ref = l[8];

		long alignmentstart = -1;
		long alignmentstop = -1;
		int mappedstrand = 0;
		int subrefstart = -1;
		int subrefstop = -1;
		int subfragstart = -1;
		int subfragstop = -1;
		// double subfragratio = -1;
		double best_score = Double.NEGATIVE_INFINITY;
		Cigar cigar = new Cigar();
		double confidence = -1;
		// int falsep = -1;
		// int falsen = -1;
		// double scale = -1;
		// boolean mappedunique = false;
		// boolean mapped = false;
		if (!((ref.compareToIgnoreCase("Unmapped") == 0) || (ref.compareToIgnoreCase("Discarded") == 0))) {
			alignmentstart = Long.parseLong(l[9]);
			alignmentstop = Long.parseLong(l[10]);
			mappedstrand = (l[11].equalsIgnoreCase("forward") || l[11].equals("+")) ? 1 : (l[11].equalsIgnoreCase("reverse") || l[11].equals("-")) ? -1 : 0;
			subrefstart = Integer.parseInt(l[12]);
			subrefstop = Integer.parseInt(l[13]);
			subfragstart = Integer.parseInt(l[14]);
			subfragstop = Integer.parseInt(l[15]);
			// subfragratio = Double.parseDouble(l[16]);

			best_score = Double.parseDouble(l[17]);
			//
			if (l[18].equalsIgnoreCase("null") || l[18].isEmpty())
				cigar = null;
			else
				cigar.importCigar(l[18]);
			confidence = Double.parseDouble(l[19]);
			// falsep = Integer.parseInt(l[20]);
			// falsen = Integer.parseInt(l[21]);
			// scale = Double.parseDouble(l[22]);
			// mapped = Boolean.parseBoolean(l[23]);
			// mappedunique = Boolean.parseBoolean(l[24]);
			// confidence = Double.parseDouble(l[13]);
			// falsep = Integer.parseInt(l[14]);
			// falsen = Integer.parseInt(l[15]);
			// scale = Double.parseDouble(l[16]);
			// mapped = Boolean.parseBoolean(l[17]);
			// mappedunique = Boolean.parseBoolean(l[18]);
		}
		// this.total_fragment++;
		// this.nextline = fp.readLine();
		proceedNextLine();
		// return new OptMapResultNode(f, ref, best_score, cigar, mappedstrand, alignmentstart, alignmentstop, falsep, falsen, scale, subrefstart, subrefstop, subfragstart, subfragstop, subfragratio, confidence, mapped);
		return new OptMapResultNode(f, new GenomicPosNode(ref, alignmentstart, alignmentstop), mappedstrand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, best_score, confidence);
	}

	private int getSubFragStart(DataNode fragment, long mappedfragstart, int mappedstrand) {
		if (fragment == null)
			return -1;
		int index = Arrays.binarySearch(fragment.refp, mappedfragstart);
		if (index >= 0)
			if (mappedstrand == 1)
				return index + 1;
			else if (mappedstrand == -1)
				return index;
			else
				return -1;
		else
			return -1;
		// long size = fragment.getRefl(0);
		// int nextSegment = 1;
		// while (nextSegment < fragment.getTotalSegment())
		// {
		// if (size == mappedfragstart)
		// {
		// if (mappedstrand == 1)
		// return nextSegment;
		// else
		// if (mappedstrand == -1)
		// return nextSegment - 1;
		// }
		// size += fragment.getRefl(nextSegment) + 1;
		// nextSegment++;
		// }
		// return -1;
	}

	private int getSubFragStop(DataNode fragment, long mappedfragstop, int mappedstrand) {
		if (fragment == null)
			return -1;
		int index = Arrays.binarySearch(fragment.refp, mappedfragstop);
		if (index >= 0)
			if (mappedstrand == 1)
				return index;
			else if (mappedstrand == -1)
				return index + 1;
			else
				return -1;
		else
			return -1;

		// long size = fragment.getRefl(0);
		// int nextSegment = 1;
		// while (nextSegment < fragment.getTotalSegment())
		// {
		// if (size == mappedfragstop)
		// {
		// if (mappedstrand == 1)
		// return nextSegment - 1;
		// else
		// if (mappedstrand == -1)
		// return nextSegment;
		// }
		// size += fragment.getRefl(nextSegment) + 1;
		// nextSegment++;
		// }
		// return -1;
	}

	private int getSubRefStart(DataNode ref, long mappedrefstart) {
		if (ref == null)
			return -1;
		for (int i = 0; i < ref.refp.length; i++)
			if (ref.refp[i] == mappedrefstart)
				return i + 1;
			else if (ref.refp[i] > mappedrefstart)
				if (i == 0)
					return -1;
				else {
					System.err.print("Warning: coordinate rounded from ");
					System.err.print(Long.toString(mappedrefstart));
					System.err.print(" to ");
					System.err.println(Long.toString(ref.refp[i - 1]));
					return i - 1;
				}
		return -1;

	}

	private int getSubRefStop(DataNode ref, long mappedrefstop) {
		if (ref == null)
			return -1;
		for (int i = 0; i < ref.refp.length; i++)
			if (ref.refp[i] == mappedrefstop)
				return i;
			else if (ref.refp[i] > mappedrefstop) {
				System.err.print("Warning: coordinate rounded from ");
				System.err.print(Long.toString(mappedrefstop));
				System.err.print(" to ");
				System.err.println(Long.toString(ref.refp[i - 1]));
				return i - 1;
			}
		return -1;

	}

	private OptMapResultNode parseXMAPResult() throws IOException, NullPointerException, NumberFormatException, ArrayIndexOutOfBoundsException {
		String s = this.nextline;
		s = s.trim();
		String[] l = s.split("\\s+");
		// 1 11841 1 3510.0 58682.0 3955.0 61196.3 + 6.10 2M1D2M1D5M
		// l[0]: Map Entry ID
		String id = l[1];
		DataNode fragment = new DataNode(id, 0, null);
		String mappedref = l[2]; // sadly it's always 1 // luckily it's not always 1
		long querymapstart = (long) Double.parseDouble(l[3]);
		long querymapstop = (long) Double.parseDouble(l[4]);
		long refmapstart = (long) Double.parseDouble(l[5]);
		long refmapstop = (long) Double.parseDouble(l[6]);

		int mappedstrand = 0;
		long mappedstart = refmapstart;
		long mappedstop = refmapstop;
		// double scale = (double) Math.abs(querymapstart - querymapstop) / (double) Math.abs(refmapstop - refmapstart);
		if (l[7].equalsIgnoreCase("+")) {
			mappedstrand = 1;
			// mappedstart = refmapstart - querymapstart;
		} else if (l[7].equalsIgnoreCase("-")) {
			mappedstrand = -1;
			// mappedstart = refmapstart - querymapstop;
		}
		int subrefstart = -1;
		int subrefstop = -1;
		int subfragstart = -1;
		int subfragstop = -1;
		if (fragmentInfo != null) {
			if (fragmentInfo.containsKey(id)) {
				fragment = fragmentInfo.get(id);
				subfragstart = this.getSubFragStart(fragment, querymapstart, mappedstrand);
				subfragstop = this.getSubFragStop(fragment, querymapstop, mappedstrand);
			} else {
				System.err.println("Error, data file does not contain molecule: " + id);
				System.err.println("Resume.");
			}
		}
		if (optrefmap != null) {
			if (optrefmap.containsKey(mappedref)) {
				subrefstart = this.getSubRefStart(optrefmap.get(mappedref), refmapstart);
				subrefstop = this.getSubRefStop(optrefmap.get(mappedref), refmapstop);
			} else {
				System.err.println("Error, reference file does not contain: " + mappedref);
				System.err.println("Resume.");
			}
		}

		// NOTE!!!!! Mind any errors if you forget to delete this ////
		// mappedstart = refmapstart;
		// ////////////////////////////////////////////////////////////

		mappedstop = refmapstop; // bug here as no real map stop is provided
		double confidence = Double.parseDouble(l[8]);
		double mappedscore = Double.parseDouble(l[8]);
		Cigar cigar = new Cigar();
		cigar.importCigar(l[9]);
		// int falsen = cigar.getFN();
		// int falsep = cigar.getFP();

		// 3M1D2M1I3M1I1D1M1I1M1D1M1D2I1M1I1M1I1D1M1I10M
		// this.total_fragment++;
		// this.nextline = fp.readLine();
		// return new OptMapResultNode(, mappedref, mappedscore, cigar, strand, mappedstart, mappedstop, falsep, falsen, scale, (int) mappedstart, (int) mappedstop, (int) querymapstart, (int) querymapstop, -1, -1, false);
		proceedNextLine();

		return new OptMapResultNode(fragment, new GenomicPosNode(mappedref, mappedstart, mappedstop), mappedstrand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, mappedscore, confidence);

	}

	public OptMapResultNode parseVALResult() throws IOException {
		// reference maps: 1
		// cur_map:0 good:0 max_score:33.3282
		// forward:
		// and hg38 for 1
		// [ 0:5.298 ]->[ 1123:1.732, 1124:2.398 ]
		// [ 1:5.436 ]->[ 1125:5.4 ]
		// [ 2:18.012 ]->[ 1126:9.397, 1127:6.523 ]
		// [ 3:13.148 ]->[ 1128:15.593 ]
		// [ 4:4.386 ]->[ 1129:3.382 ]
		// [ 5:10.879 ]->[ 1130:6.961 ]
		// [ 6:48.347 ]->[ 1131:13.764, 1132:6.121, 1133:32.386 ]
		// [ 7:54.269 ]->[ 1134:26.992, 1135:29.856 ]
		// [ 8:5.236 ]->[ 1136:6.938 ]
		// [ 9:12.601 ]->[ 1137:9.718 ]
		// [ 10:4.864 ]->[ 1138:4.185 ]
		// [ 11:3.564 ]->[ 1139:3.017 ]
		// [ 12:9.413 ]->[ 1140:8.943 ]
		// s-score:33.3282
		// t-score:11.909
		while (nextline != null && !(nextline.startsWith("forward") || nextline.startsWith("reverse")))
			proceedNextLine();
		if (nextline == null)
			return null;
		int mappedstrand = nextline.startsWith("forward") ? 1 : nextline.startsWith("reverse") ? -1 : 0;
		proceedNextLine();
		// keyword: and REF for MOLE
		// String[] l = nextline.split("and");
		// l = l[1].split("for");
		// String refName = l[0].trim();
		// String moleName = l[1].trim();

		// keyword: for x and x
		String[] l = nextline.split("for");
		String moleName = l[1].trim();
		String refName;
		if (moleName.contains("and")) {
			String[] tmp = moleName.split("and");
			moleName = tmp[0].trim();
			refName = tmp[1].trim();
		}
		else {
			proceedNextLine();
			l = nextline.split("and");
			refName = l[1].trim();
		}
		proceedNextLine();

		DataNode data = new DataNode();
		if (fragmentInfo != null)
			if (fragmentInfo.containsKey(moleName))
				data = fragmentInfo.get(moleName);
			else {
				System.err.println("Error, data file does not contain molecule: " + moleName);
				System.err.println("Resume.");
			}
		if (data.name == null)
			data.name = moleName;
		int subfragstart = -1;
		int subfragstop = -1;
		int subrefstart = -1;
		int subrefstop = -1;
		Cigar cigar = new Cigar("M");
		while (!nextline.startsWith("s-score")) {
			if (nextline.startsWith("[")) {
				l = nextline.split("\\]\\->\\[");
				String moleString = l[0].split("\\[")[1].trim();
				String refString = l[1].split("\\]")[0].trim();
				List<Integer> moleStarts = new ArrayList<Integer>();
				List<Integer> refStarts = new ArrayList<Integer>();
				List<Double> moleSizes = new ArrayList<Double>();
				List<Double> refSizes = new ArrayList<Double>();
				double totalMoleSize = 0.001; // At least one signal as 1bp
				for (String segmentInfo : moleString.split(",")) {
					int moleStart = Integer.parseInt(segmentInfo.trim().split(":")[0]);
					double moleSize = Double.parseDouble(segmentInfo.trim().split(":")[1]);
					moleStarts.add(moleStart);
					moleSizes.add(moleSize);
					totalMoleSize += moleSize + 0.001; // Add the signal as 1bp
				}
				double totalRefSize = 0.001;
				for (String segmentInfo : refString.split(",")) {
					int refStart = Integer.parseInt(segmentInfo.trim().split(":")[0]);
					double refSize = Double.parseDouble(segmentInfo.trim().split(":")[1]);
					refStarts.add(refStart);
					refSizes.add(refSize);
					totalRefSize += refSize + 0.001;
				}

				// Normalize molecule size
				double factor = totalMoleSize / totalRefSize;
				int m = 0;
				int r = 0;
				double savedMoleSize = 0.001;
				double savedRefSize = 0.001;
				Cigar tmpCigar = new Cigar();
				while (m < moleSizes.size() - 1 && r < refSizes.size() - 1) {
					if (savedMoleSize + (moleSizes.get(m) + 0.001) * factor < savedRefSize + refSizes.get(r) + 0.001) {
						savedMoleSize += (moleSizes.get(m) + 0.001) * factor;
						m++;
						tmpCigar.append('I');
					} else {
						savedRefSize += refSizes.get(r) + 0.001;
						r++;
						tmpCigar.append('D');
					}
				}
				assert (m == moleSizes.size() || r == refSizes.size());

				while (m < moleSizes.size() - 1) {
					tmpCigar.append('I');
					m++;
				}
				while (r < refSizes.size() - 1) {
					tmpCigar.append('D');
					r++;
				}
				tmpCigar.append('M');

				cigar.append(tmpCigar);

				if (subfragstart == -1)
					subfragstart = moleStarts.get(0);
				if (subrefstart == -1)
					subrefstart = refStarts.get(0);
				subfragstop = moleStarts.get(moleStarts.size() - 1);
				subrefstop = refStarts.get(refStarts.size() - 1);
			}
			proceedNextLine();
		}
		// subfragstart fix
		subfragstart++;
		subfragstop++;
		// subrefstart++;
		// subrefstop++;
		// Correction only apply to frag
		if (mappedstrand == -1) { // Error will occur if valouev et al. does not align full molecule
			if (data.refp != null) {
				subfragstart = data.refp.length - subfragstart;
				subfragstop = data.refp.length - subfragstop;
			} else {
				int tmp = subfragstart;
				subfragstart = subfragstop;
				subfragstop = tmp;
			}
		}

		double mappedscore = -1;
		double confidence = -1;
		if (nextline.startsWith("s-score")) {
			mappedscore = Double.parseDouble(nextline.split("s\\-score:")[1]);
			proceedNextLine();
		}
		if (nextline.startsWith("t-score")) {
			confidence = Double.parseDouble(nextline.split("t\\-score:")[1]);
			proceedNextLine();
		}

		GenomicPosNode pos = new GenomicPosNode(refName, -1, -1);
		if (optrefmap != null) {
			if (optrefmap.containsKey(refName))
				pos = optrefmap.get(refName).getGenomicPos(subrefstart, subrefstop, true);
			else {
				System.err.println("Error, reference file does not contain: " + refName);
				System.err.println("Resume.");
			}
		}

		return new OptMapResultNode(data, pos, mappedstrand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, mappedscore, confidence);
		// return new OptMapResultNode();
	}

	public OptMapResultNode parseSOMAv2Result() throws IOException {
		// 115 520024 1 446 524
		// 15 405.021 0 1
		// 15146,758 966,49; 5053,253; 7117,356; 3167,159; 16710,836; 10957,548; 9948,498; 6368,319; 3260,163; 2017,101; 1673,84; 3170,159; 8569,429; 14900,745; 6918,346; 3051,153; 3602,181; 3544,178; 14731,737; 1212,61; 15264,764; 1249,63 8676,434; 3236,162; 2016,101; 22749,1138; 9014,451; 6755,338 849,43; 13547,678; 1506,76; 8215,411; 6537,327; 16140,808; 5538,277; 4778,239; 5168,259 5509,276; 9524,477; 20306,1016; 3649,183; 2708,136; 5672,284; 9066,454 24,2; 12968,649; 3453,173; 10290,515; 2537,127; 2062,104; 7973,399; 1131,57; 2874,144; 3409,171 117,6; 5198,260; 2833,142; 3831,192; 5280,264; 676,34 3109,156; 2455,123; 16975,849; 14055,703; 31798,1590; 10,1 106,6 12709,636; 2059,103; 1956,98; 2730,137; 2604,131 1924,97; 9036,452; 5062,254; 4560,228; 2699,135; 4576,229
		// 15993; 5419; 7223; 3593; 16243; 11320; 9983; 6307; 3408; 2677; 1927; 2470; 9436; 7398 7012; 7411; 2665; 3619; 3451; 15200; 1378; 16085; 9625; 3314; 2132; 22929; 8737; 8563; 15140; 1406; 6453; 6513; 11567 4582; 6016; 4797; 11081; 9619; 20396; 3809; 2518; 5703; 9799; 13344; 2553; 7244 3243; 3433; 1610; 8279; 1053; 2849; 3684; 5318; 3187; 3315; 5847; 3980; 1965; 9546 8126; 14121; 32193; 13209; 1496; 2657; 2734; 4104; 9185; 5520; 4685; 1990; 2707 1930
		// 15993 21412 28635 32228 48471 59791 69774 76081 79489 82166 84093 86563 95999 103397 110409 117820 120485 124104 127555 142755 144133 160218 169843 173157 175289 198218 206955 215518 230658 232064 238517 245030 256597 261179 267195 271992 283073 292692 313088 316897 319415 325118 334917 348261 350814 358058 361301 364734 366344 374623 375676 378525 382209 387527 390714 394029 399876 403856 405821 415367 423493 437614 469807 483016 484512 487169 489903 494007 503192 508712 513397 515387 518094
		if (nextline == null)
			return null;
		String[] l = nextline.trim().split("\\s+");
		String moleName = l[0];
		long size = Long.parseLong(l[1]);
		int somastrand = Integer.parseInt(l[2]);
		int strand;
		switch (somastrand) {
			case 0:
				strand = -1;
				break;
			case 1:
				strand = 1;
				break;
			default:
				strand = 0;
				break;
		}
		int subrefstart = Integer.parseInt(l[3]);
		int subrefstop = Integer.parseInt(l[4]);
		Cigar cigar = new Cigar();
		proceedNextLine(); // Something to parse
		l = nextline.split("\\s+");
		double mappedscore = Double.parseDouble(l[1]);
		double confidence = Double.parseDouble(l[1]);
		proceedNextLine(); // Cigar string on reference to parse
		String refAlignment = nextline;
		proceedNextLine(); // Cigar string on molecule to parse
		String queryAlignment = nextline;
		proceedNextLine(); // Molecule information
		String[] ref = refAlignment.split(";");
		String[] query = queryAlignment.split(";");
		if (ref.length != query.length) {
			System.err.println("Warning, mis-match on cigar string check");
			cigar = null;
		} else {
			cigar.append('M');
			for (int i = 0; i < ref.length; i++) {
				for (int j = 0; j < ref[i].trim().split("\\s+").length - 1; j++)
					cigar.append('D');
				for (int j = 0; j < query[i].trim().split("\\s+").length - 1; j++)
					cigar.append('I');
				cigar.append('M');
			}
		}
		// This refer to the segment used by soma-v2
		// Last item in soma-v2 result may contain error
		int totalSegment = nextline.split("\\s+").length;

		boolean fixDataCut = false;
		DataNode data = new DataNode();
		if (fragmentInfo != null)
			if (fragmentInfo.containsKey(moleName)) {
				data = fragmentInfo.get(moleName);
				totalSegment = data.getTotalSegment() - 2;
				fixDataCut = true;
			} else {
				System.err.println("Error, data file does not contain molecule: " + moleName);
				System.err.println("Resume.");
			}
		if (data.name == null)
			data.name = moleName;

		int subfragstart = -1;
		int subfragstop = -1;
		if (strand == 1) {
			subfragstart = 0;
			subfragstop = totalSegment - 1;
		} else if (strand == -1) {
			subfragstart = totalSegment - 1;
			subfragstop = 0;
		}
		if (fixDataCut) {
			subfragstart++;
			subfragstop++;
		}
		proceedNextLine();

		GenomicPosNode pos = new GenomicPosNode("Dummy", -1, -1);
		if (optrefmap != null) {
			if (optrefmap.size() == 1)
				pos = optrefmap.values().iterator().next().getGenomicPos(subrefstart, subrefstop, true);
			else
				System.err.println("Error, reference file can only have on reference");
		}
		return new OptMapResultNode(data, pos, strand, subrefstart, subrefstop, subfragstart, subfragstop, cigar, mappedscore, confidence);
	}

	public OptMapResultNode parseTwinPSLResult() throws IOException {
		// 970569 0 0 970569 0 0 0 0 -- 256 970569 0 970569 ./Reference/Hsapien_twin.bin 3088266716 1805011248 1805981817 1 970569, 0, 1282284899,
		if (nextline == null)
			return null;
		String[] l = nextline.trim().split("\\s+");

		String strandInfo = l[8];
		String moleName = l[9];
		long size = Long.parseLong(l[10]);
		long start = Long.parseLong(l[15]);
		long stop = Long.parseLong(l[16]);
		int strand;
		switch (strandInfo) {
			case "--":
				strand = -1;
				break;
			case "+-":
				strand = 1;
				break;
			default:
				strand = 0;
				break;
		}
		// int subrefstart = Integer.parseInt(l[3]);
		// int subrefstop = Integer.parseInt(l[4]);
		// Cigar cigar = new Cigar();
		// proceedNextLine(); // Something to parse
		// proceedNextLine(); // Cigar string on reference to parse
		// String refAlignment = nextline;
		// proceedNextLine(); // Cigar string on molecule to parse
		// String queryAlignment = nextline;
		// proceedNextLine(); // Molecule information
		// String[] ref = refAlignment.split(";");
		// String[] query = queryAlignment.split(";");
		// if (ref.length != query.length) {
		// System.err.println("Warning, mis-match on cigar string check");
		// cigar = null;
		// }
		// else {
		// cigar.append('M');
		// for (int i = 0; i < ref.length; i++) {
		// for (int j = 0; j < ref[i].trim().split("\\s+").length; j++)
		// cigar.append('D');
		// for (int j = 0; j < query[i].trim().split("\\s+").length; j++)
		// cigar.append('I');
		// cigar.append('M');
		// }
		// }
		// int totalSegment = nextline.split("\\s+").length;
		// int subfragstart = -1;
		// int subfragstop = -1;
		// if (strand == 1) {
		// subfragstart = 0;
		// subfragstop = totalSegment - 1;
		// }
		// else
		// if (strand == -1) {
		// subfragstart = totalSegment - 1;
		// subfragstop = 0;
		// }
		//
		// proceedNextLine();

		double mappedscore = 1;
		double confidence = 1;
		DataNode data = new DataNode();
		if (fragmentInfo != null)
			if (fragmentInfo.containsKey(moleName))
				data = fragmentInfo.get(moleName);
			else {
				System.err.println("Error, data file does not contain molecule: " + moleName);
				System.err.println("Resume.");
			}
		if (data.name == null)
			data.name = moleName;

		GenomicPosNode pos = new GenomicPosNode("Dummy", -1, -1);
		if (optrefmap != null) {
			if (optrefmap.size() == 1)
				pos = new GenomicPosNode(optrefmap.values().iterator().next().name, start, stop);
			else
				System.err.println("Error, reference file can only have on reference");
		}

		proceedNextLine();
		return new OptMapResultNode(data, pos, strand, -1, -1, -1, -1, null, mappedscore, confidence);
	}

	public OptMapResultNode parseMalignerALNResult() throws IOException {
		// query_map	ref_map	is_forward	query_start	query_end	ref_start	ref_end	query_start_bp	query_end_bp	ref_start_bp	ref_end_bp	num_matched_chunks	query_misses	ref_misses	query_miss_rate	ref_miss_rate	total_score	total_rescaled_score	m_score	p_val	sizing_score	sizing_score_rescaled	query_scaling_factor	num_interior_chunks	score_per_inner_chunk	chunk_string	score_string
		// QUERY	REF	R	0	31	303	351	0	162996	1507124	1671422	30	1	18	0.0333333	0.382979	126.459	127.048	-9.79132	0	54.4589	55.0482	1.00886	28	4.53744	30,31,1374,303,304,1462;29,30,4497,304,307,4650;28,29,9158,307,310,9043;27,28,1304,310,311,1978;26,27,2557,311,312,3619;25,26,6028,312,314,6328;24,25,2519,314,316,4496;23,24,5513,316,317,6318;22,23,1330,317,318,1782;21,22,4385,318,319,3819;20,21,10027,319,320,9971;19,20,4204,320,321,3338;18,19,1415,321,322,1384;17,18,9547,322,325,8876;16,17,3219,325,326,3108;15,16,6170,326,328,6738;14,15,2989,328,329,2912;13,14,1362,329,330,1698;12,13,4599,330,332,4117;11,12,4348,332,335,4453;9,11,7652,335,337,6923;8,9,2673,337,338,2172;7,8,2677,338,339,2921;6,7,2496,339,340,1073;5,6,14309,340,341,15135;4,5,2727,341,342,1641;3,4,13759,342,345,12862;2,3,1874,345,346,1617;1,2,14872,346,348,15253;0,1,14841,348,351,14361;	(0, 0, 0);(0, 6, 0.093636);(0, 6, 0.0529);(0, 0, 1.8171);(0, 0, 4.51138);(0, 3, 0.36);(0, 3, 15.6341);(0, 0, 2.5921);(0, 0, 0.817216);(0, 0, 1.28142);(0, 0, 0.012544);(0, 0, 2.99982);(0, 0, 0.003844);(0, 6, 1.80096);(0, 0, 0.049284);(0, 3, 1.2905);(0, 0, 0.023716);(0, 0, 0.451584);(0, 3, 0.929296);(0, 6, 0.0441);(18, 3, 2.12576);(0, 0, 1.004);(0, 0, 0.238144);(0, 0, 8.09972);(0, 0, 1.19139);(0, 0, 4.71758);(0, 6, 1.94548);(0, 0, 0.264196);(0, 3, 0.249574);(0, 6, 0.446862);
		
		if (nextline == null)
			return null;
		String[] l = nextline.trim().split("\\s+");
		String queryName = l[0];
		String refName = l[1];
		DataNode data = new DataNode(queryName, 0, null);
		if (fragmentInfo != null) {
			if (fragmentInfo.containsKey(queryName)) {
				data = fragmentInfo.get(queryName);
			} else {
				System.err.println("Error, data file does not contain molecule: " + data);
				System.err.println("Resume.");
			}
		}
//		if (optrefmap != null) {
//			if (optrefmap.containsKey(mappedref)) {
//				subrefstart = this.getSubRefStart(optrefmap.get(mappedref), refmapstart);
//				subrefstop = this.getSubRefStop(optrefmap.get(mappedref), refmapstop);
//			} else {
//				System.err.println("Error, reference file does not contain: " + mappedref);
//				System.err.println("Resume.");
//			}
//		}

		int strand = l[2].equals("F")?1:l[2].equals("R")?-1:0;
		
		int queryStart = Integer.parseInt(l[3]);
		int queryStop = Integer.parseInt(l[4]) - 1;
		if (strand == -1) {
			int tmp = queryStart;
			queryStart = queryStop;
			queryStop = tmp;
		}
			
		int refStart = Integer.parseInt(l[5]);
		int refStop = Integer.parseInt(l[6]) - 1;
		
		// The ref start and ref stop bp seem couldn't match the reference position...
		long refStartBp = Long.parseLong(l[9]);
		long refStopBp = Long.parseLong(l[10]);

		GenomicPosNode region = new GenomicPosNode(refName, refStartBp, refStopBp);
		double totalScore = Double.parseDouble(l[16]);
		String[] chunkStrings = l[25].split(";");
		StringBuilder precigar = new StringBuilder();
		precigar.append('M');
		for (String chunk : chunkStrings) {
			String[] subchunks = chunk.split(",");
			int subQueryStart = Integer.parseInt(subchunks[0]);
			int subQueryStop = Integer.parseInt(subchunks[1]) - 1;
			// subchunks[2]
			int subRefStart = Integer.parseInt(subchunks[3]);
			int subRefStop = Integer.parseInt(subchunks[4]) - 1;
			// subchunks[5]
			int insertion = subQueryStop - subQueryStart;
			int deletion = subRefStop - subRefStart;
			for (int j = 0; j < insertion; j++)
				precigar.append('I');
			for (int j = 0; j < deletion; j++)
				precigar.append('D');
			precigar.append('M');
		}
		Cigar cigar = new Cigar(precigar.toString());
		proceedNextLine();
		return new OptMapResultNode(data, region, strand, refStart, refStop, queryStart, queryStop, cigar, totalScore, -1);
	}

	public LinkedHashMap<String, OptMapResultNode> readAllData() throws IOException {
		LinkedHashMap<String, OptMapResultNode> resultmap = new LinkedHashMap<String, OptMapResultNode>();
		OptMapResultNode result;
		do {
			result = read();
			if (result == null)
				break;
			else {
				if (resultmap.containsKey(result.parentFrag.name)) // **** Only the first time appearing will be imported. For multiple mapping, use readAllDataInList
				{
					if (resultmap.get(result.parentFrag.name).confidence < result.confidence)
						resultmap.put(result.parentFrag.name, result);
				} else
					resultmap.put(result.parentFrag.name, result);
			}
		} while (result != null);
		return resultmap;
	}

	public LinkedHashMap<String, List<OptMapResultNode>> readAllDataInList() throws IOException {
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = new LinkedHashMap<String, List<OptMapResultNode>>();
		List<OptMapResultNode> resultlist;
		while ((resultlist = readNextList()) != null) {
			String name = resultlist.get(0).parentFrag.name;
			if (resultlistmap.containsKey(name))
				System.err.println("Warning: more than one set of alignments are named as \"" + name + "\". Please make sure (1) two data entires should not have the same name, and (2) alignment result should be sorted by name.");
			resultlistmap.put(name, resultlist);
		}
		return resultlistmap;
	}

	public static LinkedHashMap<String, OptMapResultNode> readAllData(String filename) throws IOException {
		return readAllData(filename, -1);
	}

	public static LinkedHashMap<String, OptMapResultNode> readAllData(String filename, int format) throws IOException {
		OptMapResultReader omdr = new OptMapResultReader(filename, format);
		LinkedHashMap<String, OptMapResultNode> resultmap = omdr.readAllData();
		omdr.close();
		return resultmap;
	}

	public static LinkedHashMap<String, OptMapResultNode> readAllData(OptionSet options) throws IOException {
		OptMapResultReader omdr = new OptMapResultReader(options);
		LinkedHashMap<String, OptMapResultNode> resultmap = omdr.readAllData();
		omdr.close();
		return resultmap;
	}

	public static LinkedHashMap<String, List<OptMapResultNode>> readAllDataInList(String filename) throws IOException {
		return readAllDataInList(filename, -1);
	}

	public static LinkedHashMap<String, List<OptMapResultNode>> readAllDataInList(String filename, int format) throws IOException {
		OptMapResultReader omdr = new OptMapResultReader(filename, format);
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = omdr.readAllDataInList();
		omdr.close();
		return resultlistmap;
	}

	public static LinkedHashMap<String, List<OptMapResultNode>> readAllDataInList(OptionSet options) throws IOException {
		OptMapResultReader omdr = new OptMapResultReader(options);
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap = omdr.readAllDataInList();
		omdr.close();
		return resultlistmap;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Result Reader Options", level);
		parser.accepts("optresin", "Input alignment result file").withRequiredArg().ofType(String.class).required();
		parser.accepts("optresinformat", ResultFormat.getFormatHelp()).withRequiredArg().ofType(Integer.class).defaultsTo(-1);
	}

	public static void assignOptions(ExtendOptionParser parser) {
		assignOptions(parser, 1);
	}

}
