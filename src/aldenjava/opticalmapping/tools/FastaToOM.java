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


package aldenjava.opticalmapping.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.ReferenceWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.sequence.fasta.StreamFastaNode;
import aldenjava.sequence.fasta.StreamFastaReader;

/**
 * Performs an in silico digestion on DNA sequence.
 * (A bitwise operation needs to be implemented. Now it's string of '0' and '1' Try to make a byte to contain 8 bits, and put the bits into an array)
 * @author Alden 
 */
public class FastaToOM {

	private final List<EnzymeSiteNode> enzymeList;

	public FastaToOM(EnzymeSiteNode enzyme) {
		List<EnzymeSiteNode> enzymeList = new ArrayList<EnzymeSiteNode>();
		enzymeList.add(enzyme);
		this.enzymeList = enzymeList;
	}

	public FastaToOM(List<EnzymeSiteNode> enzymeList) {
		this.enzymeList = enzymeList;
	}

	private boolean[] virtualFragmentation(String seq, String resite, int enzymepos) {
		resite = resite.toLowerCase();
		seq = seq.toLowerCase();
		int index = 0;
		int lastindex = 0;
		boolean[] fragmentedSeq = new boolean[seq.length()];
		while ((index = seq.indexOf(resite, lastindex)) != -1) {
			int site = index + enzymepos; // this is 0-based rather than 1-based
			if (site >= 0 && site < fragmentedSeq.length)
				fragmentedSeq[site] = true;
			lastindex = index + 1;
		}
		return fragmentedSeq;
	}

	/**
	 * Merge multiple seqs using an OR-gate
	 * 
	 * @param seqs
	 * @return combined seq
	 */
	private boolean[] combinedSingleColor(boolean[]... seqs) {
		if (seqs.length == 0)
			throw new IllegalArgumentException("Number of seq = 0.");
		int length = seqs[0].length;
		for (boolean[] seq : seqs)
			if (seq.length != length)
				throw new IllegalArgumentException("Inconsistent seq length.");

		boolean[] newseq = new boolean[length];
		for (int i = 0; i < length; i++) {
			boolean hasSignal = false;
			for (int j = 0; j < seqs.length; j++)
				hasSignal |= seqs[j][i];
			newseq[i] = hasSignal;
		}
		return newseq;
	}

	public DataNode convertFasta(StreamFastaNode sfn) {
		if (enzymeList.isEmpty())
			return new DataNode(sfn.name, sfn.length(), new long[] {});
		boolean[][] forwardSeqs = new boolean[enzymeList.size()][];
		boolean[][] reverseSeqs = new boolean[enzymeList.size()][];
		int index = 0;
		for (EnzymeSiteNode enzyme : enzymeList) {
			forwardSeqs[index] = this.virtualFragmentation(sfn.seq, enzyme.getForwardSeq(), enzyme.pos);
			if (!enzyme.isPalindromic())
				reverseSeqs[index] = this.virtualFragmentation(sfn.seq, enzyme.getReverseSeq(), enzyme.seq.length() - enzyme.pos - 1);
			else // Don't cut
				reverseSeqs[index] = new boolean[forwardSeqs[index].length];
			index++;
		}

		boolean[] finalSeq = this.combinedSingleColor(this.combinedSingleColor(forwardSeqs), this.combinedSingleColor(reverseSeqs));

		return new DataNode(sfn.name, finalSeq);
	}

	public List<PotentialNickingSiteBreak> getPotentialNickingSiteBreaks(String name, long[] forwardRefp, long[] reverseRefp) {

		List<PotentialNickingSiteBreak> PotentialNickingSiteBreaks = new ArrayList<>();
		int forwardIndex = -1;
		int reverseIndex = -1;
		while (forwardIndex < forwardRefp.length - 1 || reverseIndex < reverseRefp.length - 1) {
			if (forwardIndex == forwardRefp.length - 1 || (reverseIndex < reverseRefp.length - 1 && forwardRefp[forwardIndex + 1] > reverseRefp[reverseIndex + 1])) {
				reverseIndex++;
				Long prevForwardSig = forwardIndex > -1 ? forwardRefp[forwardIndex] : null;
				Long nextForwardSig = forwardIndex + 1 < forwardRefp.length ? forwardRefp[forwardIndex + 1] : null;
				PotentialNickingSiteBreaks.add(new PotentialNickingSiteBreak(new GenomicPosNode(name, reverseRefp[reverseIndex], reverseRefp[reverseIndex]), -1, prevForwardSig, nextForwardSig));
				// Output
			} else if (reverseIndex == reverseRefp.length - 1 || (forwardIndex < forwardRefp.length - 1 && forwardRefp[forwardIndex + 1] < reverseRefp[reverseIndex + 1])) {
				forwardIndex++;
				Long prevReverseSig = reverseIndex > -1 ? reverseRefp[reverseIndex] : null;
				Long nextReverseSig = reverseIndex + 1 < reverseRefp.length ? reverseRefp[reverseIndex + 1] : null;
				PotentialNickingSiteBreaks.add(new PotentialNickingSiteBreak(new GenomicPosNode(name, forwardRefp[forwardIndex], forwardRefp[forwardIndex]), 1, prevReverseSig, nextReverseSig));
				// Output
			} else if (forwardRefp[forwardIndex + 1] == reverseRefp[reverseIndex + 1]) {
				forwardIndex++;
				reverseIndex++;
				PotentialNickingSiteBreaks.add(new PotentialNickingSiteBreak(new GenomicPosNode(name, forwardRefp[forwardIndex], forwardRefp[forwardIndex]), 0, forwardRefp[forwardIndex], reverseRefp[reverseIndex]));
				// Consider if we also add reverse
			}

		}
		return PotentialNickingSiteBreaks;

	}

