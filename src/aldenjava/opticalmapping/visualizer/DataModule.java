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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import aldenjava.opticalmapping.data.annotation.AnnotationNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.data.mappingresult.ResultsBreaker;
import aldenjava.opticalmapping.mapper.clustermodule.ResultClusterModule;
import aldenjava.opticalmapping.multiplealignment.CollinearBlock;
import aldenjava.opticalmapping.multiplealignment.CollinearBlockOrder;


public class DataModule extends JComponent {
	public static boolean useResultsBreaker = false;
	
	private LinkedHashMap<String, LinkedHashMap<String, DataNode>> fragmentInfoMap = new LinkedHashMap<String, LinkedHashMap<String, DataNode>>();
	private LinkedHashMap<String, LinkedHashMap<String, DataNode>> referenceInfoMap = new LinkedHashMap<String, LinkedHashMap<String, DataNode>>();
	private LinkedHashMap<String, LinkedHashMap<String, List<List<OptMapResultNode>>>> resultInfoMap = new LinkedHashMap<String, LinkedHashMap<String, List<List<OptMapResultNode>>>>();
	private LinkedHashMap<String, LinkedHashMap<String, CollinearBlock>> multipleAlignmentInfoMap = new LinkedHashMap<String, LinkedHashMap<String, CollinearBlock>>();
	private LinkedHashMap<String, CollinearBlockOrder> multipleAlignmentOrderInfoMap = new LinkedHashMap<String, CollinearBlockOrder>();
	private LinkedHashMap<String, LinkedHashMap<String, Color>> multipleAlignmentColorInfoMap = new LinkedHashMap<String, LinkedHashMap<String, Color>>();
	private LinkedHashMap<String, List<? extends AnnotationNode>> annoInfoMap = new LinkedHashMap<String, List<? extends AnnotationNode>>();

	private LinkedHashMap<VDataType, LinkedHashMap<String, ?>> dataInfoMap = new LinkedHashMap<VDataType, LinkedHashMap<String, ?>>(); {
		for (VDataType type : VDataType.values())
			switch (type) {
				case REFERENCE: 
					dataInfoMap.put(type, referenceInfoMap);
					break;
				case MOLECULE: 
					dataInfoMap.put(type, fragmentInfoMap);
					break;
				case ALIGNMENT: 
					dataInfoMap.put(type, resultInfoMap);
					break;
				case MULTIPLEALIGNMENTBLOCK: 
					dataInfoMap.put(type, multipleAlignmentInfoMap);
					break;
				case MULTIPLEALIGNMENTCOLOR: 
					dataInfoMap.put(type, multipleAlignmentColorInfoMap);
					break;
				case MULTIPLEALIGNMENTORDER: 
					dataInfoMap.put(type, multipleAlignmentOrderInfoMap);
					break;
				case ANNOTATION: 
					dataInfoMap.put(type, annoInfoMap);
					break;
				default:
					assert false;
			}
	}
	
	private OMView mainView;
	
	
	public List<SwingWorker<?,?>> taskList = new ArrayList<SwingWorker<?,?>>();
	
	public int meas = 1000;
	public double ear = 0.1;

		
	public DataModule(OMView mainView)
	{
		this.mainView = mainView;
		this.addPropertyChangeListener(mainView);
	}

	public void setResultsBreakerParameters(int meas, double ear) 
	{
		this.meas = meas;
		this.ear = ear;
	}

	private String getSimpleFileName(String filename)
	{
		File f = new File(filename);
		return f.getName();
	}
	
	public void addData(String filename, LinkedHashMap<String, DataNode> fragmentInfo)
	{		
		filename = getSimpleFileName(filename); 
		if (fragmentInfoMap.containsKey(filename))
			// ask? 
			return;
		fragmentInfoMap.put(filename, fragmentInfo);
		this.firePropertyChange("Molecule", "", filename);
	}
	public void addReference(String filename, LinkedHashMap<String, DataNode> referenceInfo)
	{		
		filename = getSimpleFileName(filename);
		if (referenceInfoMap.containsKey(filename))
			return;
		referenceInfoMap.put(filename, referenceInfo);
		this.firePropertyChange("Reference", "", filename);
	}
	public void addResult(String filename, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap)
	{
		if (useResultsBreaker)
			breakResult(filename, resultlistmap);
		else
			groupResult(filename, resultlistmap);
		this.firePropertyChange("Result", "", filename);
	}
	public void addMultipleAlignment(String filename, LinkedHashMap<String, CollinearBlock> multipleAlignment) {
		filename = getSimpleFileName(filename);
		if (multipleAlignmentInfoMap.containsKey(filename))
			return;
		multipleAlignmentInfoMap.put(filename, multipleAlignment);
		this.firePropertyChange("MultipleAlignment", "", filename);
	}
	public void addMultipleAlignmentOrder(String filename, CollinearBlockOrder multipleAlignmentOrder) {
		filename = getSimpleFileName(filename);
		if (multipleAlignmentOrderInfoMap.containsKey(filename))
			return;
		multipleAlignmentOrderInfoMap.put(filename, multipleAlignmentOrder);
		this.firePropertyChange("MultipleAlignment", "", filename);
	}
	public void addMultipleAlignmentColor(String filename, LinkedHashMap<String, Color> multipleAlignmentColor) {
		filename = getSimpleFileName(filename);
		if (multipleAlignmentColorInfoMap.containsKey(filename))
			return;
		multipleAlignmentColorInfoMap.put(filename, multipleAlignmentColor);
		this.firePropertyChange("MultipleAlignment", "", filename);
	}

	
	
