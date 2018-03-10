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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import aldenjava.file.ListExtractor;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.OptMapDataWriter;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;

public class DuplicatedMoleculesRemover {

	public static void main(String[] args) throws IOException {
		ExtendOptionParser parser = new ExtendOptionParser(DuplicatedMoleculesRemover.class.getSimpleName(), "Removes detected duplicated molecules from the data file");
		parser.addHeader("Duplicated Molecules Remover Options", 1);
		OptionSpec<String> dupin = parser.accepts("dupin", "Files containing duplicated molecules").withRequiredArg().ofType(String.class).required();
		OptMapDataReader.assignOptions(parser);
		OptMapDataWriter.assignOptions(parser);

		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}

		OptionSet options = parser.parse(args);
		String dupFile = dupin.value(options);
		List<String> dlist = ListExtractor.extractList(dupFile);

		HashSet<String> dset = new HashSet<String>(dlist);
		OptMapDataReader omdr = new OptMapDataReader(options);
		OptMapDataWriter omdw = new OptMapDataWriter(options);
		DataNode data;
		while ((data = omdr.read()) != null)
			if (!dset.contains(data.name))
				omdw.write(data);
		omdr.close();
		omdw.close();
	}
}
