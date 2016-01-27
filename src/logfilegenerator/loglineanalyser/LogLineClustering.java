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

package logfilegenerator.loglineanalyser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

public class LogLineClustering {
	
	public static void preprocessInput(String inputFile, String preprocessedInput){
		
		System.out.println("Start preprocessing input...");
		
		//remove \r
		try{
			BufferedReader br = new BufferedReader(new FileReader(inputFile));
			PrintStream ps = new PrintStream(new File (preprocessedInput));
			String line;
			String newText;
			int lineCount = 0;
			while((line = br.readLine()) != null){
				//replace multiple line breaks
				newText = line.replaceAll("[\r]","")+"\n";
				//replace multiple spaces and adds a new line breakk
				newText = newText.trim().replaceAll(" +", " ")+"\n";
				lineCount+=1;
				if(lineCount%10000==0)
					System.out.println("Preprocessed lines: "+lineCount);
				ps.print(newText);
			}
			br.close();
			ps.close();
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		System.out.println("Finished preprocessing input...");
	}
	
	public static void clusterLogLines(String removedBits, String supportValue, String preprocessedInput, String clusterFile){
		
		System.out.println("Start clustering...");
		
		//calculate clusters with SLCT
    	try {
    		//Windows
			Process process = new ProcessBuilder("cmd.exe","/C","slct.exe" ,"-b",removedBits,"-j","-r","-o","outliers.txt","-s",supportValue,preprocessedInput,">",clusterFile).start();
			try {
				if (process.waitFor()==0) {
					System.out.println("Clusters calculated successful with SLCT!");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
