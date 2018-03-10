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


package aldenjava.opticalmapping.mapper.omfmmapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import aldenjava.opticalmapping.Cigar;
import aldenjava.opticalmapping.GenomicPosNode;
import aldenjava.opticalmapping.data.data.DataNode;
import aldenjava.opticalmapping.data.mappingresult.MatchingSignalPair;
import aldenjava.opticalmapping.data.mappingresult.OptMapResultNode;
import aldenjava.opticalmapping.mapper.AlignmentOptions;
import aldenjava.opticalmapping.mapper.Mapper;
import aldenjava.opticalmapping.mapper.MapperConstructionException;
import aldenjava.opticalmapping.mapper.MatchHelper;
import aldenjava.opticalmapping.mapper.seeding.SeedDatabase;
import aldenjava.opticalmapping.miscellaneous.ExtendOptionParser;
import joptsimple.OptionSet;

public class OMFMMapper extends Mapper {

	private LinkedHashMap<String, LinkedHashMap<Integer, List<FMEdge>>> refEdgeMapCollection;
	private int measure;
	private double scalingRange;
	private int rfalselimit;
	private int qfalselimit;
	private int cfalselimit;
	private int matchscore;
	private int falseppenalty;
	private int falsenpenalty;
	private int minalignscore;
	public OMFMMapper(LinkedHashMap<String, DataNode> optrefmap) {
		super(optrefmap);
	}
	private LinkedHashMap<Integer, List<FMEdge>> buildEdges(DataNode data, int maxErrorLimit) {
		LinkedHashMap<Integer, List<FMEdge>> dataEdgeMap = new LinkedHashMap<>();
		for (int error = 0; error <= maxErrorLimit; error++) {
			List<FMEdge> edges = new ArrayList<>();
			for (int i = 1; i + error < data.getTotalSegment() - 1; i++) {
				edges.add(new FMEdge(data.name, i - 1, i + error, data.length(i, i + error)));
			}
			Collections.sort(edges);
			dataEdgeMap.put(error, edges);
		}
		return dataEdgeMap;
	}

	@Override
	public void setParameters(OptionSet options) throws IOException {
		super.setParameters(options);
		this.setParameters((int) options.valueOf("meas"), (double) options.valueOf("ear"), 
				(int) options.valueOf("match"), (int) options.valueOf("fpp"), (int) options.valueOf("fnp"),
				(int) options.valueOf("rfalselimit"), (int) options.valueOf("qfalselimit"), (int) options.valueOf("cfalselimit"), (int) options.valueOf("minalignscore"));
	}
	public void setParameters(int measure, double scalingRange, int matchscore, int falseppenalty, int falsenpenalty, int rfalselimit, int qfalselimit, int cfalselimit, int minalignscore) {
		this.measure = measure;
		this.scalingRange = scalingRange;
		this.matchscore = matchscore;
		this.falseppenalty = falseppenalty;
		this.falsenpenalty = falsenpenalty;
		this.rfalselimit = rfalselimit;
		this.qfalselimit = qfalselimit;
		this.cfalselimit = cfalselimit;
		this.minalignscore = minalignscore;
		// Initialize
		LinkedHashMap<String, LinkedHashMap<Integer, List<FMEdge>>>  refEdgeMapCollection = new LinkedHashMap<>();
		for (DataNode ref : optrefmap.values()) {
			refEdgeMapCollection.put(ref.name, buildEdges(ref, rfalselimit));
		}
		this.refEdgeMapCollection = refEdgeMapCollection;
	}

