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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logfilegenerator.loglineanalyser.modules.Cluster;
import logfilegenerator.loglineanalyser.modules.LogLine;

/*
 * This class stores clusters in an array list
 */

public class ClusterManager {

	private ArrayList<Cluster> clusterList;

	public ClusterManager() {
		super();
		this.clusterList = new ArrayList<Cluster>();
	}

	public void importCluster(String clusterFile, Map<Integer, Cluster> mapIdCluster) {
		
		System.out.println("Start importing SLCT clusters...");
		
		try{
			//read SLCT clusters line by line
			BufferedReader br = new BufferedReader(new FileReader(clusterFile));
			String line;
			int lineCount = 0;
			String cluster = "";
			int clusterCount = mapIdCluster.size();
			Cluster tempCluster;
			//int support = 0;
			
			while((line = br.readLine()) != null){
				//only every third line in the output of SLCT represents a cluster. The clusters are represented 
				//by the lines, which fulfill: linenumber moc 3 = 0.
				if(lineCount%3 == 0){
					cluster = line;
					System.out.println("cluster: "+clusterCount);
					System.out.println(cluster);
				}				
				//lines, which fulfill linenumber mod 3 0= 1 store the support value
				/*else if(lineCount%3 == 1){
					support=Integer.valueOf(line.split(" ")[1]);
				}*/
				//lines, which fulfill linenumber mod 3 == 2 are empty.
				//Therefore the cluster can be added to the clusterList
				else if(lineCount%3 == 2){
					tempCluster=new Cluster(clusterCount, cluster);
					this.clusterList.add(tempCluster);
					mapIdCluster.put(tempCluster.getClusterID(), tempCluster);
					
					clusterCount += 1;
				}
				
				lineCount += 1;
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished importing SLCT clusters...");
		
	}
	
	public ArrayList<Cluster> getClusterList() {
		return clusterList;
	}

	public void setClusterList(ArrayList<Cluster> clusterList) {
		this.clusterList = clusterList;
	}

	public void generateWildcardContent() {
		System.out.println("Start generating wildcard content...");
		for (Cluster cluster : this.clusterList){
			String wildcardContent[][]; // stores the wildcard content of every log line, which has the considered cluster as most accurate cluster 
			String clusterDescription;
			int loglineCount = 0;
			String line ="";
			
			wildcardContent = new String[cluster.getNumberOfWildcards()][cluster.getMostAccurateLogLines().getLogLineList().size()];
			//replace the wildcard symbol, with the pattern, which is used to find the content in the logline, which is replaced by the wildcard.
			clusterDescription = cluster.getDescr();
			clusterDescription = clusterDescription.replaceAll("(\\?)", "\\\\$1");
			clusterDescription = clusterDescription.replaceAll("([\\[\\]\\(\\)\\+\\|\\$\\^\\{\\}\\.\\*])", "\\\\$1");
			clusterDescription = clusterDescription.replace("\\\\", "\\\\");
			clusterDescription = clusterDescription.replaceAll("\\<\\\\\\*", "(.*?)");
			clusterDescription = clusterDescription.trim();			
			
			for(LogLine logline : cluster.getMostAccurateLogLines().getLogLineList()){
				line = logline.getLine();		
				Matcher m = Pattern.compile(clusterDescription).matcher(line);
				for(int i = 0; i<cluster.getNumberOfWildcards(); i++){
					if(m.matches()){
						wildcardContent[i][loglineCount] = m.group(i+1);
					}
				}
				loglineCount +=1;
			}
			cluster.setWildcardContent(wildcardContent);
		}
		System.out.println("Finished generating wildcard content...");
	}
	
}
