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

package logfilegenerator.generatelf.modules;

public class TransitionMatrix {
	
	double[][] transMatrix;
	double[][] stochasticTransMatrix;
	int dim;
	double[] rowSums;
	double[] colSums;
	double elements;

	public TransitionMatrix(int size) {
		super();
		this.transMatrix = new double[size][size];
		this.stochasticTransMatrix = new double [size][size];
		this.rowSums = new double[size];
		this.colSums = new double[size];
		this.dim = size;
	}

	public double[][] getTransMatrix() {
		return transMatrix;
	}

	public void setTransMatrix(double[][] transitionMatrix) {
		this.transMatrix = transitionMatrix;
	}

	public int getDim() {
		return dim;
	}

	public void setDim(int dim) {
		this.dim = dim;
	}

	public double[][] getStochasticTransMatrix() {
		return stochasticTransMatrix;
	}

	public void setStochasticTransMatrix(double[][] stochsticTransMatrix) {
		this.stochasticTransMatrix = stochsticTransMatrix;
	}

	public double[] getRowSums() {
		return rowSums;
	}

	public void setRowSums(double[] rowSums) {
		this.rowSums = rowSums;
	}

	public double[] getColSums() {
		return colSums;
	}

	public void setColSums(double[] colSums) {
		this.colSums = colSums;
	}

	public double getElements() {
		return elements;
	}

	public void setElements(double elements) {
		this.elements = elements;
	}

	public void calculateStochsticTransMatrix() {
		
		for(int i = 0; i<=this.getDim(); i++){
			for(int j = 0; j<this.getDim(); j++){
				if(i>0){
					this.stochasticTransMatrix[i-1][j]=this.transMatrix[i-1][j]/this.rowSums[i-1];
				}
				if(i<this.getDim()){
					this.rowSums[i] += this.transMatrix[i][j];
					this.colSums[j] += this.transMatrix[i][j];
					this.elements += this.transMatrix[i][j];
				}
			}
		}
	}
		
}
