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


package aldenjava.opticalmapping.miscellaneous;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang.StringUtils;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

/**
 * A class extending the <code>OptionParser</code> from the package joptsimple. This allows better help menu layout
 * 
 * @author Alden
 *
 */
public class ExtendOptionParser extends OptionParser {

	// A static variable for manual generation
	public static boolean manualGeneration = false;
	// Static variables for OMTools GUI
	public static boolean guiGeneration = false;
	public static ExtendOptionParser lastParserInstance = null;
	
	
	private List<String> headers;
	private List<Integer> headerLevels;
//	private List<OptionParser> subParser;
	private List<List<String>> optionList;
	private List<List<String>> optionDescriptionList;
	private List<String> recentOptionList;
	private List<String> recentOptionDescriptionList;
//	private OptionParser recentparser;
	private final String title;
	private final String description;
	private Map<String, String> aliasMap;
	
 	public ExtendOptionParser() { 
		this(null, null);
	}

	public ExtendOptionParser(String title) {
		this(title, null);
	}
	public ExtendOptionParser(String title, String description) {
		super();
		this.title = title;
		this.description = description;
		headers = new ArrayList<String>();
		headerLevels = new ArrayList<Integer>();
		optionList = new ArrayList<List<String>>();
		optionDescriptionList = new ArrayList<List<String>>();
		recentOptionList = null;
		recentOptionDescriptionList = null;
		aliasMap = new HashMap<>();

	}

	public void addHeader(String s, int headerLevels) {
		if (s != null) {
			if (headerLevels >= 0) {
				this.headers.add(s);
				this.headerLevels.add(headerLevels);
				recentOptionList = new ArrayList<String>();
				recentOptionDescriptionList = new ArrayList<String>();
			}
			optionList.add(recentOptionList);
			optionDescriptionList.add(recentOptionDescriptionList);
		} else {
			recentOptionList = null;
			recentOptionDescriptionList = null;
		}
	}

	@Override
	public OptionSpecBuilder accepts(String option, String description) {
		if (recentOptionList != null && recentOptionDescriptionList != null) {
			recentOptionList.add(option);
			recentOptionDescriptionList.add(description);
		} else {
			// rewrite description when there is no headers
			for (int i = 0; i < optionList.size(); i++) {
				List<String> singleOptionList = optionList.get(i);
				for (int j = 0; j < singleOptionList.size(); j++) {
					String oldOption = singleOptionList.get(j);
					if (oldOption.equalsIgnoreCase(option))
						optionDescriptionList.get(i).set(j, description);
				}
			}

		}
		return super.accepts(option, description);
	}

	private List<String> paragraphSeparate(String s, int len) {
		List<String> list = new ArrayList<String>();
		s = s.trim();
		while (s.length() > len) {
			String tmpStr = s.substring(0, len);
			int breakPt = tmpStr.lastIndexOf(" ");
			list.add(s.substring(0, breakPt).trim());
			s = s.substring(breakPt).trim();
		}
		list.add(s.trim());
		return list;
	}
	
	@Override
	public void printHelpOn(OutputStream sink) {
		// Hack for manual generation and GUI menu generation
		if (manualGeneration) {
			printHelpOnManualGeneration(sink);
			return;
		}
		if (guiGeneration) {
			lastParserInstance = this;
			return;
		}
		
		int paragraphLength = 75;
		try {
			NullHelpFormatter nhf = new NullHelpFormatter();
			super.formatHelpWith(nhf);
			super.printHelpOn(new NullOutputStream());
			
//			if (recentparser != null)
//				subParser.add(recentparser);
//			recentparser = null;
			if (title != null) {
				sink.write(("  " + title + "\n").getBytes());
				if (description != null && !description.isEmpty()) {
					List<String> descriptionList = this.paragraphSeparate(description, paragraphLength);
					String leftAdjust = "  ";
					sink.write("\n".getBytes());
					for (int k = 0; k < descriptionList.size(); k++)
						sink.write((leftAdjust + String.format("%s\n", descriptionList.get(k))).getBytes());
				}
				sink.write((StringUtils.repeat("=", 60) + "\n").getBytes());
			}
			
			for (int i = 0; i < headers.size(); i++) {
				String separateLine = "";
				String leftAdjust = StringUtils.repeat(" ", (headerLevels.get(i) - 1) * 2);
				if (headerLevels.get(i) == 1)
					separateLine = StringUtils.repeat("=", 10);
				else
					separateLine = StringUtils.repeat("-", 10);
				sink.write(("\n" + leftAdjust + separateLine + headers.get(i) + separateLine + "\n").getBytes());
				int elements = optionList.get(i).size();
				leftAdjust = "  " + leftAdjust;
				for (int j = 0; j < elements; j++) {
					String option = "--" + optionList.get(i).get(j);
					String description = optionDescriptionList.get(i).get(j);
					List<?> defaultValues = nhf.options.get(optionList.get(i).get(j)).defaultValues();
					if (nhf.options.get(optionList.get(i).get(j)).isRequired())
						description += " [Required]";
					if (defaultValues.size() >= 1)
						description += String.format(" [Default: %s]", defaultValues.size() == 1 ? defaultValues.get(0).toString() : defaultValues.toString());
						
					List<String> descriptionList = this.paragraphSeparate(description, paragraphLength);
					sink.write((leftAdjust + String.format("%-25s%s\n", option, descriptionList.get(0))).getBytes());
					for (int k = 1; k < descriptionList.size(); k++)
						sink.write((leftAdjust + String.format("%-25s  %s\n", "", descriptionList.get(k))).getBytes());
				}
			}
		} catch (IOException e) {
			System.err.println("IO Error. Help menu is not displayed successfully.");
			e.printStackTrace();
		}
	}
	
