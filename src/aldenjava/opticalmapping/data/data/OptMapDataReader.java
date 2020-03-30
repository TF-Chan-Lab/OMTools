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


package aldenjava.opticalmapping.data.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import joptsimple.OptionSet;

import org.apache.commons.lang.ArrayUtils;

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.DataFormat;
import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

/**
 * The optical mapping data reader. It supports various input format
 * 
 * @author Alden
 *
 * @see DataFormat
 */
public class OptMapDataReader extends OMReader<DataNode> {

	/**
	 * The data format of the input file
	 */
	private DataFormat dformat;
	
	private boolean singleRecord;
	private String filename;
	private double bnxSNR = 3.0;
	private XMLStreamReader xmlReader;

	public OptMapDataReader(OptionSet options) throws IOException {
		this((String) options.valueOf("optmapin"), (int) options.valueOf("optmapinformat"), (double) options.valueOf("bnxsnr"));
	}

	public OptMapDataReader(String filename) throws IOException {
		this(filename, -1);
	}

	public OptMapDataReader(String filename, int format) throws IOException {
		this(filename, DataFormat.lookup(filename, format));
	}

	public OptMapDataReader(String filename, int format, double bnxSNR) throws IOException {
		this(filename, DataFormat.lookup(filename, format));
		this.bnxSNR = bnxSNR;
	}

	public OptMapDataReader(String filename, DataFormat dformat) throws IOException {
		super(filename);
		if (dformat == null)
			throw new NullPointerException("dformat");
		this.dformat = dformat;
		this.filename = filename;
		if (dformat == DataFormat.XML)
			createXMLReader();
	}

	public OptMapDataReader(InputStream stream, DataFormat dformat) throws IOException {
		super(stream);
		if (dformat == null)
			throw new NullPointerException("dformat");
		this.dformat = dformat;
		this.filename = "";
		if (dformat == DataFormat.XML)
			createXMLReader();

	}

	@Override
	public DataNode read() throws IOException {
		if (nextline == null)
			return null;
		else {
			switch (dformat) {
				case REF:
				case SILICO:
					return parseREF();
				case FA01:
					return parseFA01();
				case SPOTS:
					return parseSPOT();
				case OPT:
					return parseOPT();
				case DATA:
					return parseDATA();
				case SDATA:
					return parseSDATA();
				case BNX:
					return parseBNX();
				case CMAP:
					return parseCMAP();
				case XML:
					return parseXML();
				case VALDATA:
					return parseVALDATA();
				case MAPS:
					return parseMAPS();
				default:
					return null;

			}
		}
	}