	private synchronized void breakResult(String filename, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap) {
		ResultBreaker resultBreaker = new ResultBreaker(filename, resultlistmap, getAllReference(), meas, ear);
		resultBreaker.addPropertyChangeListener(mainView.statusPanel);
		resultBreaker.execute();
		taskList.add(resultBreaker);
	}
	private synchronized void groupResult(String filename, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap)
	{
		ResultGrouper resultGrouper = new ResultGrouper(filename, resultlistmap, getAllReference());
		resultGrouper.addPropertyChangeListener(mainView.statusPanel);
		resultGrouper.execute();
		taskList.add(resultGrouper);
	}
	public void addAnnotation(String filename, List<? extends AnnotationNode> annoList)
	{
		filename = getSimpleFileName(filename);
		if (annoInfoMap.containsKey(filename))
			return;
		annoInfoMap.put(filename, annoList);
		this.firePropertyChange("Annotation", "", filename);
	}
	
	// Getting the data
	public LinkedHashMap<String, DataNode> getReference(String filename)
	{
		return referenceInfoMap.get(filename);
	}
	public LinkedHashMap<String, DataNode> getReference(Collection<String> filelist)
	{
		int duplicatedEntries = 0;
		LinkedHashMap<String, DataNode> mergedMap = new LinkedHashMap<String, DataNode>();
		for (String filename : filelist) {
			LinkedHashMap<String, DataNode> map = getReference(filename);
			for (String key : map.keySet())
				if (mergedMap.put(key, map.get(key)) != null)
					duplicatedEntries++;
		}
		if (duplicatedEntries > 0)
			System.err.println("Warning: " + duplicatedEntries + " duplicated entries are found and overwritten in selected reference files.");
		return mergedMap;		
	}
	public LinkedHashMap<String, DataNode> getAllReference()
	{
		return getReference(referenceInfoMap.keySet());
	}
	
	public LinkedHashMap<String, DataNode> getData(String filename)
	{
		return fragmentInfoMap.get(filename);		
	}
	public LinkedHashMap<String, DataNode> getData(Collection<String> filelist)
	{
		int duplicatedEntries = 0;
		LinkedHashMap<String, DataNode> mergedMap = new LinkedHashMap<String, DataNode>();
		for (String filename : filelist) {
			LinkedHashMap<String, DataNode> map = getData(filename);
			for (String key : map.keySet())
				if (mergedMap.put(key, map.get(key)) != null)
					duplicatedEntries++;
		}
		if (duplicatedEntries > 0)
			System.err.println("Warning: " + duplicatedEntries + " duplicated entries are found and overwritten in selected molecule files.");
		return mergedMap;		
	}
	public LinkedHashMap<String, DataNode> getAllData()
	{
		return getData(new ArrayList<>(fragmentInfoMap.keySet()));
	}

	public LinkedHashMap<String, List<List<OptMapResultNode>>> getResult(String filename) {
		return resultInfoMap.get(filename);
	}
	public LinkedHashMap<String, List<List<OptMapResultNode>>> getResult(Collection<String> filelist) {
		int duplicatedEntries = 0;
		LinkedHashMap<String, List<List<OptMapResultNode>>> mergedMap = new LinkedHashMap<String, List<List<OptMapResultNode>>>();
		for (String filename : filelist) {
			LinkedHashMap<String, List<List<OptMapResultNode>>> map = getResult(filename);
			for (String key : map.keySet())
				if (mergedMap.put(key, map.get(key)) != null)
					duplicatedEntries++;
		}
		if (duplicatedEntries > 0)
			System.err.println("Warning: " + duplicatedEntries + " duplicated entries are found and overwritten in selected alignment files.");
		return mergedMap;		
	}
	public LinkedHashMap<String, List<List<OptMapResultNode>>> getAllResult() {
		return getResult(resultInfoMap.keySet());
	}
	public List<? extends AnnotationNode> getAnno(String filename) {
		return annoInfoMap.get(filename);
	}
	public List<AnnotationNode> getAnno(Collection<String> filelist) {
		List<AnnotationNode> list = new ArrayList<>();
		for (String filename : filelist)
			list.addAll(getAnno(filename));
		return list;
	}
	public List<AnnotationNode> getAllAnno() {
		return getAnno(annoInfoMap.keySet());
	}