	@Override
	public List<OptMapResultNode> getResult(DataNode query, List<GenomicPosNode> regionList) {
		List<OptMapResultNode> resultList = new ArrayList<>();
		LinkedHashMap<Integer, List<FMEdge>> queryEdgeMap = buildEdges(query, qfalselimit);
		

		for (String ref : refEdgeMapCollection.keySet()) {
			LinkedHashMap<Integer, List<FMEdge>> refEdgeMap = refEdgeMapCollection.get(ref);
			Map<MatchingSignalPair, GraphMSPVertex> forwardVertexMap = new LinkedHashMap<>();
			Map<MatchingSignalPair, GraphMSPVertex> reverseVertexMap = new LinkedHashMap<>();
			for (int queryError = 0; queryError <= qfalselimit; queryError++) {
				List<FMEdge> queryEdges = queryEdgeMap.get(queryError);
				for (int refError = 0; refError <= rfalselimit && queryError + refError <= cfalselimit; refError++) {
					List<FMEdge> refEdges = refEdgeMap.get(refError);
					for (FMEdge queryEdge : queryEdges) {
						// To use binary search to replace linear search
						
						FMEdge minEdge = FMEdge.newDummyEdge(MatchHelper.getMinMatchingRefSegmentSize(queryEdge.getLength(), measure, scalingRange));
						FMEdge maxEdge = FMEdge.newDummyEdge(MatchHelper.getMaxMatchingRefSegmentSize(queryEdge.getLength(), measure, scalingRange));
						int index1 = Collections.binarySearch(refEdges, minEdge);
						if (index1 < 0)
							index1 = (index1 + 1) * -1;
						else
							while (index1 > 0 && refEdges.get(index1 - 1).compareTo(minEdge) == 0)
								index1--;
						int index2 = Collections.binarySearch(refEdges, maxEdge);
						if (index2 < 0)
							index2 = (index2 + 1) * -1 - 1;
						else
							while (index2 < refEdges.size() - 1 && refEdges.get(index2 + 1).compareTo(maxEdge) == 0)
								index2++;
						for (int index = index1; index <= index2; index++) {
							FMEdge refEdge = refEdges.get(index);
							assert (MatchHelper.match(refEdge.getLength(), queryEdge.getLength(), measure, scalingRange));
							// Forward
							{
								MatchingSignalPair msp1 = new MatchingSignalPair(refEdge.getPos1(), queryEdge.getPos1());
								MatchingSignalPair msp2 = new MatchingSignalPair(refEdge.getPos2(), queryEdge.getPos2());
								GraphMSPVertex vertex1 =forwardVertexMap.get(msp1);
								GraphMSPVertex vertex2 =forwardVertexMap.get(msp2);
								if (vertex1 == null) {
									vertex1 = new GraphMSPVertex(msp1.rpos, msp1.qpos);
									forwardVertexMap.put(msp1, vertex1);
								}
								if (vertex2 == null) {
									vertex2 = new GraphMSPVertex(msp2.rpos, msp2.qpos);
									forwardVertexMap.put(msp2, vertex2);
								}
								vertex1.addPotentialNextVertex(vertex2, matchscore - falsenpenalty * refEdge.getError() - falseppenalty * queryEdge.getError());
							}
							// Reverse
							{
								MatchingSignalPair msp1 = new MatchingSignalPair(refEdge.getPos1(), queryEdge.getPos2());
								MatchingSignalPair msp2 = new MatchingSignalPair(refEdge.getPos2(), queryEdge.getPos1());
								GraphMSPVertex vertex1 =reverseVertexMap.get(msp1);
								GraphMSPVertex vertex2 =reverseVertexMap.get(msp2);
								if (vertex1 == null) {
									vertex1 = new GraphMSPVertex(msp1.rpos, msp1.qpos);
									reverseVertexMap.put(msp1, vertex1);
								}
								if (vertex2 == null) {
									vertex2 = new GraphMSPVertex(msp2.rpos, msp2.qpos);
									reverseVertexMap.put(msp2, vertex2);
								}
								vertex1.addPotentialNextVertex(vertex2, matchscore - falsenpenalty * refEdge.getError() - falseppenalty * queryEdge.getError());
							
							}

						}
						/*
						for (int index = 0; index < refEdges.size(); index++) {
							FMEdge refEdge = refEdges.get(index);
							if (MatchHelper.match(refEdge.getLength(), queryEdge.getLength(), measure, scalingRange)) {
								// Forward
								{
									MatchingSignalPair msp1 = new MatchingSignalPair(refEdge.getPos1(), queryEdge.getPos1());
									MatchingSignalPair msp2 = new MatchingSignalPair(refEdge.getPos2(), queryEdge.getPos2());
									GraphMSPVertex vertex1 =forwardVertexMap.get(msp1);
									GraphMSPVertex vertex2 =forwardVertexMap.get(msp2);
									if (vertex1 == null) {
										vertex1 = new GraphMSPVertex(msp1.rpos, msp1.qpos);
										forwardVertexMap.put(msp1, vertex1);
									}
									if (vertex2 == null) {
										vertex2 = new GraphMSPVertex(msp2.rpos, msp2.qpos);
										forwardVertexMap.put(msp2, vertex2);
									}
									vertex1.addPotentialNextVertex(vertex2, matchscore - falsenpenalty * refEdge.getError() - falseppenalty * queryEdge.getError());
								}
								// Reverse
								{
									MatchingSignalPair msp1 = new MatchingSignalPair(refEdge.getPos1(), queryEdge.getPos2());
									MatchingSignalPair msp2 = new MatchingSignalPair(refEdge.getPos2(), queryEdge.getPos1());
									GraphMSPVertex vertex1 =reverseVertexMap.get(msp1);
									GraphMSPVertex vertex2 =reverseVertexMap.get(msp2);
									if (vertex1 == null) {
										vertex1 = new GraphMSPVertex(msp1.rpos, msp1.qpos);
										reverseVertexMap.put(msp1, vertex1);
									}
									if (vertex2 == null) {
										vertex2 = new GraphMSPVertex(msp2.rpos, msp2.qpos);
										reverseVertexMap.put(msp2, vertex2);
									}
									vertex1.addPotentialNextVertex(vertex2, matchscore - falsenpenalty * refEdge.getError() - falseppenalty * queryEdge.getError());
								
								}
							}
						}
						*/
					}
				}
			}
			
//			{
//				for (GraphMSPVertex vertex : forwardVertexMap.values())
//					vertex.computeScore();
//				for (GraphMSPVertex vertex : forwardVertexMap.values())
//					if (vertex.getGroup() == -1)
//						vertex.setGroup(nextGroup++);
//				int bestScore = 0;
//				GraphMSPVertex bestVertex = null;
//				for (GraphMSPVertex vertex : forwardVertexMap.values())
//					if (vertex.getScore() > bestScore) {
//						bestScore = vertex.getScore();
//						bestVertex = vertex;
//					}
//				GraphMSPVertex vertex = bestVertex;
//				StringBuilder cigarString = new StringBuilder();
//				cigarString.append('M');
//				while (vertex.getNextVertex() != null) {
//					GraphMSPVertex nextVertex = vertex.getNextVertex();
//					for (int x = vertex.qpos + 1; x < nextVertex.qpos; x++)
//						cigarString.append('I');
//					for (int x = vertex.rpos + 1; x < nextVertex.rpos; x++)
//						cigarString.append('D');
//					cigarString.append('M');
//					vertex = nextVertex;
//				}
//				OptMapResultNode result = new OptMapResultNode(query, new GenomicPosNode(ref, optrefmap.get(ref).refp[bestVertex.rpos], optrefmap.get(ref).refp[vertex.rpos]), 1, bestVertex.rpos + 1, vertex.rpos, bestVertex.qpos + 1, vertex.qpos, new Cigar(cigarString.toString()), bestScore, -1);
//				resultList.add(result);
//			}
			// Forward
			{
				// Compute Score
				for (GraphMSPVertex vertex : forwardVertexMap.values())
					vertex.computeScore();
				
				int nextGroup = 1;
				// Compute Group
				int startGroupID = nextGroup;
				for (GraphMSPVertex vertex : forwardVertexMap.values())
					if (vertex.getGroup() == -1)
						vertex.setGroup(nextGroup++);
				int stopGroupID = nextGroup - 1;
				
				// Initialize best score map for each group
				GraphMSPVertex[] bestVertices = new GraphMSPVertex[nextGroup];
				for (GraphMSPVertex vertex : forwardVertexMap.values()) {
					int group = vertex.getGroup();
					assert group != -1;
					if (bestVertices[group] == null || bestVertices[group].getScore() < vertex.getScore())
						bestVertices[group] = vertex;
				}
//				LinkedHashMap<Integer, Integer> bestScoreMap = new LinkedHashMap<>();
//				LinkedHashMap<Integer, GraphMSPVertex> bestVertexMap = new LinkedHashMap<>();
//				for (int x = startGroupID; x <= stopGroupID; x++) {
//					bestScoreMap.put(x, 0);
//					bestVertexMap.put(x, null);
//				}
//				for (GraphMSPVertex vertex : forwardVertexMap.values())
//					if (vertex.getScore() > bestScoreMap.get(vertex.getGroup())) {
//						bestScoreMap.put(vertex.getGroup(), vertex.getScore());
//						bestVertexMap.put(vertex.getGroup(), vertex);
//					}
//				
				
				// Output one best result for each group
				for (int y = startGroupID; y <= stopGroupID; y++) {
					GraphMSPVertex bestVertex = bestVertices[y];
					GraphMSPVertex vertex = bestVertex;
					if (vertex == null)
						continue;
					if (bestVertex.getScore() + matchscore < minalignscore)
						continue;
					StringBuilder cigarString = new StringBuilder();
					cigarString.append('M');
					while (vertex.getNextVertex() != null) {
						GraphMSPVertex nextVertex = vertex.getNextVertex();
						for (int x = vertex.qpos + 1; x < nextVertex.qpos; x++)
							cigarString.append('I');
						for (int x = vertex.rpos + 1; x < nextVertex.rpos; x++)
							cigarString.append('D');
						cigarString.append('M');
						vertex = nextVertex;
					}
					
					OptMapResultNode result = new OptMapResultNode(query, new GenomicPosNode(ref, optrefmap.get(ref).refp[bestVertex.rpos], optrefmap.get(ref).refp[vertex.rpos]), 1, bestVertex.rpos + 1, vertex.rpos, bestVertex.qpos + 1, vertex.qpos, new Cigar(cigarString.toString()), bestVertex.getScore(), -1);
					resultList.add(result);
				}
			}
			// Reverse
			{
				// Compute Score
				for (GraphMSPVertex vertex : reverseVertexMap.values())
					vertex.computeScore();
				
				int nextGroup = 1;
				// Compute Group
//				int startGroupID = nextGroup;
//				for (GraphMSPVertex vertex : reverseVertexMap.values())
//					if (vertex.getGroup() == -1)
//						vertex.setGroup(nextGroup++);
//				int stopGroupID = nextGroup - 1;
//				
//				// Initialize best score map for each group
//				LinkedHashMap<Integer, Integer> bestScoreMap = new LinkedHashMap<>();
//				LinkedHashMap<Integer, GraphMSPVertex> bestVertexMap = new LinkedHashMap<>();
//				for (int x = startGroupID; x <= stopGroupID; x++) {
//					bestScoreMap.put(x, 0);
//					bestVertexMap.put(x, null);
//				}
//				for (GraphMSPVertex vertex : reverseVertexMap.values())
//					if (vertex.getScore() > bestScoreMap.get(vertex.getGroup())) {
//						bestScoreMap.put(vertex.getGroup(), vertex.getScore());
//						bestVertexMap.put(vertex.getGroup(), vertex);
//					}
//				
				int startGroupID = nextGroup;
				for (GraphMSPVertex vertex : reverseVertexMap.values())
					if (vertex.getGroup() == -1)
						vertex.setGroup(nextGroup++);
				int stopGroupID = nextGroup - 1;
				
				// Initialize best score map for each group
				GraphMSPVertex[] bestVertices = new GraphMSPVertex[nextGroup];
				for (GraphMSPVertex vertex : reverseVertexMap.values()) {
					int group = vertex.getGroup();
					assert group != -1;
					if (bestVertices[group] == null || bestVertices[group].getScore() < vertex.getScore())
						bestVertices[group] = vertex;
				}

				// Output one best result for each group
				for (int y = startGroupID; y <= stopGroupID; y++) {
					GraphMSPVertex bestVertex = bestVertices[y];
					GraphMSPVertex vertex = bestVertex;
					if (vertex == null)
						continue;
					if (bestVertex.getScore() + matchscore < minalignscore)
						continue;
					StringBuilder cigarString = new StringBuilder();
					cigarString.append('M');
					while (vertex.getNextVertex() != null) {
						GraphMSPVertex nextVertex = vertex.getNextVertex();
						for (int x = nextVertex.qpos + 1; x < vertex.qpos; x++)
							cigarString.append('I');
						for (int x = vertex.rpos + 1; x < nextVertex.rpos; x++)
							cigarString.append('D');
						cigarString.append('M');
						vertex = nextVertex;
					}

					OptMapResultNode result = new OptMapResultNode(query, new GenomicPosNode(ref, optrefmap.get(ref).refp[bestVertex.rpos], optrefmap.get(ref).refp[vertex.rpos]), -1, bestVertex.rpos + 1, vertex.rpos, bestVertex.qpos, vertex.qpos + 1, new Cigar(cigarString.toString()), bestVertex.getScore() + matchscore, -1);
					resultList.add(result);
				}
			}
		}
		return resultList;
	}

	
	@Override
	public OMFMMapper copy() {
		OMFMMapper mapper = new OMFMMapper(optrefmap);
		super.setCopyMapperParameters(mapper);
		mapper.setParameters(measure, scalingRange, matchscore, falseppenalty, falsenpenalty, rfalselimit, qfalselimit, cfalselimit, minalignscore);
		return mapper;
	}

