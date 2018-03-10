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


package aldenjava.file;

import java.io.File;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FilenameUtils;

import aldenjava.opticalmapping.data.CompressionFormat;

/**
 * A class the is mainly based on FileNameExtensionFilter. The main difference is on the filter allow compressed file (with format defined in CompressionFormat)
 * @author Alden
 *
 */
public class FileNameCompressionExtensionFilter extends FileFilter {
	private final String description;
	private final String[] extensions;
	private final String[] lowerCaseExtensions;
	public FileNameCompressionExtensionFilter(String description, String... extensions) {
		if (extensions == null || extensions.length == 0) {
			throw new IllegalArgumentException(
					"Extensions must be non-null and not empty");
		}
		this.description = description;
		this.extensions = new String[extensions.length];
		this.lowerCaseExtensions = new String[extensions.length];
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i] == null || extensions[i].length() == 0) {
				throw new IllegalArgumentException(
					"Each extension must be non-null and not empty");
			}
			this.extensions[i] = extensions[i];
			lowerCaseExtensions[i] = extensions[i].toLowerCase(Locale.ENGLISH);
		}
	}

	@Override
	public boolean accept(File f) {
		if (f != null) {
			if (f.isDirectory()) {
				return true;
			}
			String filename = f.getName();
			String desiredExtension = FilenameUtils.getExtension(filename);
			if (CompressionFormat.isValidFormat(desiredExtension))
				desiredExtension = FilenameUtils.getExtension(FilenameUtils.getBaseName(filename));
			desiredExtension = desiredExtension.toLowerCase(Locale.ENGLISH);
			for (String extension : lowerCaseExtensions)
				if (desiredExtension.equals(extension)) {
					return true;
				}
		}
		return false;
	}

	@Override
	public String getDescription() {
		return description;
	}

	
}
