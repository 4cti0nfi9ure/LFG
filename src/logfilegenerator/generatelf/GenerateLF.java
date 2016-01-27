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

package logfilegenerator.generatelf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

import logfilegenerator.generatelf.modules.TransitionMatrix;
import logfilegenerator.loglineanalyser.manager.LogLineManager;
import logfilegenerator.loglineanalyser.modules.Cluster;


public class GenerateLF {
	
	public static void markovChainGenenerator(int generatedLogFileSizeInt, TransitionMatrix transitionMatrix, Random r) throws IOException{
		
		System.out.println("Start generating Markov Chain...");
		
		//stores state Space
		String [] stateSpace = new String[transitionMatrix.getDim()];
		double[] startDistribution = new double[transitionMatrix.getDim()];
		double[][] cumSums = new double[transitionMatrix.getDim()][transitionMatrix.getDim()];
		double[] cumSumStartDistribution = new double[transitionMatrix.getDim()];
		double total = 0.0;
		double totalStartDistribution = 0.0;
		double randomNumber;
		int index = 0;
		
		for (int i=0;i<transitionMatrix.getDim();i++){
			//check if tansitionMatrix is a transition matrix
			if(transitionMatrix.getRowSums()[i]==0.0 && transitionMatrix.getColSums()[i]!=0.0 ){
				System.out.println("ALARM!! NO TRANSITION MATRIX");
				System.exit(0);
				return;
			}
			stateSpace[i] = "Cluster "+Integer.toString(i);
			//calculate the distribution for the first log line
			startDistribution[i] = transitionMatrix.getRowSums()[i]/transitionMatrix.getElements();
			total = 0.0;
			//calculate cumulative sums for every row
			totalStartDistribution+=startDistribution[i];
			cumSumStartDistribution[i]=totalStartDistribution;
			for(int j=0;j<transitionMatrix.getDim();j++){
				total+=transitionMatrix.getStochasticTransMatrix()[i][j];
				cumSums[i][j]=total;
			}
		}
		
		//generate markov chain
		PrintStream ps = new PrintStream(new File ("NESData.txt"));
		randomNumber = r.nextDouble();
		if(randomNumber>=0 && randomNumber<cumSumStartDistribution[0]){
			index = 0;
		}
		else{
			for(int i = 1;i<transitionMatrix.getDim();i++){
				if(randomNumber>=cumSumStartDistribution[i-1] && randomNumber<cumSumStartDistribution[i]){
					index=i;
					break;
				}
			}
		}
		ps.print(stateSpace[index]+"\n");		
		
		for (int j=0; j<generatedLogFileSizeInt-1;j++){
			randomNumber = r.nextDouble();
			if(randomNumber>=0 && randomNumber<cumSums[index][0]){
				index = 0;
			}
			else{
				for(int i = 1;i<transitionMatrix.getDim();i++){
					if(randomNumber>=cumSums[index][i-1] && randomNumber<cumSums[index][i]){
						index=i;
						break;
					}
				}
			}
			ps.print(stateSpace[index]+"\n");
		}
		ps.close();
		
		
		System.out.println("Finished generating Markov Chain...");
	}

	public static void fillLogFile(Map<Integer, Cluster> mapIdCluster, Date sdfStartDate, SimpleDateFormat sdf, ArrayList<Long> timeStampDistributionOutlier, Random r, LogLineManager outlierList) {
		System.out.println("Start filling generated log file with content...");
		//Read NESData file (output of markovChainGenerator) and replace wildcards with log line content
		try{
			BufferedReader br = new BufferedReader(new FileReader("NESData.txt"));
			PrintStream ps = new PrintStream(new File ("filledNESData.log"));
			String line;
			String text;
			int clusterID;
			int lineCount = 0;
			long plus;
			Date actualTime = sdfStartDate;
			String content;
			
			while((line = br.readLine()) != null){
				//extract cluster ID from markovChainGenerator output
				clusterID = Integer.parseInt(line.substring(8));
				
				if(clusterID == mapIdCluster.size()){
					plus = calcTimeBetween(timeStampDistributionOutlier,r);
				}
				else{
					plus = calcTimeBetween(mapIdCluster.get(clusterID).getTimeStampDistribution(),r);
				}
				
				lineCount+=1;
				if(lineCount%10000==0){
					System.out.println("filled lines: "+lineCount);
				}
				text = sdf.format(actualTime);
				if(clusterID!=mapIdCluster.size()){
					content = mapIdCluster.get(clusterID).getDescr();
					for(int i = 0 ; i<mapIdCluster.get(clusterID).getNumberOfWildcards(); i++){
						int nextRand = r.nextInt(mapIdCluster.get(clusterID).getWildcardContent()[i].length);
						content = content.replaceFirst("\\<\\*", Matcher.quoteReplacement(mapIdCluster.get(clusterID).getWildcardContent()[i][nextRand]));
					}
				}
				else{
					content = outlierList.getLogLineList().get(r.nextInt(outlierList.getLogLineList().size())).getLine();
				}
				ps.print(text+" "+content+"\n");
				actualTime = new Date(actualTime.getTime() + plus);
			}
			br.close();
			ps.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Finished filling generated log file with content...");
	}

	//does the same like a random number generator based on an ecdf 
	private static long calcTimeBetween(ArrayList<Long> timeDists, Random r) {
		Long randTimeDist = timeDists.get(r.nextInt(timeDists.size()));
		return randTimeDist;
	}

}
