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
import java.lang.reflect.Field;

public class ViewSetting {
	public static int alignmentLineHeight = 50;
	public static int alignmentLineLength = alignmentLineHeight - 10;
	public static int signalStrokeWidth = 2;
	public static int bodyHeight = 10;
	public static int moleculeSpace = 10;
	public static long anchorFlankSize = 200000;
	public static long closeSignal = 2000;
	public static int groupRefDistance = 2000000;
	public static int groupFragDistance = 2000000;
	public static int moleculeNameSize = 20;
	public static long minSVObjectSize = 5000;
	public static int maxMoleculeViewItems = 100;
	
	public static double defaultDNARatio = 400;
	public static double defaultZoom = 1.0;
	public static double zoomPerRotation = 0.1;
	public static double minZoom = 0.2;
	public static double maxZoom = 10.0;
	
	public static Color moleculeColor = Color.GREEN;
	public static Color refColor = Color.RED;
	public static Color alignedRefColor = Color.RED;
	public static Color unalignedRefColor = Color.BLUE;
	public static Color alignedMoleculeColor = Color.YELLOW;
	public static Color unalignedMoleculeColor = Color.GREEN;
	
	public static Color signalColor = Color.BLACK;
	public static Color regionalViewAlignedSignalColor = Color.MAGENTA;
	public static Color anchorViewAlignedSignalColor = Color.MAGENTA;
	public static Color anchorViewAnchoredSignalColor = Color.BLUE;

	public static boolean alignmentViewModify = true;
	public static boolean alignmentViewModifyScale = false;
	
	public static void changeSetting(String setting, int value) {
		if (setting == null)
			return;
		try {
			Field field = ViewSetting.class.getDeclaredField(setting);
			Object oldvalue = field.get(null);
			field.set(null, value);
			Object newvalue = field.get(null);
			System.err.println("Field " + setting + " has changed value from " + oldvalue + " to " + newvalue);
		} catch (NoSuchFieldException e) {
			
			System.err.println("Field " + setting + " doesn't exist");
			System.err.println("All available settings:");
			for (Field field : ViewSetting.class.getDeclaredFields())
				System.err.println("- " + field.getName());

		} catch (SecurityException e) {
			System.err.println("Security execption while assessing fields");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