	public List<PotentialNickingSiteBreak> getPotentialNickingSiteBreaks(StreamFastaNode sfn) {
		if (enzymeList.isEmpty())
			return new ArrayList<>();
		boolean[][] forwardSeqs = new boolean[enzymeList.size()][];
		boolean[][] reverseSeqs = new boolean[enzymeList.size()][];
		int index = 0;
		for (EnzymeSiteNode enzyme : enzymeList) {
			forwardSeqs[index] = this.virtualFragmentation(sfn.seq, enzyme.getForwardSeq(), enzyme.pos);
			if (!enzyme.isPalindromic()) // When the enzyme is palindromic, probably it is an restriction enzyme
				reverseSeqs[index] = this.virtualFragmentation(sfn.seq, enzyme.getReverseSeq(), enzyme.seq.length() - enzyme.pos - 1);
			else // Don't cut
				reverseSeqs[index] = new boolean[forwardSeqs[index].length];
			index++;
		}

		return getPotentialNickingSiteBreaks(sfn.name, new DataNode("Dummy", this.combinedSingleColor(forwardSeqs)).refp, new DataNode("Dummy", this.combinedSingleColor(reverseSeqs)).refp);
	}

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(FastaToOM.class.getSimpleName(), "Performs in silico digestion on DNA sequence.");
		StreamFastaReader.assignOptions(parser);
		EnzymeSiteNode.assignOptions(parser);
		ReferenceWriter.assignOptions(parser);
		parser.addHeader("Nicking Site Break Prediction Options", 1);
		OptionSpec<String> oNickingSiteBreakOut = parser.accepts("nsbout", "Potential nicking site breaks output (The prediction is useful for nicking enzyme-based data only)").withRequiredArg().ofType(String.class);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		OMWriter<PotentialNickingSiteBreak> ow = null;
		if (options.has(oNickingSiteBreakOut))
			ow = new OMWriter<PotentialNickingSiteBreak>(options.valueOf(oNickingSiteBreakOut)) {
				@Override
				public void write(PotentialNickingSiteBreak fs) throws IOException {
					bw.write(fs.region.ref + "\t" + fs.region.start + "\t" + fs.region.stop + "\t" + fs.getName() + "\t" + fs.getScore() + "\t" + fs.strand + "\t" + "\t" + "\t" + "0,0,0" + "\t" + "\t"
							+ "\t" + "\n");
				}
			};
		ReferenceWriter rw = null;
		if (options.has("refmapout"))
			rw = new ReferenceWriter(options);
		List<EnzymeSiteNode> enzymeList = EnzymeSiteNode.getEnzyme(options);
		enzymeList.addAll(EnzymeSiteNode.getEnzymeSequence(options));
		if (enzymeList.isEmpty())
			System.err.println("Warning: no enzyme is input!");
		FastaToOM fto = new FastaToOM(enzymeList);
		StreamFastaReader fsr = new StreamFastaReader(options);

		StreamFastaNode sfn;
		while ((sfn = fsr.read()) != null) {
			if (rw != null)
				rw.write(fto.convertFasta(sfn));
			if (ow != null) {
				for (PotentialNickingSiteBreak fs : fto.getPotentialNickingSiteBreaks(sfn))
					ow.write(fs);
			}
		}

		fsr.close();
		if (rw != null)
			rw.close();
		if (ow != null)
			ow.close();
	}
}

class PotentialNickingSiteBreak {
	public final GenomicPosNode region;
	public final int strand;
	public final Long closestPrev;
	public final Long closestNext;

	public PotentialNickingSiteBreak(GenomicPosNode region, int strand, Long closestPrev, Long closestNext) {
		this.region = region;
		this.strand = strand;
		this.closestPrev = closestPrev;
		this.closestNext = closestNext;
	}

	public String getName() {
		Long prevDiff = closestPrev == null ? null : region.start - closestPrev;
		Long nextDiff = closestNext == null ? null : closestNext - region.start;
		return (prevDiff == null ? "NA" : prevDiff) + ":" + (nextDiff == null ? "NA" : nextDiff);
	}

	public long getScore() {
		Long prevDiff = closestPrev == null ? null : region.start - closestPrev;
		Long nextDiff = closestNext == null ? null : closestNext - region.start;

		if (prevDiff == null)
			return nextDiff;
		if (nextDiff == null)
			return prevDiff;
		return Math.min(prevDiff, nextDiff);
	}

}
