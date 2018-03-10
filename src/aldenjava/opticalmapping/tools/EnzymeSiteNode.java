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


package aldenjava.opticalmapping.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import joptsimple.OptionSet;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class EnzymeSiteNode {

	public static final LinkedHashMap<String, EnzymeSiteNode> ENZYMEMAP = new LinkedHashMap<String, EnzymeSiteNode>() {
		private static final long serialVersionUID = 1L;
		{
			put("BspQI", new EnzymeSiteNode("BspQI", "GCTCTTC", 8));
			put("BbvCI", new EnzymeSiteNode("BbvCI", "CCTCAGC", 2));
			put("AlwI", new EnzymeSiteNode("AlwI", "GGATC", 9));
			put("BsmAI", new EnzymeSiteNode("BsmAI", "GTCTC", 6));
			put("BstNBI", new EnzymeSiteNode("BstNBI", "GAGTC", 9));
			put("BsmI", new EnzymeSiteNode("BsmI", "GAATGC", 0));
			put("BsrDI", new EnzymeSiteNode("BsrDI", "GCAATG", 0));
			put("BssSI", new EnzymeSiteNode("BssSI", "CACGAG", 1));
			put("BtsI", new EnzymeSiteNode("BtsI", "GCAGTG", 0));
		}
	};

	public static String getSupportedEnzymes() {
		return ENZYMEMAP.keySet().toString();
	}
	public final String name;
	public final String seq;
	public final int pos; // 1 based

	public EnzymeSiteNode(String name, String seq, int pos) {
		this.name = name;
		this.seq = seq;
		this.pos = pos;
	}

	public EnzymeSiteNode(String name, String seq) {
		this.name = name;
		if (seq.contains("/")) {
			this.pos = seq.indexOf("/");
			this.seq = seq.replaceAll("/", "");
		} else {
			this.seq = seq;
			this.pos = 0;
		}
	}
	private String revComplement(String dna) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < dna.length(); i++)
		{
			char c = dna.charAt(dna.length() - i - 1);
			char newc;
			switch (c)
			{
				case 'A': case 'a':
					newc = 't';
					break;
				case 'T': case 't':
					newc = 'a';
					break;
				case 'C': case 'c':
					newc = 'g';
					break;
				case 'G': case 'g':
					newc = 'c';
					break;
				default:
					newc = 'n';
					break;
			}
			s.append(newc);
		}
		return s.toString();
	}

	public String getForwardSeq() {
		return seq;
	}
	public String getReverseSeq() {
		return revComplement(seq);
	}
	public boolean isPalindromic() {
		return (getForwardSeq().equalsIgnoreCase(getReverseSeq()));
	}
	public static EnzymeSiteNode getEnzyme(String enzymeName) throws EnzymeNotFoundException {
		if (!EnzymeSiteNode.ENZYMEMAP.containsKey(enzymeName))
			throw new EnzymeNotFoundException();
		return EnzymeSiteNode.ENZYMEMAP.get(enzymeName);
	}

	public static List<EnzymeSiteNode> getEnzyme(List<String> enzymeNameList) {
		List<EnzymeSiteNode> enzymeList = new ArrayList<EnzymeSiteNode>();
		for (String enzymeName : enzymeNameList)
			try {
				enzymeList.add(getEnzyme(enzymeName));
			} catch (EnzymeNotFoundException e)	{
				System.err.println("Enzyme " + enzymeName + " is not found. Note that enzyme names are case sensitive.");
			}
		return enzymeList;
	}

	public static List<EnzymeSiteNode> getEnzyme(OptionSet options) {
		if (options.has("enzyme"))
			return getEnzyme((List<String>) options.valuesOf("enzyme"));
		else
			return new ArrayList<EnzymeSiteNode>();
	}

	public static List<EnzymeSiteNode> getEnzymeSequence(List<String> enzymeSequenceList) {

		List<EnzymeSiteNode> enzymeList = new ArrayList<EnzymeSiteNode>();
		for (String enzymeSequence : enzymeSequenceList) {
			String u = enzymeSequence.toUpperCase();
			if (u.matches("[^ATCG]")) {
				System.err.println("Do not accept characters other than ATCG, " + enzymeSequence);
				continue;
			}
			enzymeList.add(new EnzymeSiteNode("DummyEnzyme", u));
		}
		return enzymeList;
	}

	public static List<EnzymeSiteNode> getEnzymeSequence(OptionSet options) {
		if (options.has("enzymestring"))
			return getEnzymeSequence((List<String>) options.valuesOf("enzymestring"));
		else
			return new ArrayList<EnzymeSiteNode>();
	}

	public static void assignOptions(ExtendOptionParser parser) {
		parser.addHeader("Enzyme Input Options", 1);
		parser.accepts("enzyme", "Built-in enzymes " + getSupportedEnzymes() + " (Support multiple enzymes input)").withRequiredArg().ofType(String.class);
		parser.accepts("enzymestring", "Enzyme sequence (e.g. GCTCTTC)").withRequiredArg().ofType(String.class);
	}
}

class EnzymeNotFoundException extends RuntimeException {
	
}