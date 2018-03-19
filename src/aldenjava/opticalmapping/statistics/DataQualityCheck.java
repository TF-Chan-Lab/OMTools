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


package aldenjava.opticalmapping.statistics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JFrame;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.data.OptMapDataReader;
import aldenjava.opticalmapping.data.data.ReferenceReader;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import aldenjava.opticalmapping.visualizer.viewpanel.ImageSaveFormat;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class DataQualityCheck {

	public static void main(String[] args) throws IOException {
	
		ExtendOptionParser parser = new ExtendOptionParser(DataQualityCheck.class.getSimpleName(), "Performs basic data quality check, including distributions of molecule length, signal density and segment length");
		ReferenceReader.assignOptions(parser, 1);
		OptMapDataReader.assignOptions(parser, 1);
		parser.addHeader("Data Quality Check Options", 1);
		parser.accepts("name", "Use data set name instead of file name.").withRequiredArg().ofType(String.class);
		parser.accepts("prefix", "Statistics output prefix").withRequiredArg().ofType(String.class).defaultsTo("output");
		parser.accepts("gradcolor", "Use gradient color").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
		OptionSpec<String> oimageformat = parser.accepts("imageformat", "Formats of image to be saved. " + ImageSaveFormat.getFormatHelp()).withRequiredArg().ofType(String.class).defaultsTo("png");
		parser.addHeader(null, -1);
		parser.accepts("refmapin");
		if (args.length == 0) {
			parser.printHelpOn(System.out);
			return;
		}
		OptionSet options = parser.parse(args);
		List<String> imageExtensions = options.valuesOf(oimageformat);
		boolean gradcolor = (boolean) options.valueOf("gradcolor");
		String prefix = (String) options.valueOf("prefix");
		List<String> dataFileNames = new ArrayList<>((List<String>) options.valuesOf("optmapin"));
		List<String> names = new ArrayList<>(dataFileNames);		
		if (options.has("name")) {
			names = new ArrayList<>((List<String>) options.valuesOf("name"));
			if (names.size() != dataFileNames.size())
				throw new IllegalArgumentException("Mismatch number of arguments provided for optmapin and name");
		}
		
		List<Integer> errorFileIndices = new ArrayList<>();
		LinkedHashMap<String, LinkedHashMap<String, DataNode>> dataSetMap = new LinkedHashMap<>();
		for (int i = 0; i < dataFileNames.size(); i++) {
			String dataFileName = dataFileNames.get(i);
			try {
				LinkedHashMap<String, DataNode> dataMap = OptMapDataReader.readAllData(dataFileName);
				dataSetMap.put(dataFileName, dataMap);
			} catch (IOException e) {
				errorFileIndices.add(i);
				System.err.println("Error in parsing " + dataFileName);
				e.printStackTrace();
			}
		}
		Collections.reverse(errorFileIndices);
		for (int i : errorFileIndices) {
			dataFileNames.remove(i);
			names.remove(i);
		}
		
		int noOfDataSets = names.size();
		LinkedHashMap<String, DataNode> refMap = null;
		if (options.has("refmapin"))
			refMap = ReferenceReader.readAllData(options);
		
	
		boolean show = false; 
		boolean toolTips = true;
		boolean urls = false; 
		
		long totalRefLength = -1;
		long totalRefSignals = -1;
		double refSignalDensity = -1;
		
		if (refMap != null) {
	       totalRefLength = refMap.values().stream().mapToLong(d -> d.length()).sum();
	       totalRefSignals = refMap.values().stream().mapToInt(d -> d.getTotalSignal()).sum();
	       refSignalDensity = totalRefSignals / (double) totalRefLength;
		}
       
	   Color color1 = Color.RED;
	   Color color2 = Color.BLUE;

       // Display Signal Density
       {
    	   int binNumber = 10;
    	   HistogramDataset dataset = new HistogramDataset();
    	   dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    	   for (int i = 0; i < noOfDataSets; i++) {
	    	   LinkedHashMap<String, DataNode> dataMap = dataSetMap.get(dataFileNames.get(i));
	    	   double[] signalDensity = dataMap.values().stream().mapToDouble(data -> data.getSignalDensity() * 100000).toArray();
	    	   dataset.addSeries(names.get(i), signalDensity, binNumber, 0, 20);
    	   }
    	   GradientColor gc = new GradientColor(color1, color2, noOfDataSets);
    	   String plotTitle = "Signal density distribution";
    	   String xaxis = "Signal density (No. of signals / 100kbp)";
    	   String yaxis = "Relative frequency"; 
    	   PlotOrientation orientation = PlotOrientation.VERTICAL; 
    	   JFreeChart chart = ChartFactory.createXYLineChart( plotTitle, xaxis, yaxis, 
    	   dataset, orientation, show, toolTips, urls);
    	   for (int i = 0; i < noOfDataSets; i++) {
    		   chart.getXYPlot().getRenderer().setSeriesStroke(i, new BasicStroke(3));
    		   if (gradcolor)
    			   chart.getXYPlot().getRenderer().setSeriesPaint(i, gc.getNext());
    	   }

    	   
    	   Shape s = new Line2D.Float(0,0,20,0);
    	   LegendItemCollection legends = chart.getXYPlot().getLegendItems();
    	   
    	   if (refMap != null) {
	    	   ValueMarker refSignalDensityMark = new ValueMarker(refSignalDensity * 100000);
	    	   refSignalDensityMark.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4}, 0));
	    	   refSignalDensityMark.setPaint(Color.BLACK);
	    	   chart.getXYPlot().addDomainMarker(refSignalDensityMark);
	    	   legends.add(new LegendItem("Reference signal density", null, null, null, s, refSignalDensityMark.getStroke(), refSignalDensityMark.getPaint()));
    	   }
    	   chart.addLegend(new LegendTitle(() -> legends));
    	   save(prefix + "SignalDensityDistribution.png", chart, imageExtensions);
       }
       
       // Length
       {
    	   int binNumber = 10;
    	   HistogramDataset dataset = new HistogramDataset();
    	   dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    	   for (int i = 0; i < noOfDataSets; i++) {
    	   	LinkedHashMap<String, DataNode> dataMap = dataSetMap.get(dataFileNames.get(i));
    	   	double[] length = dataMap.values().stream().mapToDouble(data -> data.length() / 1000.0).toArray();
    	   	dataset.addSeries(names.get(i), length, binNumber, 0, 500);
    	   }
    	   GradientColor gc = new GradientColor(color1, color2, noOfDataSets);

    	   String plotTitle = "Length distribution";
    	   String xaxis = "Length (kbp)";
    	   String yaxis = "Relative frequency"; 
    	   PlotOrientation orientation = PlotOrientation.VERTICAL; 
    	   JFreeChart chart = ChartFactory.createXYLineChart( plotTitle, xaxis, yaxis, 
    	   dataset, orientation, show, toolTips, urls);
    	   for (int i = 0; i < noOfDataSets; i++) {
    		   chart.getXYPlot().getRenderer().setSeriesStroke(i, new BasicStroke(3));
    		   if (gradcolor)
    			   chart.getXYPlot().getRenderer().setSeriesPaint(i, gc.getNext());

    	   }

    	   LegendItemCollection legends = chart.getXYPlot().getLegendItems();
    	   chart.addLegend(new LegendTitle(() -> legends));
    	   save(prefix + "LengthDistribution.png", chart, imageExtensions);
       }
       // segment length
       {
    	   int binNumber = 10;
    	   HistogramDataset dataset = new HistogramDataset();
    	   dataset.setType(HistogramType.RELATIVE_FREQUENCY);
    	   for (int i = 0; i < noOfDataSets; i++) {
    		   LinkedHashMap<String, DataNode> dataMap = dataSetMap.get(dataFileNames.get(i));
    		   double[] segLength = dataMap.values().stream().flatMapToLong(data -> Arrays.stream(data.getRefl())).mapToDouble(d -> d / 1000.0).toArray();
    		   dataset.addSeries(names.get(i), segLength, binNumber, 0, 50);
    	   }
    	   if (refMap != null) {
	    	   double[] refSegLength = refMap.values().stream().flatMapToLong(data -> Arrays.stream(data.getRefl())).mapToDouble(d -> d / 1000.0).toArray();
	    	   dataset.addSeries("Reference", refSegLength, binNumber, 0, 50);
    	   }
    	   
    	   GradientColor gc = new GradientColor(color1, color2, noOfDataSets);
    	   String plotTitle = "Segment length distribution";
    	   String xaxis = "Segment length (kbp)";
    	   String yaxis = "Relative frequency"; 
    	   PlotOrientation orientation = PlotOrientation.VERTICAL; 
    	   JFreeChart chart = ChartFactory.createXYLineChart( plotTitle, xaxis, yaxis, 
    	   dataset, orientation, show, toolTips, urls);
    	   for (int i = 0; i < noOfDataSets; i++) {
    		   chart.getXYPlot().getRenderer().setSeriesStroke(i, new BasicStroke(3));
    		   if (gradcolor)
    			   chart.getXYPlot().getRenderer().setSeriesPaint(i, gc.getNext());
    	   }
    	   chart.getXYPlot().getRenderer().setSeriesStroke(noOfDataSets, new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4}, 0));
    	   chart.getXYPlot().getRenderer().setSeriesPaint(noOfDataSets, Color.BLACK);
    	   LegendItemCollection legends = chart.getXYPlot().getLegendItems();
    	   chart.addLegend(new LegendTitle(() -> legends));
    	   save(prefix + "SegmentLengthDistribution.png", chart, imageExtensions);
       }
	}
	
	public static void save(String filename, JFreeChart chart, List<String> imageExtensions) throws IOException {
		int width = 600;
		int height = 600;
		for (String extension : imageExtensions) {
			ImageSaveFormat format = ImageSaveFormat.lookupfileext(extension); 	
			switch (format) {
				case PNG:
					ChartUtilities.saveChartAsPNG(new File(filename), chart, width, height);
					break;
				case JPG:
					ChartUtilities.saveChartAsJPEG(new File(filename), chart, width, height);
					break;
				case SVG:
					// Using the default code to generate svg
					DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
					String svgNS = "http://www.w3.org/2000/svg";
					Document document = domImpl.createDocument(svgNS, "svg", null);
					SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
					boolean useCSS = true; 
					BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
					svgGenerator.stream(bw, useCSS);
					bw.close();
					break;
				default:
					;
			}
		}
	}
	
	static void show(JFreeChart chart) {
		ChartPanel chartPanel = new ChartPanel( chart);  
		JFrame frame = new JFrame("JFrame Example");
		frame.add(chartPanel); 
		frame.setSize(500,500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}

class GradientColor {
	Color color1;
	Color color2;
	double currentRedColor;
	double currentGreenColor;
	double currentBlueColor;
	double currentAlphaColor;
	double redColorStep;
	double greenColorStep;
	double blueColorStep;
	double alphaColorStep;
	
	public GradientColor(Color color1, Color color2, int totalPoints) {
		this.color1 = color1;
		this.color2 = color2;
 	   int red1 = color1.getRed();
 	   int red2 = color2.getRed();
 	   int green1 = color1.getGreen();
 	   int green2 = color2.getGreen();
 	   int blue1 = color1.getBlue();
 	   int blue2 = color2.getBlue();
 	   int alpha1 = color1.getAlpha();
 	   int alpha2  = color2.getAlpha();
 	   if (totalPoints >= 2) {
	 	   this.redColorStep = (red2 - red1) / (double) (totalPoints - 1);
	 	   this.greenColorStep = (green2 - green1) / (double) (totalPoints - 1);
	 	   this.blueColorStep = (blue2 - blue1) / (double) (totalPoints - 1);
	 	   this.alphaColorStep = (alpha2 - alpha1) / (double) (totalPoints - 1);
 	   }
 	   else {
 		   this.redColorStep = 0;
		   this.greenColorStep = 0;
		   this.blueColorStep = 0;
		   this.alphaColorStep = 0;
 	   }
 	   this.currentRedColor = red1;
 	   this.currentGreenColor = green1;
 	   this.currentBlueColor = blue1;
 	   this.currentAlphaColor = alpha1;
	}
	
	public Color getNext() {
		Color color = new Color((int) currentRedColor, (int) currentGreenColor, (int) currentBlueColor, (int) currentAlphaColor);
		currentRedColor += redColorStep;
		currentGreenColor += greenColorStep;
		currentBlueColor += blueColorStep;
		currentAlphaColor += alphaColorStep;
		return color;
	}
}