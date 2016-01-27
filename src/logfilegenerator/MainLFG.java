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

package logfilegenerator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import logfilegenerator.generatelf.GenerateLF;
import logfilegenerator.generatelf.modules.TransitionMatrix;
import logfilegenerator.loglineanalyser.LogLineClustering;
import logfilegenerator.loglineanalyser.manager.ClusterManager;
import logfilegenerator.loglineanalyser.manager.LogLineManager;
import logfilegenerator.loglineanalyser.modules.Cluster;
import logfilegenerator.loglineanalyser.modules.LogLine;



public class MainLFG {
	
	//stores clusters
	private static ClusterManager clusterList = new ClusterManager();
	private static Map<Integer,Cluster> mapIdCluster = new HashMap<Integer,Cluster>();
	//stores log lines
	private static LogLineManager logLineList = new LogLineManager();
	private static Map<String,LogLine> processedLogLines = new HashMap<String,LogLine>();
	//stores outliers
	private static LogLineManager outlierList = new LogLineManager();

	//stores transitions
	private static TransitionMatrix transitionMatrix;
	//date format for storing the time a log line occurred
	private static SimpleDateFormat sdf = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH);
	private static ArrayList<Long> timeStampDistributionOutlier = new ArrayList<Long>();
	private static Random r = new Random();

	public static void main(String[] args) throws ParseException, IOException {
		long startTime = System.currentTimeMillis();
		System.out.println("Starting...");
		
		String inputFile = args[0];
		String preprocessedInput =args[1];
		String removedBits = args[2];
		int removedBitsInt = Integer.parseInt(removedBits);
		String supportValue = args[3];
		int supportValueInt = Integer.parseInt(supportValue);
		String clusterFile = args[4];
		String generatedLogFileSize = args[5];
		int generatedLogFileSizeInt = Integer.parseInt(generatedLogFileSize);
		String startDate = args[6];
		Date sdfStartDate = sdf.parse(startDate);
		
		//preprocess input
		LogLineClustering.preprocessInput(inputFile, preprocessedInput);
		
		//run log line clustering
		LogLineClustering.clusterLogLines(removedBits, supportValue, preprocessedInput, clusterFile);
		
		//import clusters from SLCT Output
		clusterList.importCluster(clusterFile,mapIdCluster);
		
		//check the outlier file for cluster candidates
	    try
	    { 
	       Files.copy(Paths.get("outliers.txt"),Paths.get("outliersInput.txt"), StandardCopyOption.REPLACE_EXISTING);
	       System.out.println("File Copied");
	    }
	    catch(IOException e)
	    {
	        e.printStackTrace();
	    }
	    
		while(getNumberOfLines("outliers.txt")>=supportValueInt && getNumberOfLines(clusterFile)>=3){
			LogLineClustering.clusterLogLines(removedBits, supportValue, "outliersInput.txt", clusterFile);
			clusterList.importCluster(clusterFile,mapIdCluster);
		    try
		    { 
		       Files.copy(Paths.get("outliers.txt"),Paths.get("outliersInput.txt"), StandardCopyOption.REPLACE_EXISTING);
		       //System.out.println("File Copied");
		    }
		    catch(IOException e)
		    {
		        e.printStackTrace();
		    }
		}
		
		//initialize transitionMatrix
		//one cluster is added to store the transition probabilities of the outliers
		transitionMatrix = new TransitionMatrix(clusterList.getClusterList().size()+1);
		
		//import log lines from log file and assign them to clusters
		logLineList.importLogLines(preprocessedInput,clusterList,outlierList,processedLogLines,transitionMatrix,removedBitsInt,sdf,mapIdCluster,timeStampDistributionOutlier);
		
		//calculate stochsticTransMatrix, which stores the transition probabilities
		transitionMatrix.calculateStochsticTransMatrix();
		
		//printing some output
		System.out.println("Most accurate clusters and number of log lines:");
		for(Cluster cluster : clusterList.getClusterList()){
			System.out.println("Cluster "+cluster.getClusterID()+": "+cluster.getMostAccurateLogLines().getLogLineList().size());
		}
		
		
		System.out.println("Number of outliers: "+outlierList.getLogLineList().size());
		System.out.println("Number of clusters: "+clusterList.getClusterList().size());
		
		//print outliers to file
		try (
				PrintStream outliersLFG = new PrintStream(new File ("outliersLFG.txt"));
				PrintStream clustersLFG = new PrintStream(new File ("clustersLFG.txt"));
				){	
			for(LogLine outlierLine : outlierList.getLogLineList()){
				outliersLFG.println(outlierLine.getLine());
			}
			for (int i = 0; i<clusterList.getClusterList().size();i++){
				clustersLFG.println("cluster "+i+": "+clusterList.getClusterList().get(i).getDescr());				
			}
			outliersLFG.close();
			clustersLFG.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//generating log file
		GenerateLF.markovChainGenenerator(generatedLogFileSizeInt,transitionMatrix,r);
		clusterList.generateWildcardContent();
		GenerateLF.fillLogFile(mapIdCluster,sdfStartDate,sdf,timeStampDistributionOutlier,r,outlierList);
		
		//clean up
		File fileA = new File("outliers.txt");
		File fileB = new File("outliersInput.txt");
		File fileC = new File(preprocessedInput);
		File fileD = new File(clusterFile);
		
		fileA.delete();
		fileB.delete();
		fileC.delete();
		fileD.delete();
		
		System.out.println("...Finished!");
	    long stopTime = System.currentTimeMillis();
	    double elapsedTime = (stopTime - startTime)*0.001;
	    System.out.println("TIME: "+elapsedTime+"s");
	    
   
	}
	
	//count the number of lines in the input file
	private static int getNumberOfLines(String filename) throws IOException{
        int numberOfLines = 0;
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
	    return numberOfLines;
	}

}