	public static void assignOptions(ExtendOptionParser parser, int level) {

		Mapper.assignOptions(parser, level);

		parser.addHeader("OMFMMapper Options", level);
		AlignmentOptions.assignErrorToleranceOptions(parser);
		AlignmentOptions.assignScoreOptions(parser);
		parser.accepts("rfalselimit", "Max consecutive false signals on reference").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		parser.accepts("qfalselimit", "Max consecutive false signals on query").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		parser.accepts("cfalselimit", "Max consecutive false signals on both reference and query").withRequiredArg().ofType(Integer.class).defaultsTo(5);
		parser.accepts("minalignscore", "Minimum score at alignment stage").withRequiredArg().ofType(Integer.class).defaultsTo(20);
		SeedDatabase.assignOptions(parser, level + 1);
	}

	public static void main(String[] args) throws IOException, MapperConstructionException {
		Mapper.standardMapperProcedure(args, OMFMMapper.class);
	}
}
class FMEdge implements Comparable<FMEdge> {
	private final String name;
	private final int pos1;
	private final int pos2;
	private final double length;
	public FMEdge(String name, int pos1, int pos2, double length) {
		assert pos1 < pos2 || (pos1 == -1 && pos2 == -1);
		assert name != null;
		assert length >= 0;
		this.name = name;
		this.pos1 = pos1;
		this.pos2 = pos2;
		this.length = length;
	}
	