	public void addHiddenAlias(String option, String... aliases) {
		for (String alias : aliases)
			aliasMap.put(alias, option);
	}
	
	// This method is used to generate part of the software manual
	public void printHelpOnManualGeneration(OutputStream sink) {
		try {
			NullHelpFormatter nhf = new NullHelpFormatter();
			super.formatHelpWith(nhf);
			super.printHelpOn(new NullOutputStream());

//			if (recentparser != null)
//				subParser.add(recentparser);
//			recentparser = null;
			if (title != null) {
				sink.write(("\\section{" + title + "}\n").getBytes());
				String description = this.description.replaceAll("in\\ssilico", "\\\\textit{in silico}");
				sink.write((description + "\n").getBytes());
			}
			
			for (int i = 0; i < headers.size(); i++) {
				
				if (headerLevels.get(i) == 1)
					sink.write(("\\subsection{" + headers.get(i) + "}\n").getBytes());
				else if (headerLevels.get(i) == 2)
					sink.write(("\\subsubsection{" + headers.get(i) + "}\n").getBytes());
				sink.write(("\\begin{description}\n").getBytes());
				
				
				int elements = optionList.get(i).size();
				for (int j = 0; j < elements; j++) {
					String option = "--" + optionList.get(i).get(j);
					String description = optionDescriptionList.get(i).get(j);
					List<?> defaultValues = nhf.options.get(optionList.get(i).get(j)).defaultValues();
					if (nhf.options.get(optionList.get(i).get(j)).isRequired())
						description += " [Required]";
					if (defaultValues.size() >= 1)
						description += String.format(" [Default: %s]", defaultValues.size() == 1 ? defaultValues.get(0).toString() : defaultValues.toString());
					description = description.replaceAll("\\\\", "\\\\textbackslash ");
					
					sink.write(("\\item[" + "\\texttt{" + option + "}] " + description + "\n").getBytes());
				}
				sink.write(("\\end{description}\n").getBytes());
			}
			sink.write(("\\newpage\n").getBytes());
		} catch (IOException e) {
			System.err.println("IO Error. Help menu is not displayed successfully.");
			e.printStackTrace();
		}
	}
	
	@Override
	public OptionSet parse(String... arguments) {
		// To assign the non-option values to the last occurring option by adding extra option
		// -A 1 2 **2 is non-options value
		// -A 1 -A 2
		List<String> args = new ArrayList<String>();
		String currentOption = null;
		boolean prevIsOption = false;
		for (String s : arguments) {
			if (s.startsWith("-")) // option
			{
				int index = s.lastIndexOf('-');
				String tmpOption = s.substring(index + 1);
				if (aliasMap.containsKey(tmpOption))
					s = s.substring(0, index + 1) + aliasMap.get(tmpOption);
				currentOption = s;
				prevIsOption = true;
			} else {

				if (currentOption != null)
					if (!prevIsOption)
						args.add(currentOption);
				prevIsOption = false;
			}
			args.add(s);
		}
		return super.parse(args.toArray(new String[args.size()]));
	}
	
	public Map<String, ? extends OptionDescriptor> getOptionDescriptors() {
		try {
			NullHelpFormatter nhf = new NullHelpFormatter();
			super.formatHelpWith(nhf);
			super.printHelpOn(new NullOutputStream());
			return nhf.options;
		} catch (IOException e) {
			throw new RuntimeException("Exception in obtaining option descriptors");
		}
	}
	
	/**
	 * Return the ExtendOptionParser of a main class. The method requires guiGeneration to be true.
	 * @param clazz
	 * @return
	 */
	public static ExtendOptionParser getClassParser(Class<?> clazz) {
		if (!ExtendOptionParser.guiGeneration)
			throw new IllegalStateException("guiGeneration needs to be true. ");
		try {
			Class<?>[] argTypes = new Class[] { String[].class };
			Method main = clazz.getDeclaredMethod("main", argTypes);
			main.invoke(null, (Object) new String[]{});
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return ExtendOptionParser.lastParserInstance;
	}
	// Getters. Used in GUI
	public List<String> getHeaders() {
		return headers;
	}

	public List<Integer> getHeaderLevels() {
		return headerLevels;
	}

	public List<List<String>> getOptionList() {
		return optionList;
	}

	public List<List<String>> getOptionDescriptionList() {
		return optionDescriptionList;
	}

	public List<String> getRecentOptionList() {
		return recentOptionList;
	}

	public List<String> getRecentOptionDescriptionList() {
		return recentOptionDescriptionList;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}
	
}

class NullHelpFormatter implements HelpFormatter {

	public Map<String, ? extends OptionDescriptor> options;

	@Override
	public String format(Map<String, ? extends OptionDescriptor> options) {
		this.options = options;
		return "";
	}

}
