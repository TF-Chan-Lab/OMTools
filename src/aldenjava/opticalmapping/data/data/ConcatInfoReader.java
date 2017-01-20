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

import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.OMReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class ConcatInfoReader extends OMReader<ConcatInfo> {

	public ConcatInfoReader(String filename) throws IOException {
		super(filename);
	}

	@Override
	public ConcatInfo read() throws IOException {
		if (nextline == null)
			return null;
		String[] l = nextline.split("\t");
		GenomicPosNode oldPos = new GenomicPosNode(l[0]);
		GenomicPosNode newPos = new GenomicPosNode(l[1]);
		proceedNextLine();
		return new ConcatInfo(oldPos, newPos);
	}

	public static void assignOptions(ExtendOptionParser parser, int level)	{
		parser.addHeader("ConcatInfo Reader Options", level);
		parser.accepts("concatin", "ConcatInfo file input.").withRequiredArg().ofType(String.class);
	}

}
