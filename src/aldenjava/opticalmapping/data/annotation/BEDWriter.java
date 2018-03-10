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


package aldenjava.opticalmapping.data.annotation;

import java.io.IOException;
import java.util.List;

import aldenjava.opticalmapping.data.OMWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

public class BEDWriter extends OMWriter<BEDNode> {

	/**
	 * BED Writer
	 * 
	 * @author Alden
	 *
	 */

	public BEDWriter(String filename) throws IOException {
		super(filename);
	}

	public BEDWriter(OptionSet options) throws IOException {
		this((String) options.valueOf("bedout"));
	}

	@Override
	public void initializeHeader() throws IOException {
	}

	@Override
	public void write(BEDNode bed) throws IOException {
		bw.write(bed.chrom);
		bw.write("\t");
		bw.write(Long.toString(bed.chromStart));
		bw.write("\t");
		bw.write(Long.toString(bed.chromEnd));

		bw.write("\t");
		if (bed.name != null)
			bw.write(bed.name);
		bw.write("\t");
		if (bed.score != null)
			bw.write(Integer.toString(bed.score));
		bw.write("\t");
		if (bed.strand != null)
			bw.write(bed.strand);
		bw.write("\t");
		if (bed.thickStart != null)
			bw.write(Long.toString(bed.thickStart));
		bw.write("\t");
		if (bed.thickEnd != null)
			bw.write(Long.toString(bed.thickEnd));
		bw.write("\t");
		if (bed.itemRgb != null)
			bw.write(bed.itemRgb.getRed() + "," + bed.itemRgb.getGreen() + "," + bed.itemRgb.getBlue());
		bw.write("\t");
		if (bed.blockCount != null)
			bw.write(Integer.toString(bed.blockCount));
		bw.write("\t");
		if (bed.blockSizes != null)
			;
		if (bed.blockStarts != null)
			;
		bw.write("\n");
		// Block sizes and starts are not supported yet
	}

	public void writeAll(List<BEDNode> bedList) throws IOException {
		for (BEDNode bed : bedList)
			write(bed);
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("BED Writer Options", level);
		parser.accepts("bedout", "Output BED file").withRequiredArg().ofType(String.class);
	}

}
