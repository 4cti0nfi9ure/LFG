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

package logfilegenerator.loglineanalyser.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import logfilegenerator.loglineanalyser.manager.ClusterManager;

/*
 * This class stores log lines
 */

public class LogLine {
	
	private String line;
	private Date timeStamp;
	private ClusterManager clusters;
	private Cluster mostAccurateCluster;
	private boolean isOutlier;

	public LogLine(String line, Date logDateTime) {
		super();
		this.timeStamp = logDateTime;
		this.line = line;
		this.clusters = new ClusterManager();
		this.isOutlier = true;
		this.mostAccurateCluster = null;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}

	public ClusterManager getClusters() {
		return clusters;
	}

	public void setClusters(ClusterManager clusters) {
		this.clusters = clusters;
	}

	public boolean isOutlier() {
		return isOutlier;
	}

	public void setOutlier(boolean isOutlier) {
		this.isOutlier = isOutlier;
	}
	
	public void findMostAccurateCluster(
			Map<Integer, Integer> clusterValueFrequency,
			Map<Cluster, Integer> clusterClusterValue,
			ArrayList<Integer> clusterValueList) {
		//since the last entry in the clusterValueList is the largest, this is the first candidate for 
		//the most accurate cluster.
		int index = clusterValueList.size();
		int clusterValue;
		int supersetCount;
	
		//sort the clusterValueList, beginning with the smallest value.
		Collections.sort(clusterValueList);
		
		//Since one log line can match two clusters with the same clusterValue, the candidate's clusterValue 
		//frequency has to be checked. If there is a second cluster, which matches the log line, with the same 
		//clusterValue as the candidate, a cluster with a lower clusterValue has to be the most accurate cluster, 
		//because, these two clusters have the same super cluster.
		do {
			index -= 1;
			if (index<0){
				this.setOutlier(true);
				return;
			}
			if(clusterValueFrequency.get(clusterValueList.get(index))>1){
				ArrayList<Cluster> candidates = new ArrayList<Cluster>();
				clusterValue=clusterValueList.get(index);
				for(Cluster cluster : clusterClusterValue.keySet()){
					if(cluster.getClusterValue()==clusterValue){
						candidates.add(cluster);
					}
				}
				//check if one of the clusters, with the same clusterValue is a supercluster of the others. for example: the clusters with 
				//the descriptions cluster 1 and cluster 10 have the same cluster value, but cluster 10 is a supercluster of cluster 1.
				//NOTE: should be redundant since, solved problem in regex generation, if the description doesn't end with a wild card -> last symbolo $
				for (int i = 0; i <candidates.size(); i++){
					supersetCount = 0;
					for (int j = 0; j < candidates.size(); j++){						
						if (candidates.get(i).getDescr().trim().contains(candidates.get(j).getDescr().trim())){
							supersetCount += 1;
						}
					}
					if(supersetCount==candidates.size()){
						this.setMostAccurateCluster(candidates.get(i));
						candidates.get(i).getMostAccurateLogLines().getLogLineList().add(this);
						return;						
					}
				}
				
			}
		}
		while(clusterValueFrequency.get(clusterValueList.get(index))>1);
		
		//find the cluster, which corresponds to the clusterValue
		for (Cluster cluster : clusterClusterValue.keySet()){
			if (clusterClusterValue.get(cluster) == clusterValueList.get(index)){
				this.setMostAccurateCluster(cluster);
				cluster.getMostAccurateLogLines().getLogLineList().add(this);
				return;
			}
		}
	}

	public Cluster getMostAccurateCluster() {
		return mostAccurateCluster;
	}

	public void setMostAccurateCluster(Cluster mostAccurateCluster) {
		this.mostAccurateCluster = mostAccurateCluster;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	

}
