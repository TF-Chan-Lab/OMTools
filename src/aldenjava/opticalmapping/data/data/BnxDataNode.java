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


package aldenjava.opticalmapping.data.data;

/**
 * A class to store BnxData with snr and intensity information
 * @author Alden
 *
 */
public class BnxDataNode extends DataNode {

	// Requires overriding certain modification method in DataNode
	
	// BNX version 1.0
	public double avgIntensity;
	public double moleculeSNR;
	public int originalMoleculeID;
	public int scanNumber;
	public int scanDirection;
	public String chipID;
	public int flowCell;
	public boolean hasScanNumber;
	
	// BNX version 1.2
	public int runID;
	public int globalScanNumber;
	public boolean hasGlobalScanNumber;
	
	public double[] snr; // equals to refp.length
	public double[] intensity; // equals to refp.length
	public BnxDataNode(String name, long size, long[] refp, double[] snr, double[] intensity) {
		super(name, size, refp);
		if (snr.length != refp.length)
			throw new IllegalArgumentException("Array snr must have the same length as refp");
		if (intensity.length != refp.length)
			throw new IllegalArgumentException("Array intensity must have the same length as refp");
		this.snr = snr;
		this.intensity = intensity;
		
		this.hasScanNumber = false;
		this.hasGlobalScanNumber = false;
	}
	
	public BnxDataNode(String name, long size, long[] refp, double[] snr, double[] intensity, double avgIntensity, double moleculeSNR, int originalMoleculeID, int scanNumber, int scanDirection, String chipID, int flowCell) {
		super(name, size, refp);
		this.avgIntensity = avgIntensity;
		this.moleculeSNR = moleculeSNR;
		this.originalMoleculeID = originalMoleculeID;
		this.scanNumber = scanNumber;
		this.scanDirection = scanDirection;
		this.chipID = chipID;
		this.flowCell = flowCell;
		this.snr = snr;
		this.intensity = intensity;
		
		this.hasScanNumber = true;
		this.hasGlobalScanNumber = false;
	}
	
	public BnxDataNode(String name, long size, long[] refp, double[] snr, double[] intensity, double avgIntensity, double moleculeSNR, int originalMoleculeID, int scanNumber, int scanDirection, String chipID, int flowCell, int runID,
			int globalScanNumber) {
		super(name, size, refp);
		this.avgIntensity = avgIntensity;
		this.moleculeSNR = moleculeSNR;
		this.originalMoleculeID = originalMoleculeID;
		this.scanNumber = scanNumber;
		this.scanDirection = scanDirection;
		this.chipID = chipID;
		this.flowCell = flowCell;
		this.runID = runID;
		this.globalScanNumber = globalScanNumber;
		this.snr = snr;
		this.intensity = intensity;

		this.hasScanNumber = true;
		this.hasGlobalScanNumber = true;
	}
	
}
