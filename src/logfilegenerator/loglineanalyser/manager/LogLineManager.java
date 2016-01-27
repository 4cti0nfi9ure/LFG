/*
*LogFileGenerator (LFG) allows generating Network Event Sequence (NES) data of any length based on a small piece of real log data.
*
*Copyright (C) 2016 Markus Wurzenberger
*
*This program is free software; you can redistribute it and/or modify
*it under the terms of the GNU General Public License as published by
*the Free Software Foundation; either version 3 of the License, or
*(at your option) any later version.
*
*This program is distributed in the hope that it will be useful,
*but WITHOUT ANY WARRANTY; without even the implied warranty of
*MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*GNU General Public License for more details.
*
*You should have received a copy of the GNU General Public License
*along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package logfilegenerator.loglineanalyser.manager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import logfilegenerator.generatelf.modules.TransitionMatrix;
import logfilegenerator.loglineanalyser.modules.Cluster;
import logfilegenerator.loglineanalyser.modules.LogLine;

/*
 * This class stores log lines in an array list
 */

public class LogLineManager {
	
	private ArrayList<LogLine> logLineList;

	public LogLineManager() {
		super();
		this.logLineList = new ArrayList<LogLine>();
	}

	public void importLogLines(String filename, ClusterManager clusterList, LogLineManager outlierList, Map<String, LogLine> processedLogLines, TransitionMatrix transitionMatrix, int removedBitsInt,SimpleDateFormat sdf, Map<Integer, Cluster> mapIdCluster, ArrayList<Long> timeStampDistributionOutlier) {
		System.out.println("Start importing log lines, finding the most accurate cluster and calculating the transition matrix...");
		BufferedReader br;
		try{
			//read log file line by line
			br = new BufferedReader(new FileReader(filename));
			String line;
			LogLine logLine;
			int numberOfLines;
			int lineNumber = 0;
			int actualCluster = 0;
			int nextCluster = 0;
			Date logDateTime = null;
			Date actualTime = null;
			Date nextTime = null;
			long timeDiff;
			double[][] tmpTransitionMatrix = new double[clusterList.getClusterList().size()+1][clusterList.getClusterList().size()+1];
			
			
			//count the number of lines in the input file
		    InputStream is = new BufferedInputStream(new FileInputStream(filename));
		    try {
		        byte[] c = new byte[1024];
		        numberOfLines = 0;
		        int readChars = 0;
		        boolean endsWithoutNewLine = false;
		        while ((readChars = is.read(c)) != -1) {
		            for (int i = 0; i < readChars; ++i) {
		                if (c[i] == '\n')
		                    ++numberOfLines;
		            }
		            endsWithoutNewLine = (c[readChars - 1] != '\n');
		        }
		        if(endsWithoutNewLine) {
		            ++numberOfLines;
		        } 
		    } finally {
		        is.close();
		    }
			
		    System.out.println("linenumber input: "+numberOfLines+"\n");
		    
			while((line = br.readLine()) != null){
				lineNumber+=1;
				if(lineNumber%10000==0)
					System.out.println("Already processed lines: "+lineNumber);
								
				//remove time stamp, because they are not considered during clustering
				try
			       {
					logDateTime = sdf.parse(line.substring(0,15)); // the first 16 characters belong to the datetime

			        // System.out.println("date : "+this.logDateTime);
			       }
			       catch (ParseException ex)
			       {
			    	   System.out.println("Exception "+ex);
			        }
				line = line.substring(removedBitsInt).trim();
				
				//look up in the hashmap, if a log line, with the same content has already been processed.
				//If so, copy the attributes.
				//NOTE: For some reason, the hashcode function gives an incorrect output. Therefore use the string itself as key.
				if(processedLogLines.containsKey(line)){
					logLine = processedLogLines.get(line);
					//add the new line to the clusters
					for (int i = 0; i < logLine.getClusters().getClusterList().size(); i++){
						logLine.getClusters().getClusterList().get(i).getLogLines().getLogLineList().add(logLine);
					}
					//if the line is an outlier, there is no most accurrate cluster.
					if(logLine.isOutlier()==false){
						logLine.getMostAccurateCluster().getMostAccurateLogLines().getLogLineList().add(logLine);	
					}
					logLine.setTimeStamp(logDateTime);
				}
				else{
					logLine = new LogLine(line,logDateTime);
					//store clusterValues and how often they occur in a hash map
					Map<Integer,Integer> clusterValueFrequency = new HashMap<Integer,Integer>();
					//store clusters and their clusterValue in a hash map
					Map<Cluster, Integer> clusterClusterValue = new HashMap<Cluster,Integer>();
					//store all occurred clusterValues in an array list
					ArrayList<Integer> clusterValueList = new ArrayList<Integer>();
					
					
					for (Cluster cluster : clusterList.getClusterList()){
						
						//check which clusters the log line matches
						if (logLine.getLine().matches(cluster.getRegex())){
							cluster.getLogLines().logLineList.add(logLine);
							logLine.getClusters().getClusterList().add(cluster);
							logLine.setOutlier(false);
							
							if(clusterValueFrequency.containsKey(cluster.getClusterValue())){
								clusterValueFrequency.put(cluster.getClusterValue(), clusterValueFrequency.get(cluster.getClusterValue())+1);				
								}
							else {
								clusterValueFrequency.put(cluster.getClusterValue(), 1);
							}
							
							clusterClusterValue.put(cluster, cluster.getClusterValue());
							
							if(!(clusterValueList.contains(cluster.getClusterValue()))){
								clusterValueList.add(cluster.getClusterValue());
							}

						}
					}
					
					//if the log line matches at least one cluster, the most accurate cluster has to be found.
					if (clusterValueList.size()>0){
						logLine.findMostAccurateCluster(clusterValueFrequency,clusterClusterValue,clusterValueList);						
					}
					//add the line to the hashmap, which stores the already processed lines.
					processedLogLines.put(line,logLine);
				}

				this.logLineList.add(logLine);
				if(logLine.isOutlier()){
					outlierList.getLogLineList().add(logLine);
				}
				
				//calculate transitionMatrix; start with second line, because there is no transition to the first line.
				if(lineNumber>1){
					//calculate the time difference between the last two log lines
					nextTime = logDateTime;
					timeDiff = nextTime.getTime()-actualTime.getTime();
					if(mapIdCluster.containsKey(actualCluster)){
						mapIdCluster.get(actualCluster).getTimeStampDistribution().add(timeDiff);
					}
					else{
						timeStampDistributionOutlier.add(timeDiff);
					}

					//if the considered log line is an outlier it is not corresponding to any cluster. Therefore it is added to the last cluster, 
					//which stores all outliers (c.f. main)
					//every line of the transitionMatrix stores the transitions from cluster i to cluster j.
					if(logLine.isOutlier()){
						nextCluster = clusterList.getClusterList().size();
						}
					else{
						nextCluster = logLine.getMostAccurateCluster().getClusterID();
					}
					tmpTransitionMatrix[actualCluster][nextCluster]+=1;
				}
				
				actualTime=logDateTime;
				if(logLine.isOutlier()){
					actualCluster = clusterList.getClusterList().size();
				}
				else{
					actualCluster = logLine.getMostAccurateCluster().getClusterID();
				}
				transitionMatrix.setTransMatrix(tmpTransitionMatrix);
			}
						
		} catch(FileNotFoundException e){
			e.printStackTrace();			
		} catch(IOException e){
			e.printStackTrace();
		}


		System.out.println("Finished importing log lines, finding the most accurate cluster and calculating the transition matrix...");
	}

	public ArrayList<LogLine> getLogLineList() {
		return logLineList;
	}

	public void setLogLineList(ArrayList<LogLine> logLineList) {
		this.logLineList = logLineList;
	}

}