	public LinkedHashMap<String, CollinearBlock> getMultipleAlignmentBlock(String filename) {
		return multipleAlignmentInfoMap.get(filename);
	}
	public CollinearBlockOrder getMultipleAlignmentOrder(String filename) {
		return multipleAlignmentOrderInfoMap.get(filename);
	}
	public LinkedHashMap<String, Color> getMultipleAlignmentColor(String filename) {
		return multipleAlignmentColorInfoMap.get(filename);
	}
	
	class ResultBreaker extends SwingWorker<LinkedHashMap<String, List<OptMapResultNode>>, List<OptMapResultNode>>{

		String filename;
		LinkedHashMap<String, DataNode> optrefmap;
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap;
		int meas;
		double ear;
		public ResultBreaker(String filename, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap, LinkedHashMap<String, DataNode> optrefmap, int meas, double ear)
		{
//			return newresultlistmap;		
			this.filename = filename;
			this.resultlistmap = resultlistmap;
			this.optrefmap = optrefmap;
			this.meas = meas;
			this.ear = ear;
		}

		@Override
		protected LinkedHashMap<String, List<OptMapResultNode>> doInBackground() throws Exception 
		{
			ResultsBreaker resultsBreaker = new ResultsBreaker(optrefmap);
			resultsBreaker.setMode(1);
			resultsBreaker.setParameters(meas, ear, 5, 2, 2);
			LinkedHashMap<String, List<OptMapResultNode>> newresultlistmap = new LinkedHashMap<String, List<OptMapResultNode>>();
			for (String key : resultlistmap.keySet())
			{
				List<OptMapResultNode> resultlist = resultlistmap.get(key);
				List<OptMapResultNode> newresultlist = new ArrayList<OptMapResultNode>();
				for (OptMapResultNode result : resultlist)
					newresultlist.addAll(resultsBreaker.breakResult(result));
				if(!newresultlist.isEmpty())
					newresultlistmap.put(key, newresultlist);
				publish(newresultlist);
			}
					
			return newresultlistmap;
		}
		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					groupResult(filename, get());
					DataModule.this.taskList.remove(this);
				} catch (InterruptedException | ExecutionException e) {
					// TODO Auto-generated catch block
					JOptionPane.showMessageDialog(mainView, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(mainView, "Unknown error occurs in breaking results.");
					e.printStackTrace();
				}
			else
				JOptionPane.showMessageDialog(mainView, "Cancelled.");

		}

	}
	
	class ResultGrouper extends SwingWorker<LinkedHashMap<String, List<List<OptMapResultNode>>>, List<List<OptMapResultNode>>>{
		String filename;
		LinkedHashMap<String, DataNode> optrefmap;
		LinkedHashMap<String, List<OptMapResultNode>> resultlistmap;
		public ResultGrouper(String filename, LinkedHashMap<String, List<OptMapResultNode>> resultlistmap, LinkedHashMap<String, DataNode> optrefmap)
		{
//			return newresultlistmap;		
			this.filename = filename;
			this.resultlistmap = resultlistmap;
			this.optrefmap = optrefmap;
		}

		@Override
		protected LinkedHashMap<String, List<List<OptMapResultNode>>> doInBackground() throws Exception 
		{
			ResultClusterModule rcm = new ResultClusterModule(optrefmap);
			LinkedHashMap<String, List<List<OptMapResultNode>>> newresultlistmap = new LinkedHashMap<String, List<List<OptMapResultNode>>>();
//			int processedItems = 0;
			for (List<OptMapResultNode> resultlist : resultlistmap.values())
			{
				OptMapResultNode source = resultlist.get(0);
				if (source != null && source.isUsed())
				{
					List<List<OptMapResultNode>> resultlistlist = new ArrayList<List<OptMapResultNode>>();
					List<List<OptMapResultNode>> maplistlist = (rcm.group(resultlist, false, ViewSetting.groupRefDistance, ViewSetting.groupFragDistance));
					
					
					for (List<OptMapResultNode> maplist : maplistlist)
						resultlistlist.add(maplist);
					Collections.sort(resultlistlist, new Comparator<List<OptMapResultNode>>()
					{
						@Override
						public int compare(List<OptMapResultNode> list1, List<OptMapResultNode> list2) {
							int minsubfrag1 = list1.get(0).getTotalSegment();
							int minsubfrag2 = list1.get(0).getTotalSegment();
							for (OptMapResultNode result : list1)
							{
								if (result.subfragstart < minsubfrag1)
									minsubfrag1 = result.subfragstart;
								if (result.subfragstop < minsubfrag1)
									minsubfrag1 = result.subfragstop;
							}
							for (OptMapResultNode result : list2)
							{
								if (result.subfragstart < minsubfrag2)
									minsubfrag2 = result.subfragstart;
								if (result.subfragstop < minsubfrag2)
									minsubfrag2 = result.subfragstop;
							}
							return Integer.compare(minsubfrag1, minsubfrag2);
						}
					});
					newresultlistmap.put(source.parentFrag.name, resultlistlist);
					publish(resultlistlist);
//					processedItems++;
//					if (processedItems % (resultlistmap.size() / 100) == 0)
//						progressMonitor.setProgress(processedItems / (resultlistmap.size() / 100));
//					progressMonitor.setProgress(processedItems);
//					System.out.println(processedItems);
//					progressMonitor.setNote(String.format("Result processed: %d/%d", 0, processedItems));
				}
//				progressMonitor.close();
			}			
			return newresultlistmap;
		}
		@Override
		public void done()
		{
			if (!isCancelled())
				try {
					filename = getSimpleFileName(filename);
					resultInfoMap.put(filename, get());
					DataModule.this.firePropertyChange("Result", "", filename);
					DataModule.this.taskList.remove(this);
				} catch (InterruptedException | ExecutionException e) {
					JOptionPane.showMessageDialog(mainView, "Task Interrupted.");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(mainView, "Unknown error occurs in grouping results.");
					e.printStackTrace();
				}
				
			else
				JOptionPane.showMessageDialog(mainView, "Cancelled.");

		}

	}

	public void clearResult(String filename) {
		resultInfoMap.remove(filename);
		DataModule.this.firePropertyChange("Result", filename, "");
	}

	public void clearAllResult() {
		List<String> keySet = new ArrayList<String>(resultInfoMap.keySet());
		for (String filename : keySet)
			this.clearResult(filename);
	}

	public void clearMolecule(String filename) {
		fragmentInfoMap.remove(filename);
		DataModule.this.firePropertyChange("Molecule", filename, "");
	}

	public void clearAllMolecule() {
		List<String> keySet = new ArrayList<String>(fragmentInfoMap.keySet());
		for (String filename : keySet)
			this.clearMolecule(filename);
	}

	public synchronized boolean isAllTasksDone() {
		return taskList.size() == 0;
	}


	
	public LinkedHashMap<VDataType, List<String>> getAllDataSelection(VDataType... types) {
		LinkedHashMap<VDataType, List<String>> map = new LinkedHashMap<>();
		for (VDataType type : types) {
			map.put(type, new ArrayList<>(dataInfoMap.get(type).keySet()));
		}
		return map;
	}
	public List<LinkedHashMap<VDataType, List<String>>> getIndividualDataSelection(VDataType type) {
		List<String> filelist = new ArrayList<>(dataInfoMap.get(type).keySet());
		List<LinkedHashMap<VDataType, List<String>>> dataSelections = new ArrayList<>();
		for (String file : filelist) {
			LinkedHashMap<VDataType, List<String>> map = new LinkedHashMap<>();
			map.put(type, Arrays.asList(file));
			dataSelections.add(map);
		}
		return dataSelections;
	}


	public LinkedHashMap<VDataType, List<String>> getDataSelection(List<VDataType> types, LinkedHashMap<VDataType, List<String>> dataSelection) {
		LinkedHashMap<VDataType, List<String>> map = new LinkedHashMap<>();
		for (VDataType type : types) {
			map.put(type, new ArrayList<>(dataInfoMap.get(type).keySet()));
		}
		DataPanel dp = new DataPanel(map, dataSelection);
    	int answer = JOptionPane.showOptionDialog(null, dp, "Data selection",JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
    	if (answer == JOptionPane.OK_OPTION)
    		return dp.getValues();
    	else
    		return null;
	}

	/**
	 * Retain valid selections. This method should be run after the data set is updated
	 * @param type
	 * @param list
	 * @return valid selections
	 */
	public List<String> retainValidSelection(VDataType type, List<String> list) {
		List<String> retainedList = new ArrayList<>();
		for (String s : list)
			if (dataInfoMap.get(type).keySet().contains(s))
				retainedList.add(s);
		return retainedList;
	}
	public LinkedHashMap<VDataType, List<String>> retainValidSelection(LinkedHashMap<VDataType, List<String>> dataSelection) {
		LinkedHashMap<VDataType, List<String>> retainedDataSelection = new LinkedHashMap<>();
		for (VDataType type : dataSelection.keySet())
			retainedDataSelection.put(type, retainValidSelection(type, dataSelection.get(type)));
		return retainedDataSelection;
	}


}