	public int getError() {
		assert pos2 - pos1 - 1 >= 0 || (pos1 == -1 && pos2 == -2);
		return pos2 - pos1 - 1;
	}
	public double getLength() {
		return length;
	}
	
	public int getPos1() {
		return pos1;
	}

	public int getPos2() {
		return pos2;
	}

	@Override
	public int compareTo(FMEdge edge) {
		return Double.compare(this.length, edge.length);
	}
	public static FMEdge newDummyEdge(double length) {
		return new FMEdge("Dummy", -1, -1, length);
	}
}

class GraphMSPVertex extends MatchingSignalPair {
	private GraphMSPVertex bestNextVertex;
	private List<GraphMSPVertex> potentialNextVertices;
	private List<Integer> potentialNextMspTransitionScores;
	private int group = -1;
	private int score;
	private boolean processedState = false;
	
	public GraphMSPVertex(int rpos, int qpos) {
		super(rpos, qpos);
		potentialNextVertices = new ArrayList<>();
		potentialNextMspTransitionScores = new ArrayList<>();
	}
	
	public void addPotentialNextVertex(GraphMSPVertex vertex, int transitionScore) {
		this.potentialNextVertices.add(vertex);
		this.potentialNextMspTransitionScores.add(transitionScore);
		this.processedState = false;
	}

