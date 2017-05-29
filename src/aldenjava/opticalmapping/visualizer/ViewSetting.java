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


package aldenjava.opticalmapping.visualizer;

import java.awt.Color;
import java.awt.Paint;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

import aldenjava.opticalmapping.miscellaneous.VerbosePrinter;

/**
 * A class provides
 * @author Alden
 *
 */
public class ViewSetting {
	
	
	public static int alignmentLineHeight = 50;
	public static int alignmentLineLength = 40;
	public static int signalStrokeWidth = 2;
	public static int bodyHeight = 10;
	public static int moleculeSpace = 10;
	public static int maMoleculeSpace = 10;
	public static long anchorFlankSize = 200000;
	public static long closeSignal = 2000;
	public static int groupRefDistance = 2000000;
	public static int groupFragDistance = 2000000;
	public static int moleculeNameSize = 20;
	public static long SVObjectSize = 5000;
	public static int maxMoleculeViewItems = 100;
	
	public static int coverageHeight = 30;
	public static int variabilityHeight = 20;
	
	public static int maxVariableBlockTypes = 10;
	// Panel
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

	// Color
	public static Color coverageBGColor = new Color(248, 248, 248);
	public static Color coverageColor = new Color(51, 51, 202);
	public static Color variationColor1 = new Color(240, 240, 240);
	public static Color variationColor2 = new Color(202, 51, 51);
	
	public static boolean alignmentViewModify = true;
	public static boolean alignmentViewModifyScale = false;
	
	// Ruler setting
	public static boolean displayRuler = true;
	public static int rulerHeight = 30;
	public static long rulerSmallMark = 10000;
	public static long rulerLargeMark = 100000;
	public static Color rulerSmallMarkColor = new Color(127, 127, 127);
	public static Color rulerLargeMarkColor = Color.BLACK;
	public static Color rulerBodyColor = Color.BLACK;

	// Multiple alignment setting
	public static boolean displayQueryName = true;
	public static Color queryNameColor = Color.BLACK;
	public static Color maBGColor1 = new Color(236, 242, 254);
	public static Color maBGColor2 = new Color(255, 255, 255);
	
	
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
								if (value.contains(",")) {
									String[] l = value.split(",");
									if (l.length == 4)
										parsedValue = new Color(Integer.parseInt(l[0]),Integer.parseInt(l[1]),Integer.parseInt(l[2]),Integer.parseInt(l[3]));
									else
										if (l.length == 3)
											parsedValue = new Color(Integer.parseInt(l[0]),Integer.parseInt(l[1]),Integer.parseInt(l[2]));
										else
											throw new IllegalArgumentException("Unknown color format.");
								}
								else
									parsedValue = new Color(Integer.parseInt(value), true);
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
		} catch (NumberFormatException e) {
			System.err.println("Incorrect number format on the field " + setting);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
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
				String[] l = line.split("\t");
				String setting = l[0];
				String value = l[1];
				changeSetting(setting, value);
			}
		} catch (IOException e) {
			System.err.println("Cannot open setting file: " + settingFile);
		}
		
	}
}
