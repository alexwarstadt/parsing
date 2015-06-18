package parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.*;

public class Parser {

	public Map<String, Integer> cat2num = new HashMap<String, Integer>();
	public Map<Integer, String> num2cat = new HashMap<Integer, String>();

	// parent --> child --> count
	public Map<Integer, Map<Integer, Integer>> uCountsb = 
			new HashMap<Integer, Map<Integer, Integer>>();

	// right --> left --> par --> count
	public Map<Integer, Map<Integer, Map<Integer, Integer>>> biCountsc = 
			new HashMap<Integer, Map<Integer, Map<Integer, Integer>>>();

	// child --> parent --> count
	public Map<Integer, Map<Integer, Double>> uRho = 
			new HashMap<Integer, Map<Integer, Double>>();

	// right --> left --> parent --> count
	public Map<Integer, Map<Integer, Map<Integer, Double>>> biRho = 
			new HashMap<Integer, Map<Integer, Map<Integer, Double>>>();

	public Map<Integer, Integer> totalRules = new HashMap<Integer, Integer>();



	/**
	 * 
	 * @param file
	 */
	public void getCounts(String file){
		try{
			BufferedReader r = new BufferedReader(new FileReader(file));
			String line = r.readLine();
			String[] split;
			int code = 0;
			int count;
			Integer lastTotal;
			while (line != null){
				split = line.split(" ");
				count = Integer.parseInt(split[0]);
				Integer left = cat2num.get(split[3]);
				if (left == null){
					left = code;
					this.cat2num.put(split[3], code);
					this.num2cat.put(code, split[3]);
					code++;
				}
				Integer parent = cat2num.get(split[1]);
				if (parent == null){
					parent = code;
					this.cat2num.put(split[1], code);
					this.num2cat.put(code, split[1]);
					code++;
				}
				lastTotal = this.totalRules.get(parent);
				if (lastTotal == null){lastTotal = 0;}
				this.totalRules.put(parent, lastTotal + count);
				
				
				
				
				if (split.length == 4){
					Map<Integer, Integer> leftToCount = this.uCountsb.get(parent);
					if (leftToCount == null){leftToCount =  new HashMap<Integer, Integer>();}
					leftToCount.put(left, count);
					this.uCountsb.put(parent, leftToCount);
				} else {
					Integer right = cat2num.get(split[4]);
					if (right == null){
						right = code;
						this.cat2num.put(split[4], right);
						this.num2cat.put(right, split[4]);
						code++;
					}
					Map<Integer, Map<Integer, Integer>> leftToPar = this.biCountsc.get(right);
					if (leftToPar == null){leftToPar =  new HashMap<Integer, Map<Integer, Integer>>();}
					Map<Integer, Integer> parToCount = leftToPar.get(left);
					if (parToCount == null){parToCount = new HashMap<Integer, Integer>();}
					parToCount.put(parent, count);
					leftToPar.put(left, parToCount);
					this.biCountsc.put(right, leftToPar);
				}
				line = r.readLine();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}


	/**
	 * calculate rho values based on counts
	 */
	public void getRho(){
		double newVal;
		double parentTotal;
		
		/**
		 *  unary rhos
		 */
		for (int parent: this.uCountsb.keySet()){
			parentTotal = this.totalRules.get(parent).doubleValue();
			for (int left: this.uCountsb.get(parent).keySet()){
				Map<Integer, Double> parentToRho = this.uRho.get(left);
				if (parentToRho == null){parentToRho = new HashMap<Integer, Double>();}
				newVal = this.uCountsb.get(parent).get(left).doubleValue() / parentTotal;
				parentToRho.put(parent, newVal);
				this.uRho.put(left, parentToRho);
			}
		} 
		
		/**
		 *  binary rhos
		 */
		for (int right: this.biCountsc.keySet()){
			for (int left: this.biCountsc.get(right).keySet()){
				for (int par: this.biCountsc.get(right).get(left).keySet()){
					if (this.biRho.get(right) == null){
						this.biRho.put(right, new HashMap<Integer, Map<Integer, Double>>());
					}
					if (this.biRho.get(right).get(left) == null){
						this.biRho.get(right).put(left, new HashMap<Integer, Double>());
					}
					double rho = this.biCountsc.get(right).get(left).get(par).doubleValue() / this.totalRules.get(par);;
					this.biRho.get(right).get(left).put(par, rho);
				}
			}
		}
	}

	
	
	/**
	 * CONSTITUENT class
	 *
	 */
	private class Constituent{
		public int label;
		public Constituent left;
		public Constituent right;
		public double mu;

		public Constituent(int label, Constituent left, Constituent right, double mu){
			this.label = label;
			this.left = left;
			this.right = right;
			this.mu = mu;
		}

		/**
		 * recursively build parentheses notation version of tree structures
		 */
		@Override
		public String toString(){
			String toReturn;
			String label = num2cat.get(this.label);
			if (this.left == null){
				toReturn = label; 
			} else if(this.right == null){
				toReturn = "(" + label + " " + this.left.toString() + ")";
			} else {
				toReturn = "(" + label + " " + this.left.toString() + " " + this.right.toString() + ")";
			}
			return toReturn;
		}
	}

	
	
	/**
	 * basically a wrapper for all the constituents.
	 *
	 * I hash every constituent by its parent label, since there should only be one 
	 * constituent per category label.
	 *
	 */
	private class Cell{
		public Map<Integer, Constituent> cell;
		public Cell(){
			this.cell = new HashMap<Integer, Constituent>();
		}
	}

	
	
	/**
	 * square array
	 * 
	 */
	private class Chart{
		public Cell[][] chart;
		public Chart(int N){
			chart = new Cell[N][N+1];
		}
	}
	
	
	
	

	/**
	 * 
	 * builds the chart for a sentence
	 * 
	 */
	public Chart parse(String sentence){
		String[] split = sentence.split(" ");
		int L = split.length;
		if(L > 25){
			return null;
		}
		List<Integer> words = new ArrayList<Integer>(L);
		for (String w: split){
			words.add(this.cat2num.get(w));
		}
		Chart C = new Chart(L);
		for (int l=1; l<=L; l++){
			for (int s=0; s<=L-l; s++){
				C.chart[s][s+l] = new Cell();
				this.fill(C.chart[s][s+l], s, s+l, C, words);
			}
		}
		return C;
	}

	
	
	
	/**
	 * 
	 * just writes the toString() for a chart to a file
	 * 
	 */
	public void writeChart(Chart C, BufferedWriter w){
		try{
			if (C == null){
				w.write("*IGNORE*");
			} else {
				Constituent top = C.chart[0][C.chart.length-1].cell.get(this.cat2num.get("TOP"));
				if (top == null){
				} else {
					w.write(top.toString());
				}
			}
		} catch (IOException e){
			e.printStackTrace();
		}
	}


	
	

	/**
	 * 
	 * see the book
	 * 
	 */
	public void fill(Cell c, int i, int k, Chart C, List<Integer> words){
		if (k == i+1){
			Constituent word = new Constituent(words.get(i), null, null, 1);
			c.cell.put(words.get(i), word);
		} else {
			for (int j=i+1; j<=k-1; j++){
				for (Constituent c2: C.chart[j][k].cell.values()){
					Map<Integer, Map<Integer, Double>> leftToParent = this.biRho.get(c2.label);
					if (leftToParent != null){
						for (Integer left: leftToParent.keySet()){
							Constituent c1 = C.chart[i][j].cell.get(left);
							if (c1 != null){
								Map<Integer, Double> parentToRho = leftToParent.get(left);
								if (leftToParent != null){
									for (int parent: parentToRho.keySet()){
										Constituent newc = new Constituent(
												parent, c1, c2, Math.log(parentToRho.get(parent))+c1.mu+c2.mu);
										Constituent oldc = c.cell.get(parent);
										if (oldc != null){
											if (newc.mu > oldc.mu){
												c.cell.put(parent, newc);
											}
										} else {
											c.cell.put(parent, newc);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		this.unary(c);
	}



	
	/**
	 * keep checking the whole cell for possible new unary constituents until there aren't any more
	 * 
	 */
	public void unary(Cell c){
		boolean changing = true;
		while (changing){
			Constituent newc = this.unaryHelp(c);
			if (newc == null){
				changing = false;
			} else {
				changing = true;
				c.cell.put(newc.label, newc);
			}
		}
	}


	
	
	/**
	 * Check the whole cell for new unary constituents until you find one, then return it
	 * 
	 */
	public Constituent unaryHelp(Cell c){
		Map<Integer, Constituent> news = new HashMap<Integer, Constituent>();
		for (int label: c.cell.keySet()){
			Map<Integer, Double> parentToRho = this.uRho.get(label);
			if (parentToRho != null){
				for (int parent: parentToRho.keySet()){
					Constituent daughter = c.cell.get(label);
					Constituent newc = new Constituent(
							parent, daughter, null, Math.log(parentToRho.get(parent))+daughter.mu);
					Constituent oldc = c.cell.get(parent);
					if (oldc == null || oldc.mu < newc.mu){
						return newc;
					}
				}
			}
		}
		return null;
	}


	


	/**
	 * read file, parse, and write line by line 
	 * 
	 */
	public void parseFile(String input, String output){
		try{
			BufferedReader r = new BufferedReader(new FileReader(input));
			BufferedWriter w = new BufferedWriter(new FileWriter(output));
			String line = r.readLine();
			while (line != null){
				Chart C = this.parse(line);
				this.writeChart(C, w);
				w.write("\n");
				line = r.readLine();
			}
			r.close();
			w.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}



	public static void main(String[] args){
		String rules = args[0];
		String sentences = args[1];
		String output = args[2];
		Parser p = new Parser();
		p.getCounts(rules);
		p.getRho();
		p.parseFile(sentences, output);
	}
}