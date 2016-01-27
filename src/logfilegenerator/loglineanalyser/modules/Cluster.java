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

import org.apache.commons.lang3.StringUtils;

import logfilegenerator.loglineanalyser.manager.LogLineManager;

/*
 * This class stores cluster
 */

public class Cluster {
	
	private int clusterID;
	private String descr;
	private String regex;
	private int clusterValue;
	private LogLineManager logLines;
	private LogLineManager mostAccurateLogLines;
	private ArrayList<Long>  timeStampDistribution;
	private String[][] wildcardContent;
	private int numberOfWildcards;
	
	public Cluster(int clusterCount, String cluster) {
		super();
		this.setClusterID(clusterCount);
		this.setDescr(cluster);
		this.setRegex(generateRegexFromDescription(cluster));
		this.setClusterValue(calculateClusterValue(cluster));
		this.logLines = new LogLineManager();
		this.mostAccurateLogLines = new LogLineManager();
		this.timeStampDistribution = new ArrayList<Long>();
		this.numberOfWildcards = StringUtils.countMatches(this.getDescr(), "<*");
	}
	
	//calculate the clusterValue, which specifies, how many frequent words define the cluster
	private int calculateClusterValue(String cluster) {
		
		int wordCount = 0;
		//split the cluster description at every space
		String[] s = cluster.split(" ");
		
		//iterate through the string list
		for(int i = 0; i<s.length; i++){
			//if s[i] is not equal to the wildcard symbol, raise the wordCount
			if (s[i].equals("<*")){
				continue;
			}
			else{
				wordCount += 1;
			}
		}
		System.out.println("clusterValue: "+wordCount+"\n");
		return wordCount;
	}
	

	//generate a regular expression from the cluster description
	private String generateRegexFromDescription(String cluster) {
		
		StringBuffer sbuf = new StringBuffer();
		
		//EDIT: better first remove the time stamp of the logline, as SLCT does. do not allow a reluctant 
		//number of characters in the beginning, because in this case, the regular expression will also match 
		//lines, where the significant words are found on different positions.
		//NOTE: SLCT cares about the position of a word
		
		//store the cluster description in a List, splitted by the SLCT wildcard symbol <*
		String[] s = descr.split("\\<\\*");
		
		//iterate through the string list
		for(int i = 0; i<s.length; i++){
			s[i] = s[i].replaceAll("(\\?)", "\\\\$1");
			s[i] = s[i].replaceAll("([\\[\\]\\(\\)\\+\\|\\$\\^\\{\\}\\.\\*])", "\\\\$1");
			s[i] = s[i].replace("\\\\", "\\\\");
			
			sbuf.append(s[i]);
			//replace SLCT wildcard symbol
			//NOTE: be careful if a line starts with the wildcard symbol
			if(i==0 && s[i].equals("") && !(s[i+1].substring(0, 1).equals(" "))){
				sbuf.append("\\S*");
			}
			else if(i==0 && s[i].equals(""))
				sbuf.append("\\S+");
			else if(!(i+1 == s.length)){
				//\S*? or \S* or \S+ ?
				//EDIT: has to be \S+, because SLCT does not consider a space as a word and \S* allows spaces,
				//because then the next "empty" string is considered as a word.
				//EDIT: if in SLCT the refine option is used, some wildcard symbols are extended with characters. In this case, the wildcard 
				//symbol can be also replaced by an empty string.
				if(!(s[i].substring(s[i].length() - 1).equals(" ")) || !(s[i+1].substring(0, 1).equals(" "))){
					sbuf.append("\\S*");
				}
				else{
					sbuf.append("\\S+");
				}
			}
		}
		//delete the last character, because it is a space. Therefore, \S+ .* is returned instead of \S+.*.
		sbuf.deleteCharAt(sbuf.length()-1);
		if(descr.trim().substring(descr.length()-3).equals("<*")){
			sbuf.append(".*");
		}
		else{
			//if the cluster description doesn't end with a wild card, the $ symbol makes sure, that the last symbol of the log line, which matches
			//the cluster, is equal to the last symbol of the regular expression
			sbuf.append("$.*");
		}
		

		System.out.println("generated regex: " + sbuf.toString());
			
		return sbuf.toString();
	}	

	public int getClusterID() {
		return clusterID;
	}
	public void setClusterID(int clusterID) {
		this.clusterID = clusterID;
	}
	public String getDescr() {
		return descr;
	}
	public void setDescr(String descr) {
		this.descr = descr;
	}
	public String getRegex() {
		return regex;
	}
	public void setRegex(String regex) {
		this.regex = regex;
	}
	public int getClusterValue() {
		return clusterValue;
	}
	public void setClusterValue(int clusterValue) {
		this.clusterValue = clusterValue;
	}

	public LogLineManager getLogLines() {
		return logLines;
	}

	public void setLogLines(LogLineManager logLines) {
		this.logLines = logLines;
	}

	public LogLineManager getMostAccurateLogLines() {
		return mostAccurateLogLines;
	}

	public void setMostAccurateLogLines(LogLineManager mostAccurateLogLines) {
		this.mostAccurateLogLines = mostAccurateLogLines;
	}

	public ArrayList<Long> getTimeStampDistribution() {
		return timeStampDistribution;
	}

	public void setTimeStampDistribution(ArrayList<Long> timeStampDistribution) {
		this.timeStampDistribution = timeStampDistribution;
	}

	public String[][] getWildcardContent() {
		return wildcardContent;
	}

	public void setWildcardContent(String[][] wildcardContent) {
		this.wildcardContent = wildcardContent;
	}

	public int getNumberOfWildcards() {
		return numberOfWildcards;
	}

	public void setNumberOfWildcards(int numberOfWildcards) {
		this.numberOfWildcards = numberOfWildcards;
	}

}
