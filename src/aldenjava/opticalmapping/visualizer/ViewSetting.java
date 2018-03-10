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


package aldenjava.opticalmapping.visualizer;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

import aldenjava.common.ColorStringParser;
import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

/**
 * Stores all OMView settings
 * @author Alden
 *
 */
public class ViewSetting {
	
	// General options
	// Panel options
	public static int objBorderX = 20;
	public static int objBorderY = 20;
	
	// Zooming options
	public static double defaultDNARatio = 400;
	public static double defaultZoom = 1.0;
	public static double zoomPerRotation = 0.1;
	public static double minZoom = 0.2;
	public static double maxZoom = 10.0;
	
	// Goto options
	public static int defaultLeftSpaceAfterGoto = 10;
	public static int defaultTopSpaceAfterGoto = 10;
//	public static int gotoMarkDelay = 100;
//	public static int gotoMarkAlphaChange = 12;
//	public static int gotoMarkRadius = 20;
//	public static Color initialGotoMarkColor = Color.RED;

	
	// VObject options
	// Molecule options
	public static int signalStrokeWidth = 2;
	public static int bodyHeight = 10;
	public static int moleculeSpace = 10;
	public static int maMoleculeSpace = 10;
	public static int mabMoleculeSpace = 200;
	
	// Query name setting
	public static boolean displayQueryName = true;
	public static Color queryNameColor = Color.BLACK;

	// Gap setting
	public static long minSpaceSize = 5000;
	public static int gapStrokeWidth = 2;
	public static Color gapStrokeColor = Color.BLACK;
	
	// Coverage and variability
	public static boolean showCoverage = true;
	public static int coverageHeight = 30;
	public static boolean showCoverageAxis = true;
	public static int coverageAxisUnit = 50;
	public static Color coverageAxisColor = Color.GRAY;
	public static int maxDisplayCoverage = -1;
	public static int variabilityHeight = 20;
	public static int maxVariableBlockTypes = 10;
	
	// Ruler options
	public static boolean displayRuler = true;
	public static int rulerHeight = 30;
	public static long rulerSmallMark = 10000;
	public static long rulerLargeMark = 100000;

	// Tool tip text setting
	public static long closeSignal = 2000;
	
	
	// Specific view setting
	// Alignment view setting
	public static int alignmentLineHeight = 50;
	public static int alignmentLineLength = 40;
	public static boolean alignmentViewModify = true;
	public static boolean alignmentViewModifyScale = false;
	public static boolean separateAlignmentDisplay = false;
	
	// Regional view setting
	public static boolean useVariableColor = true;
	public static int groupRefDistance = 2000000;
	public static int groupFragDistance = 2000000;
	
	// Anchor view setting
	public static long anchorFlankSize = 200000;

	// Molecule view setting
	public static int moleculeNameSize = 20;
	public static int maxMoleculeViewItems = 100;

	// Annotation view setting
	public static long annotationTextLength = 100000;
	public static long annotationBlockHeight = 15;
	
	// Multiple alignment view setting
	public static int hideBlockThreshold = 0;
	public static boolean hideOverlapBlocks = false;
	public static boolean useVariabilityColor = false;
	public static boolean collapseSameGroupQuery = false;

	// Multiple alignment block view setting
	public static int blockConnectionLineWidth = 2;

	// Coloring options
	// Body Color options
	public static Color moleculeColor = Color.GREEN;
	public static Color refColor = Color.RED;
	public static Color alignedRefColor = Color.RED;
	public static Color unalignedRefColor = Color.BLUE;
	public static Color alignedMoleculeColor = Color.YELLOW;
	public static Color unalignedMoleculeColor = Color.GREEN;
	
	// Signal Color options
	public static Color signalColor = Color.BLACK;
	public static Color regionalViewAlignedSignalColor = Color.MAGENTA;
	public static Color anchorViewAlignedSignalColor = Color.MAGENTA;
	public static Color anchorViewAnchoredSignalColor = Color.BLUE;

	// Ruler Color options
	public static Color rulerSmallMarkColor = new Color(127, 127, 127);
	public static Color rulerLargeMarkColor = Color.BLACK;
	public static Color rulerBodyColor = Color.BLACK;

	// Other Color options
	public static Color coverageBGColor = new Color(248, 248, 248);
	public static Color coverageColor = new Color(51, 51, 202);
	public static Color variationColor1 = new Color(240, 240, 240);
	public static Color variationColor2 = new Color(202, 51, 51);
	public static Color maBGColor1 = new Color(236, 242, 254);
	public static Color maBGColor2 = new Color(255, 255, 255);
	

	
	
	/*
	public static boolean hasField(String setting) {
		return Arrays.stream(ViewSetting.class.getFields()).map(Field::getName).anyMatch(setting::equals);
	}
	public static Object getValue(String setting) {
		Field field;
		Object value = null;
		try {
			field = ViewSetting.class.getDeclaredField(setting);
			if (field.getType() == Color.class) {
				value = ((Color) field.get(null)).getRGB();
			}
			else {
				value = field.get(null);
			}
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return value;
	}
	*/
	public static void changeSetting(String setting, String value) {
		if (setting == null || value == null)
			return;
		try {
			Field field = ViewSetting.class.getDeclaredField(setting);
			
			Object oldvalue = field.get(null);
			Object parsedValue;
			if (field.getType() == Integer.TYPE || field.getType() == Integer.class)
				parsedValue = Integer.parseInt(value);
			else
				if (field.getType() == Long.TYPE || field.getType() == Long.class)
					parsedValue = Long.parseLong(value);
				else
					if (field.getType() == Double.TYPE || field.getType() == Double.class)
						parsedValue = Double.parseDouble(value);
					else
						if (field.getType() == Boolean.TYPE || field.getType() == Boolean.class)
							parsedValue = Boolean.parseBoolean(value);
						else
							if (field.getType() == Color.class) {
								parsedValue = ColorStringParser.parseString(value);
							}
							else
								if (field.getType() == String.class)
									parsedValue = value;
								else
									throw new IllegalArgumentException("Unsupported setting field: " + field.getType());
			field.set(null, parsedValue);
			Object newvalue = field.get(null);
			if (!oldvalue.equals(newvalue))
				VerbosePrinter.println("Field " + setting + " has changed value from " + oldvalue + " to " + newvalue);
			
		} catch (NoSuchFieldException e) {
			System.err.println("Field " + setting + " doesn't exist");
		} catch (SecurityException e) {
			System.err.println("Security execption while assessing fields");
		} catch (IllegalArgumentException e) {
			System.err.println("Incorrect format on the field " + setting);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} 
	}


	public static void displaySetting() {
		System.out.println("#All available OMView setting");
		System.out.println("#Field name\tValue\tType");
		Field[] fields = ViewSetting.class.getDeclaredFields();
		for (Field field : fields)
			try {
				Object value;
				Object type;
				if (field.getType() == Color.class) {
					value = ((Color) field.get(null)).getRGB();
					type = "Color (RGB)";
				}
				else {
					value = field.get(null);
					type = field.getType();
				}
				System.out.println(field.getName() + "\t" + value + "\t" + type);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		
	}

	
	public static void importSetting(String settingFile) {
		try (BufferedReader br = new BufferedReader(new FileReader(settingFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty())
					continue;
				if (line.startsWith("#"))
					continue;
				String[] l = line.split("\\s+");
				String setting = l[0];
				String value = l[1];
				changeSetting(setting, value);
			}
		} catch (IOException e) {
			System.err.println("Cannot open setting file: " + settingFile);
		}
		
	}
}
