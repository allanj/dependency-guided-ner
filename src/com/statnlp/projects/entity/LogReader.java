package com.statnlp.projects.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import com.statnlp.commons.io.RAWF;

public class LogReader {
	
	/**
	 * The direct source file for the data
	 */
	private String data;
	
	public LogReader(String data) {
		this.data = data;
	}
	

	public void calculateDecodeTime(){
		try{
			BufferedReader reader = RAWF.reader(data);
			String line = null;
			boolean decode = false;
			ArrayList<Integer> instNum = new ArrayList<Integer>();
			double[] smallDecodeTime = null;
			while((line = reader.readLine())!=null){
				String[] vals = line.split(" ");
				if(line.startsWith("Training completes")) { decode = true; continue;}
				if(decode && line.startsWith("Thread")){
					instNum.add(Integer.valueOf(vals[3]));
					continue;
				}
				if(decode && line.startsWith("Okay. Decoding started.")){
					smallDecodeTime = new double[instNum.size()];
					continue;
				}
				if(decode && line.startsWith("Decoding time")){
					smallDecodeTime[Integer.valueOf(vals[7])] = Double.valueOf(vals[9]);
					continue;
				}
				
			}
			reader.close();
			int sum = 0;
			for(int x: instNum){
				sum+=x;
			}
			double sumTime = 0;
			for(double val: smallDecodeTime){
				sumTime += val;
			}
			System.out.println(sumTime/sum);
//			System.out.println("The average decode time for "+data+": "+sumTime/sum);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public void calculateTrainingPerIter(){
		try{
			BufferedReader reader = RAWF.reader(data);
			String line = null;
			boolean decode = false;
			ArrayList<Integer> instNum = new ArrayList<Integer>();
			double[] smallDecodeTime = null;
			while((line = reader.readLine())!=null){
				String[] vals = line.split(" ");
				if(line.startsWith("Training completes")) { decode = true; continue;}
				if(decode && line.startsWith("Thread")){
					instNum.add(Integer.valueOf(vals[3]));
					continue;
				}
				if(decode && line.startsWith("Okay. Decoding started.")){
					smallDecodeTime = new double[instNum.size()];
					continue;
				}
				if(decode && line.startsWith("Decoding time")){
					smallDecodeTime[Integer.valueOf(vals[7])] = Double.valueOf(vals[9]);
					continue;
				}
				
			}
			reader.close();
			int sum = 0;
			for(int x: instNum){
				sum+=x;
			}
			double sumTime = 0;
			for(double val: smallDecodeTime){
				sumTime += val;
			}
			System.out.println(sumTime/sum);
//			System.out.println("The average decode time for "+data+": "+sumTime/sum);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public void printFmeasure(){
		try{
			BufferedReader reader = RAWF.reader(data);
			String line = null;
			while((line = reader.readLine())!=null){
				String[] vals = line.split("\\s+");
				if(line.startsWith("accuracy:")){
					System.out.print(vals[3].split("%")[0]+"\t"+vals[5].split("%")[0]+"\t"+vals[vals.length-1]);
				}
			}
			reader.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		String[] types = new String[]{"abc","cnn","mnb","nbc", "p25","pri","voa"};
//		String[] models = new String[]{"lcrf","semi","model1","model2"};
		String[] models = new String[]{"semi"};
		String[] deps = new String[]{"dep"};
//		String[] igs = new String[]{"-ignore","-noignore"};
//		String prefix = "F:/Dropbox/SUTD/Work (1)/AAAI17/exp/Testing-bn";
		String prefix = "/Users/allanjie/Dropbox/SUTD/Work (1)/AAAI17/exp/Testing-bn-cvtuned";

		//String model = "lcrf";
//		String type = "abc";
//		String dep = "nodep";

		String goldPred = "-pred";
		for(String type: types){
			for(String model: models){
				String ignore = "-noignore";
				if(model.equals("lcrf")) { goldPred = "-pred"; ignore = "";}
				for(String dep: deps){
					String data = prefix+"/"+model+"/"+model+goldPred+"-"+dep+ignore+"-test-"+type+".log";
//					System.out.print(data+" ");
					LogReader lr = new LogReader(data);
					lr.calculateDecodeTime();
//					lr.printFmeasure();
				}
			}
			System.out.println();
		}
		
		
	}
}