	private DataNode parseREF() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\t");
		String name = l[0];
		long size = Long.parseLong(l[1]);
		long refplen = Integer.parseInt(l[2]);
		proceedNextLine();
		if (refplen == 0)
			return new DataNode(name, size);
		if (nextline != null) {
			List<Long> refp = new ArrayList<Long>();

			String[] line = nextline.trim().split("\t");
			for (int i = 0; i < line.length; i++)
				refp.add(Long.parseLong(line[i]));
			if (refplen != refp.size())
				System.err.println("Warning: " + name + "\nNumber of labels stated (" + refplen + ") does not match true number of labels (" + refp.size() + ").");
			proceedNextLine();

			return new DataNode(name, size, ArrayUtils.toPrimitive(refp.toArray(new Long[refp.size()])));
		} else {
			System.err.println("Warning: Incomplete record found: " + name);
			return null;
		}
	}

	private DataNode parseFA01() throws IOException {
		if (nextline == null)
			return null;
		String name = nextline.substring(1);
		StringBuilder zeroOneString = new StringBuilder();
		String s;
		while ((s = br.readLine()) != null) {
			if (s.startsWith(">")) {
				nextline = s;
				break;
			} else
				zeroOneString = zeroOneString.append(s);
		}

		if (zeroOneString.length() == 0)
			return null;
		else {
			long ref_size = zeroOneString.length();
			long zero = 0;
			List<Long> refl = new ArrayList<Long>();
			List<Long> refp = new ArrayList<Long>();
			for (long i = 0; i < ref_size; i++) {
				if (zeroOneString.charAt((int) i) == '0')
					zero++;
				else if (zeroOneString.charAt((int) i) == '1') {
					refl.add(zero);
					refp.add(i);
					zero = 0;
				}
			}
			refl.add(zero);
			return new DataNode(name, zeroOneString.length(), ArrayUtils.toPrimitive(refp.toArray(new Long[refp.size()])));
		}
	}

	private DataNode parseSPOT() throws IOException {
		if (nextline == null)
			return null;
		if (singleRecord) {
			System.err.println("There is only one reference in spot file.");
			return null;
		}
		singleRecord = true;
		String s;
		long refsize = 0;
		br.reset();
		while ((s = br.readLine()).startsWith("#")) {
			if (s.contains("Reference Size")) {
				String[] l = s.split("\t");
				refsize = Long.parseLong(l[l.length - 1]);
			}
		}
		while (s.startsWith("NickID"))
			s = br.readLine();
		List<Long> refl = new ArrayList<Long>();
		List<Long> refp = new ArrayList<Long>();

		while (s != null) {
			String[] line = s.split("\\s+");
			refp.add(Long.parseLong(line[line.length - 1]));
			s = br.readLine();
		}
		br.close();
		refl.add(refp.get(0));
		for (int i = 0; i < refp.size() - 1; i++)
			refl.add(refp.get(i + 1) - refp.get(i));
		refl.add(refsize - refp.get(refp.size() - 1));

		return new DataNode(new File(filename).getName(), refsize, ArrayUtils.toPrimitive(refp.toArray(new Long[refp.size()])));
	}

	private DataNode parseOPT() throws IOException {
		if (nextline == null)
			return null;
		if (singleRecord) {
			System.err.println("There is only one reference in opt file.");
			return null;
		}
		singleRecord = true;

		List<Long> refl = new ArrayList<Long>();
		String s = br.readLine();
		while (s != null) {
			String[] line = s.split("\\s+");
			refl.add((long) (Double.parseDouble(line[0]) * 1000));
			s = br.readLine();
		}
		return new DataNode(new File(filename).getName(), ArrayUtils.toPrimitive(refl.toArray(new Long[refl.size()])));
	}

	private DataNode parseDATA() throws IOException {
		String s = this.nextline;
		s = s.trim();
		String[] l = s.split("\t");
		String name = l[0];
		long size = Long.parseLong(l[1]);
		int totalSegment = Integer.parseInt(l[2]);
		long[] refl = DataNode.parseReflInString(l[3], ";");

		// Validation
		if (refl.length != totalSegment) {
			System.err.println("Warning: Inconsistent total segments and fragment details");
		}
		long mysize = 0;
		for (int i = 0; i < refl.length - 1; i++) {
			mysize += refl[i] + 1; // need +1 here?
		}
		mysize += refl[refl.length - 1];
		if (size != mysize) {
			System.err.println("Warning: Inconsistent size and fragment details");
		}
		proceedNextLine();
		return new DataNode(name, refl);
	}

	private DataNode parseSDATA() throws IOException {
		String s = this.nextline;
		s = s.trim();
		String[] l = s.split("\t");
		int count = 0;
		String name = l[count++];
		String ref = l[count++];
		int simuStrand = (l[count].equalsIgnoreCase("forward") || l[count].equalsIgnoreCase("+")) ? 1 : (l[count].equalsIgnoreCase("reverse") || l[count].equalsIgnoreCase("-")) ? -1 : 0;
		count++;
		long genomestart = Long.parseLong(l[count++]);
		long genomestop = Long.parseLong(l[count++]);
		List<VirtualSignal> vsList = null;
		if (l.length == 9) {// New sdata with virtual signal info
			String vsstring = l[count++];
			vsList = new ArrayList<>();
			if (!vsstring.isEmpty()) {
				String[] prevses = vsstring.split(";");
				for (String prevs : prevses) {
					vsList.add(new VirtualSignal(prevs));
				}
			}
		}
		long size = Long.parseLong(l[count++]);
		int totalSegment = Integer.parseInt(l[count++]);
		long[] refl = DataNode.parseReflInString(l[count++], ";");

		// Validation
		if (refl.length != totalSegment) {
			System.err.println("Warning: Inconsistent total segments and fragment details");
		}
		long mysize = 0;
		for (int i = 0; i < refl.length - 1; i++) {
			mysize += refl[i] + 1; // need +1 here?
		}
		mysize += refl[refl.length - 1];
		if (size != mysize) {
			System.out.println(size + "_" + mysize);
			System.err.println("Warning: Inconsistent size and fragment details");
		}
		proceedNextLine();
		DataNode data = new DataNode(name, refl);
		if (SimulationInfo.checkInfoValid(ref, genomestart, genomestop, simuStrand))
			data.importSimulationInfo(new SimulationInfo(new GenomicPosNode(ref, genomestart, genomestop), simuStrand, vsList));
		return data;

	}

	private DataNode parseBNX() throws IOException {

		// Currently unused information, Only support one (single-color) channel

		// int labelChannel;
		// double avgIntensity;
		// double snr;
		// int noOfLables;
		// String originalMoleculeID;
		// int scanNumber;
		// int scanDirection;
		// String chipID;
		// int flowCell;
		// int runID;
		// int globalScanNumber;

		// BNX File Version 1.0
		// #0h LabelChannel MoleculeId Length AvgIntensity SNR NumberofLabels OriginalMoleculeId ScanNumber ScanDirection ChipId Flowcell
		// #0f int int float float float int int int int string int
		// #1h LabelChannel LabelPositions[N]
		// #1f int float
		// #2h LabelChannel LabelPositions[N]
		// #2h int float
		// #Qh QualityScoreID QualityScores[N]
		// #Qf str float
		// BNX File Version 1.2
		// #0h LabelChannel MoleculeId Length AvgIntensity SNR NumberofLabels OriginalMoleculeId ScanNumber ScanDirection ChipId Flowcell RunId GlobalScanNumber
		// #0f int int float float float int int int int string int int int
		// #1h LabelChannel LabelPositions[N]
		// #1f int float
		// #2h LabelChannel LabelPositions[N]
		// #2h int float
		// #Qh QualityScoreID QualityScores[N]
		// #Qf str float
		/* BNX File Version 1.3 (According to the specification sheet of V1.3: GlobalScanNumber -- Not present in all BNX files)
		   #0h LabelChannel MoleculeId Length AvgIntensity SNR NumberofLabels OriginalMoleculeId ScanNumber ScanDirection ChipId Flowcell RunId Column StartFOV StartX StartY EndFOV EndX EndY
		   #0f int int int float float int int int int string int int int int int int int int int
		   #1h LabelChannel LabelPosition[N]
		   #1f int int
		   #Qh QualityScoreID QualityScores[N]
		   #Qf string float	
		*/

		String name = "";
		long size = -1;

		long[] refp = null;
		double[] snr = null;
		double[] intensity = null;
		
		// BNX version 1.0 and above (common objects)
		double avgIntensity = -1;
		double moleculeSNR = -1;
		int numberOfLabels = -1;
		int originalMoleculeID = -1;		
		int scanNumber = -1;
		int scanDirection = -1;
		String chipID = "";
		int flowCell = -1;
		boolean hasScanNumber = false;


		// BNX version 1.2 and above
		int runID = -1;
		int globalScanNumber = -1;		
		boolean hasGlobalScanNumber = false;

		// global check 
		boolean gotNameSizeInfo = false;
		boolean gotDetailInfo = false;
		boolean gotSNRInfo = false;
		boolean gotIntensityInfo = false;

		do {
			String s = this.nextline.trim();
			String[] l = s.split("\t");
			switch (l[0]) {
				case "0":
					name = l[1];
					size = (long) Double.parseDouble(l[2]);
					// v1.0 and above (global)
					if (l.length >= 11) {
						avgIntensity = Double.parseDouble(l[3]);
						moleculeSNR = Double.parseDouble(l[4]);
						numberOfLabels = Integer.parseInt(l[5]);
						originalMoleculeID = Integer.parseInt(l[6]);
						scanNumber = Integer.parseInt(l[7]);
						scanDirection = Integer.parseInt(l[8]);
						chipID = l[9];
						flowCell = Integer.parseInt(l[10]);
						hasScanNumber = true;
						hasGlobalScanNumber = false;
					}
					// v1.2 (specific)
					if (l.length == 13) {
						runID = Integer.parseInt(l[11]);
						globalScanNumber = Integer.parseInt(l[12]);
						hasScanNumber = true;
						hasGlobalScanNumber = true;
					}
					// v1.3 (specific)
					if (l.length == 19){//GlobalScanNumber not present
						runID = Integer.parseInt(l[11]);
						hasScanNumber = true;
					}
					if (l.length == 20){//GlobalScanNumber present
						runID = Integer.parseInt(l[11]);
						globalScanNumber = Integer.parseInt(l[19]);
						hasScanNumber = true;
						hasGlobalScanNumber = true;
					}					
					gotNameSizeInfo = true;
					break;
				case "1":
					if (hasScanNumber)
						if (l.length - 2 != numberOfLabels)
							System.err.println("Warning: Inconsistent number of labels for \"" + name + "\"");
					
					refp = new long[l.length - 2]; // last element should be the size of molecule
					for (int i = 1; i < l.length - 1; i++)
						refp[i - 1] = (long) Double.parseDouble(l[i]);
					gotDetailInfo = true;
					break;
				case "QX01":
				case "QX11":
					if (l.length - 1 != refp.length)
						System.err.println("Warning: Inconsistent number of labels and snr for \"" + name + "\"");
					snr = new double[l.length - 1];
					for (int i = 1; i < l.length; i++)
						snr[i - 1] = Double.parseDouble(l[i]);
					gotSNRInfo = true;
					break;
				case "QX02":
				case "QX12":
					if (l.length - 1 != refp.length)
						System.err.println("Warning: Inconsistent number of labels and intensity for \"" + name + "\"");
					intensity = new double[l.length - 1];
					for (int i = 1; i < l.length; i++)
						intensity[i - 1] = Double.parseDouble(l[i]);
					gotIntensityInfo = true;
					break;
				default:

			}
			proceedNextLine();
		} while ((this.nextline != null)
				&& (this.nextline.startsWith("1") || this.nextline.startsWith("QX")));
		if (gotNameSizeInfo && gotDetailInfo && gotSNRInfo && gotIntensityInfo) {
			// bnx version 1.0 or above
			List<Long> refpList = new ArrayList<>();
			List<Double> snrList = new ArrayList<>();
			List<Double> intensityList = new ArrayList<>();
			for (int i = 0; i < refp.length; i++)
				if (snr[i] >= bnxSNR) {
					refpList.add(refp[i]);
					snrList.add(snr[i]);
					intensityList.add(intensity[i]);
				}
			refp = ArrayUtils.toPrimitive(refpList.toArray(new Long[refpList.size()]));
			snr = ArrayUtils.toPrimitive(snrList.toArray(new Double[snrList.size()]));
			intensity = ArrayUtils.toPrimitive(intensityList.toArray(new Double[intensityList.size()]));
			BnxDataNode data;			
			if (hasGlobalScanNumber)
				data = new BnxDataNode(name, size, refp, snr, intensity, avgIntensity, moleculeSNR, originalMoleculeID, scanNumber, scanDirection, chipID, flowCell, runID, globalScanNumber);
			else
				if (hasScanNumber)
					data = new BnxDataNode(name, size, refp, snr, intensity, avgIntensity, moleculeSNR, originalMoleculeID, scanNumber, scanDirection, chipID, flowCell);
				else
					data = new BnxDataNode(name, size, refp, snr, intensity);
			return data;
		} else 
			if (gotNameSizeInfo && gotDetailInfo) {
				// Old bnx version
				DataNode data = new DataNode(name, size, refp);
				return data;
			} else {
				System.err.println("Warning: incomplete record is found.");
				return null;
			}
	}

	private DataNode parseCMAP() throws IOException {
		String s = this.nextline;
		s = s.trim();
		if (s.isEmpty())
			return null;
		String[] l = s.split("\\s+");
		String name = l[0];
		long size = (long) (Double.parseDouble(l[1]));
		int totalSignal = Integer.parseInt(l[2]);
		// l[3] //site x

		long[] refp = new long[totalSignal];
		for (int i = 0; i < totalSignal + 1; i++) {
			s = this.nextline;
			s = s.trim();
			l = s.split("\\s+");
			int labelchannel = Integer.parseInt(l[4]); // not used till supporting double color
			if (labelchannel != 0)
				refp[i] = (long) Double.parseDouble(l[5]);
			proceedNextLine();
		}

		return new DataNode(name, size, refp);
	}

	private DataNode parseVALDATA() throws IOException {
		if (nextline == null)
			return null;
		String s = this.nextline;
		String name = s;
		proceedNextLine();
		s = this.nextline;
		String[] l = s.trim().split("\\s+");
		String enzName = l[0];
		String enzAcr = l[1];
		List<Long> refl = new ArrayList<>();
		for (int i = 2; i < l.length; i++)
			refl.add((long) (Double.parseDouble(l[i]) * 1000));
		proceedNextLine();

		return new DataNode(name, ArrayUtils.toPrimitive(refl.toArray(new Long[refl.size()])));
	}
	
	private DataNode parseMAPS() throws IOException {
		String s = this.nextline;
		s = s.trim();
		String[] l = s.split("\t");
		String name = l[0];
		long size = Long.parseLong(l[1]);
		int totalSegment = Integer.parseInt(l[2]);
		// Validation
		if (l.length != totalSegment + 3) {
			System.err.println("Warning: Inconsistent total segments and fragment details");
		}
		long[] refl = new long[totalSegment];
		for (int i = 0; i < totalSegment; i++) {
			refl[i] = Long.parseLong(l[i + 3]);
			if (i != totalSegment - 1)
				refl[i]--;
		}
		
		long mysize = 0;
		for (int i = 0; i < refl.length - 1; i++) {
			mysize += refl[i] + 1; //
		}
		mysize += refl[refl.length - 1];
		if (size != mysize) {
			System.err.println("Warning: Inconsistent size and fragment details");
		}
		proceedNextLine();
		return new DataNode(name, refl);
	}


	private void createXMLReader() throws IOException {
		try {
			xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(super.br);
		} catch (XMLStreamException | FactoryConfigurationError e) {
			throw new IOException("XML parse exception");
		}
	}

	private DataNode parseXML() throws IOException {

		List<Long> reflList = null;
		long[] refl = null;
		String id = null;
		try {
			while (xmlReader.hasNext()) {

				int event = xmlReader.next();
				switch (event) {
					case XMLEvent.START_ELEMENT:
						switch (xmlReader.getLocalName()) {
							case "RESTRICTION_MAP":
								;
								id = xmlReader.getAttributeValue("", "ID");
								break;
							case "FRAGMENTS":
								reflList = new ArrayList<Long>();
								break;
							case "F":
								reflList.add(Long.parseLong(xmlReader.getAttributeValue("", "S")));
								break;
						}
						break;
					case XMLEvent.END_ELEMENT:
						switch (xmlReader.getLocalName()) {
							case "RESTRICTION_MAP":
								;
								return new DataNode(id, refl);
							case "FRAGMENTS":
								refl = ArrayUtils.toPrimitive(reflList.toArray(new Long[reflList.size()]));
								break;
							case "F":
								// Nothing to do
								break;
						}

						break;

					case XMLEvent.START_DOCUMENT:
						break;
					case XMLEvent.END_DOCUMENT:
						return null;

				}

			}

		} catch (XMLStreamException e) {
			throw new IOException("XML parse exception");
		}
		return null;
	}

	public LinkedHashMap<String, DataNode> readAllData() throws IOException {
		LinkedHashMap<String, DataNode> fragmentmap = new LinkedHashMap<String, DataNode>();
		DataNode fragment;
		do {
			fragment = read();
			if (fragment == null)
				break;
			else
				fragmentmap.put(fragment.name, fragment);
		} while (fragment != null);
		return fragmentmap;
	}

	public static LinkedHashMap<String, DataNode> readAllData(String filename) throws IOException {
		return readAllData(filename, -1);
	}

	public static LinkedHashMap<String, DataNode> readAllData(String filename, int format) throws IOException {
		OptMapDataReader omdr = new OptMapDataReader(filename, format);
		LinkedHashMap<String, DataNode> fragmentmap = omdr.readAllData();
		omdr.close();
		return fragmentmap;
	}

	public static LinkedHashMap<String, DataNode> readAllData(OptionSet options) throws IOException {
		OptMapDataReader omdr = new OptMapDataReader(options);
		LinkedHashMap<String, DataNode> fragmentmap = omdr.readAllData();
		omdr.close();
		return fragmentmap;
	}

	public static LinkedHashMap<Integer, LinkedHashMap<String, DataNode>> getLabelMap(OptionSet options) throws IOException {
		OptMapDataReader omdr = new OptMapDataReader(options);
		return getLabelMap(omdr);
	}

	public static LinkedHashMap<Integer, LinkedHashMap<String, DataNode>> getLabelMap(String filename, int format) throws IOException {
		OptMapDataReader omdr = new OptMapDataReader(filename, format);
		return getLabelMap(omdr);
	}

	private static LinkedHashMap<Integer, LinkedHashMap<String, DataNode>> getLabelMap(OptMapDataReader omdr) throws IOException {
		LinkedHashMap<Integer, LinkedHashMap<String, DataNode>> fragmentMapLabelMap = new LinkedHashMap<Integer, LinkedHashMap<String, DataNode>>();
		DataNode fragment;
		while ((fragment = omdr.read()) != null) {
			LinkedHashMap<String, DataNode> fragmentMap;
			if ((fragmentMap = fragmentMapLabelMap.get(fragment.getTotalSegment())) == null) {
				fragmentMap = new LinkedHashMap<String, DataNode>();
				fragmentMapLabelMap.put(fragment.getTotalSegment(), fragmentMap);
			}
			fragmentMap.put(fragment.name, fragment);
		}
		omdr.close();
		return fragmentMapLabelMap;

	}
	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("Data Reader Options", level);
		parser.accepts("optmapin", "Input optical map file").withRequiredArg().ofType(String.class).required();
		parser.accepts("optmapinformat", DataFormat.getFormatHelp()).withRequiredArg().ofType(Integer.class).defaultsTo(-1);
		parser.accepts("bnxsnr", "BNX SNR filter value").withRequiredArg().ofType(Double.class).defaultsTo(3.0);
	}

	public static void assignOptions(ExtendOptionParser parser) {
		assignOptions(parser, 1);
	}

	public static int countData(String filename) throws IOException {
		OptMapDataReader omdr = new OptMapDataReader(filename);
		int totalCount = 0;
		while (omdr.read() != null)
			totalCount++;
		omdr.close();
		return totalCount;
	}
}
