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


package aldenjava.file;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionParser;
import joptsimple.OptionSet;


public class ListExtractor {
	private List<String> data = new ArrayList<String>();
	public ListExtractor(OptionSet options) throws IOException
	{
		this((String) options.valueOf("list"));
	}
	public ListExtractor(String filename) throws IOException
	{
		  FileInputStream fstream = new FileInputStream(filename);
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
 		  String s = ""; 		  
 		  while ((s = br.readLine()) != null)
 		  {
 			  data.add(s);
 		  }
 		  br.close();
	}
	public List<String> dataextraction()
	{
		return this.data;
	}
	
	public static List<String> extractList(OptionSet options) throws IOException
	{
		return extractList((String) options.valueOf("list"));
	}
	public static List<String> extractList(String filename) throws IOException
	{
		  List<String> data = new ArrayList<String>();
		  FileInputStream fstream = new FileInputStream(filename);
		  DataInputStream in = new DataInputStream(fstream);
		  BufferedReader br = new BufferedReader(new InputStreamReader(in));
		  String s = "";
		  while ((s = br.readLine()) != null)
		  {
			  data.add(s);
		  }
		  br.close();
		  return data;
	}
	public static List<List<String>> extractMultiList(String filename, String delimiter, String header) throws IOException {
		List<String> data = extractList(filename);
		List<List<String>> parseData = new ArrayList<>();
		Pattern pattern = Pattern.compile(delimiter);
		int column = -1;
		for (String d : data) {
			if (header != null)
				if (d.startsWith(header))
					continue;
			
			String[] l = pattern.split(d);
			// Initialize
			if (column == -1) {
				column = l.length;
				for (int i = 0; i < l.length; i++)
					parseData.add(new ArrayList<String>());
			}
			if (l.length == column) {
				for (int i = 0; i < l.length; i++)
					parseData.get(i).add(l[i]);
			}
			else
				System.err.println("Warning: data column not matched!");
		}
		return parseData;
	}

	public static List<String> extractList(String filename, String delimiter, int column, String header) throws IOException {
		List<String> data = extractList(filename);
		List<String> parseData = new ArrayList<String>();
		Pattern pattern = Pattern.compile(delimiter);
		for (String d : data) {
			if (header != null)
				if (d.startsWith(header))
					continue;
			String[] l = pattern.split(d);
			if (l.length > column)
				parseData.add(l[column]);
			else
				System.err.println("Warning: data column not matched!");
		}
		return parseData;
	}
	public static void writeList(String filename, List<String> list) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
		for (String s : list)
			bw.write(s + "\n");
		bw.close();
	}

	public static <E> List<E> extractDuplicatedValue(List<E> list)
	{
		HashSet<E> duplicatedList = new HashSet<E>();
		HashSet<E> savedList = new HashSet<E>();
		for (E e : list)
			if (!savedList.add(e))
				duplicatedList.add(e);
		return new ArrayList<E>(duplicatedList);
	}
	public static void assignOptions(ExtendOptionParser parser, int level)
	{
		parser.addHeader("List in File", level);
		parser.accepts("list", "File containing list of string.").withRequiredArg().ofType(String.class);
	}
	
}
