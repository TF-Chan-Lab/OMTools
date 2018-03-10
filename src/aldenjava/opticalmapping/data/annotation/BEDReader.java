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

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

// unfinished class. First line description or browser setting is not supported
public class BEDReader extends OMReader<BEDNode> {
	public BEDReader(String filename) throws IOException {
		super(filename);
	}

	public BEDReader(InputStream stream) throws IOException {
		super(stream);
	}

	@Override
	public BEDNode read() throws IOException {
		if (nextline == null)
			return null;
		Scanner scanner = new Scanner(nextline);
		scanner.useDelimiter("\t");
		String chrom = scanner.next();
		long chromStart = scanner.nextLong();
		long chromStop = scanner.nextLong();
		String name = null;
		Integer score = null; // [0, 1000]
		String strand = null;
		Long thickStart = null;
		Long thickEnd = null;
		Color itemRgb = null;
		Integer blockCount = null;
		List<Long> blockSizes = null;
		List<Long> blockStarts = null;
		int column = 4;
		while (scanner.hasNext()) {
			String s = scanner.next();
			if (s.isEmpty()) {
				column++;
				continue;
			}
			Scanner inScanner = new Scanner(s);
			inScanner.useDelimiter("\t");
			switch (column) {
				case 4:
					name = inScanner.next();
					break;
				case 5:
					score = inScanner.nextInt();
					break;
				case 6:
					strand = inScanner.next();
					break;
				case 7:
					thickStart = inScanner.nextLong();
					break;
				case 8:
					thickEnd = inScanner.nextLong();
					break;
				case 9:
					String rgbString = inScanner.next();
					String[] rgbStringArray = rgbString.split(",");
					if (rgbStringArray.length != 3)
						break;
					int r = Integer.parseInt(rgbStringArray[0]);
					int g = Integer.parseInt(rgbStringArray[1]);
					int b = Integer.parseInt(rgbStringArray[2]);
					itemRgb = new Color(r, g, b);
					break;
				case 10:
					blockCount = inScanner.nextInt();
					break;
				case 11:
					String blockSizesString = inScanner.next();
					String[] blockSizesArray = blockSizesString.split(",");
					if (blockCount == null || blockCount != blockSizesArray.length)
						break;
					blockSizes = new ArrayList<Long>();
					for (String blockS : blockSizesArray)
						blockSizes.add(Long.parseLong(blockS));
					break;
				case 12:
					String blockStartsString = inScanner.next();
					String[] blockStartsArray = blockStartsString.split(",");
					if (blockCount == null || blockCount != blockStartsArray.length)
						break;
					blockStarts = new ArrayList<Long>();
					for (String blockS : blockStartsArray)
						blockStarts.add(Long.parseLong(blockS));
					break;
				default:
					break;
			}
			inScanner.close();
			column++;
		}
		scanner.close();
		proceedNextLine();
		return new BEDNode(chrom, chromStart, chromStop, name, score, strand, thickStart, thickEnd, itemRgb, blockCount, blockSizes, blockStarts);
	}
	public static void assignOptions(ExtendOptionParser parser, int level) {
		parser.addHeader("BED Reader", level);
		parser.accepts("bedin", "Input BED File").withRequiredArg().ofType(String.class);
	}
}
