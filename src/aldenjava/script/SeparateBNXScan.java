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


package aldenjava.script;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import aldenjava.opticalmapping.data.data.BnxDataNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.OptMapDataWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

public class SeparateBNXScan {

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(SeparateBNXScan.class.getSimpleName(), "Separates a bnx file into multiple bnx files according to the global scan number");
		
		OptMapDataReader.assignOptions(parser, 1);
		parser.addHeader("SeparateBNXScan Options", 1);
		parser.accepts("prefix", "Output prefix").withRequiredArg().ofType(String.class).defaultsTo("Scan");
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		Map<Integer, OptMapDataWriter> omdws = new HashMap<>();
		OptionSet options = parser.parse(args);
		String prefix = (String) options.valueOf("prefix");
		OptMapDataReader omdr = new OptMapDataReader(options);
		DataNode data;
		while ((data = omdr.read()) != null) {
			int scn;
			if (data instanceof BnxDataNode) {
				BnxDataNode bnxData = (BnxDataNode) data;
				
				if (bnxData.hasGlobalScanNumber)
					scn = bnxData.globalScanNumber;
				else
					if (bnxData.hasScanNumber)
						scn = bnxData.scanNumber;
					else
						scn = 0;
			}
			else
				scn = 0;
			
			if (!omdws.containsKey(scn))
				omdws.put(scn, new OptMapDataWriter(prefix + scn + ".bnx"));
			omdws.get(scn).write(data);
				
		}
		omdr.close();
		for (OptMapDataWriter omdw : omdws.values())
			omdw.close();
				
		
	}

}
