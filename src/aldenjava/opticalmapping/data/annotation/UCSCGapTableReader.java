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


package aldenjava.opticalmapping.data.annotation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class UCSCGapTableReader {

	public static List<UCSCGapNode> readAll(String filename) throws IOException
	{
		List<UCSCGapNode> gapList = new ArrayList<UCSCGapNode>();
		
		BufferedReader br = new BufferedReader(new FileReader(filename));
		
		String s;
		while ((s = br.readLine()) != null)
		{
			if (!s.startsWith("#"))
			{
				Scanner scanner = new Scanner(s).useDelimiter("\t");
				int bin = scanner.nextInt();
				String chrom = scanner.next();
				long chromStart = scanner.nextLong();
				long chromEnd = scanner.nextLong();
				int ix = scanner.nextInt();
				char n = scanner.next().charAt(0);
				long size = scanner.nextLong();
				String type = scanner.next();
				String bridge = scanner.next();
				gapList.add(new UCSCGapNode(bin, chrom, chromStart, chromEnd, ix, n, size, type, bridge));
			}
		}
		br.close();
		return gapList;
	}
}