	public int getScore() {
		if (!processedState)
			throw new IllegalStateException();
		return score;
	}
	public int getGroup() {
		if (!processedState)
			throw new IllegalStateException();
		if (group == -1 && bestNextVertex != null)
			this.group = bestNextVertex.getGroup();
		return group;
	}
	public void setGroup(int group) {
		if (this.group != -1 && this.group != group)
			throw new IllegalStateException();
		if (this.group != group) {
			this.group = group;
			if (bestNextVertex != null)
				bestNextVertex.setGroup(group);
		}
	}
	public void computeScore() {
		if (processedState)
			return;
		
		assert potentialNextVertices.size() == potentialNextMspTransitionScores.size();
		int bestScore = 0;
		GraphMSPVertex bestNextVertex = null;
		for (int i = 0; i < potentialNextVertices.size(); i++) {
			GraphMSPVertex vertex = potentialNextVertices.get(i);
			vertex.computeScore();
			int transitionScore = potentialNextMspTransitionScores.get(i);
			if (vertex.getScore() + transitionScore > bestScore) {
				bestScore = vertex.getScore() + transitionScore;
				bestNextVertex = vertex;
			}
		}
		
		this.bestNextVertex = bestNextVertex;
		this.score = bestScore;
		processedState = true;
	}
	
	public GraphMSPVertex getNextVertex() {
		return bestNextVertex;
	}
}