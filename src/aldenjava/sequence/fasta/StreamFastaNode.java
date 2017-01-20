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


package aldenjava.sequence.fasta;

public class StreamFastaNode {
	public final String name;
	public final String seq;

	public StreamFastaNode(String name, String seq) {
		this.name = name;
		this.seq = seq;
	}

	public StreamFastaNode(StreamFastaNode sfn) {
		this.name = sfn.name;
		this.seq = sfn.seq;
	}

	public StreamFastaNode getReverseComplement() {
		StringBuilder newseq = new StringBuilder();
		char[] array = seq.toCharArray();
		for (int i = array.length - 1; i >= 0; i--) {
			char c = array[i];
			char newc;
			switch (c) {
				case 'a':
				case 'A':
					newc = 'T';
					break;
				case 't':
				case 'T':
					newc = 'A';
					break;
				case 'c':
				case 'C':
					newc = 'G';
					break;
				case 'g':
				case 'G':
					newc = 'C';
					break;
				default:
					newc = c;
			}
			newseq.append(newc);
		}

		return new StreamFastaNode(name, newseq.toString());
	}

	@Override
	public String toString() {
		return ">" + name + "\n" + seq;
	}

	public int length() {
		return seq.length();
	}

	public StreamFastaNode subSeq(long start, long stop) {
		if (start > Integer.MAX_VALUE || stop > Integer.MAX_VALUE)
			throw new UnsupportedOperationException("Genomic region of long is not supported for subSeq");
		return new StreamFastaNode(name + "_" + start + "_" + stop, seq.substring((int) start - 1, (int) stop));
	}
}
