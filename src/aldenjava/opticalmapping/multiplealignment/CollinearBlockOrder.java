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


package aldenjava.opticalmapping.multiplealignment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.ProgressMonitorInputStream;

import aldenjava.opticalmapping.visualizer.OMView;
import joptsimple.OptionSet;

public class CollinearBlockOrder {

	// Key: group, value: individual
	public final LinkedHashMap<String, List<String>> orderMap;
	private final List<String> individualOrder;
	// Key: individual, value: group
	public final LinkedHashMap<String, String> assignedOrderMap;
	public CollinearBlockOrder(List<String> order) {
		orderMap = new LinkedHashMap<>();
		individualOrder = new ArrayList<>();
		assignedOrderMap = new LinkedHashMap<>();
		for (String s : order) {
			List<String> item = new ArrayList<>();
			item.add(s);
			orderMap.put(s, item);
			individualOrder.add(s);
			assignedOrderMap.put(s, s);
		}
	}
	public CollinearBlockOrder(LinkedHashMap<String, List<String>> orderMap) {
		this.orderMap = orderMap;
		this.individualOrder = new ArrayList<>();
		this.assignedOrderMap = new LinkedHashMap<>();
		for (String group : orderMap.keySet()) {
			List<String> individual = orderMap.get(group);
			individualOrder.addAll(individual);
			for (String ind : individual)
				assignedOrderMap.put(ind, group);
		}
	}
	
	public List<String> getIndividualOrder() {
		return individualOrder;
	}
	
	public static CollinearBlockOrder readAll(OptionSet options) throws IOException {
		return readAll((String) options.valueOf("cboin"));
	}
	public static CollinearBlockOrder readAll(String filename) throws IOException {
		BufferedReader reader;
		String line;
		LinkedHashMap<String, List<String>> orderMap = new LinkedHashMap<>();
		reader = new BufferedReader(new FileReader(filename));
		while ((line = reader.readLine()) != null) {
			String groupName = null;
			String individualName = null;
			String[] l = line.split("\\s+");
			if (l.length == 1) {
				groupName = l[0];
				individualName = l[0];
			}
			else if (l.length == 2) {
				groupName = l[0];
				individualName = l[1];
			}
			if (!orderMap.containsKey(groupName))
				orderMap.put(groupName, new ArrayList<String>());
			orderMap.get(groupName).add(individualName);
		}
		reader.close();
		return new CollinearBlockOrder(orderMap);
	}
	public static void writeAll(OptionSet options, CollinearBlockOrder order) throws IOException {
		writeAll((String) options.valueOf("cboout"), order);
	}
	public static void writeAll(String filename, CollinearBlockOrder order) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		for (String key : order.orderMap.keySet())
			for (String s : order.orderMap.get(key))
				bw.write(key + "\t" + s + "\n");
		bw.close();
	}
}
