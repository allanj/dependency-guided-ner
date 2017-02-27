package com.statnlp.projects.entity.lcr;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.entity.DPConfig;

public class ECRFEval {

	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException, InterruptedException{
		PrintWriter pw = RAWF.writer(nerOut);
		int lastGlobalId = Integer.MIN_VALUE;
		double max = Double.NEGATIVE_INFINITY;
		int bestId = -1;
		ECRFInstance bestInst = null;
		for(int index=0;index<testInsts.length;index++){
			ECRFInstance eInst = (ECRFInstance)testInsts[index];
			int globalId = eInst.getGlobalId();
			if(globalId!=-1){ // means the output is from the top K prediction.
				if(globalId==lastGlobalId){
					bestId = max > eInst.getPredictionScore()? bestId:eInst.getInstanceId();
					max = max > eInst.getPredictionScore()? max:eInst.getPredictionScore();
					bestInst = max > eInst.getPredictionScore()? bestInst:eInst;
				}else{
					if(lastGlobalId != Integer.MIN_VALUE){
						ArrayList<String> predEntities = bestInst.getPrediction();
						ArrayList<String> trueEntities = bestInst.getOutput();
						Sentence sent = bestInst.getInput();
						for(int i=0;i<sent.length();i++){
							pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+"\n");
						}
						pw.write("\n");
					}
					bestId = eInst.getInstanceId();
					max = eInst.getPredictionScore();
					bestInst = eInst;
					lastGlobalId = globalId;
				}
				if(index==testInsts.length-1){
					ArrayList<String> predEntities = bestInst.getPrediction();
					ArrayList<String> trueEntities = bestInst.getOutput();
					Sentence sent = bestInst.getInput();
					for(int i=0;i<sent.length();i++){
						pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+"\n");
					}
					pw.write("\n");
				}
			}else{
				bestInst = eInst;
				ArrayList<String> predEntities = bestInst.getPrediction();
				ArrayList<String> trueEntities = bestInst.getOutput();
				Sentence sent = bestInst.getInput();
				for(int i=0;i<sent.length();i++){
					pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+"\n");
				}
				pw.write("\n");
			}
			
		}
		pw.close();
		evalNER(nerOut);
	}
	
	
	private static void evalNER(String outputFile) throws IOException, InterruptedException{
		try{
			System.err.println("perl data/semeval10t1/conlleval.pl < "+outputFile);
			ProcessBuilder pb = null;
			if(DPConfig.windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
			}else{
				pb = new ProcessBuilder("data/semeval10t1/conlleval.pl"); 
			}
			pb.redirectInput(new File(outputFile));
			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			Process p = pb.start();
			p.waitFor();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static void writeNERResult(Instance[] predictions, String nerResult, boolean isNERInstance) throws IOException{
		PrintWriter pw = RAWF.writer(nerResult);
		for(int index=0;index<predictions.length;index++){
			Instance inst = predictions[index];
			ECRFInstance eInst = (ECRFInstance)inst;
			ArrayList<String> predEntities = eInst.getPrediction();
			ArrayList<String> trueEntities = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				int headIndex = sent.get(i).getHeadIndex()+1;
				pw.write((i+1)+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities.get(i)+" "+headIndex+"\n");
			}
			pw.write("\n");
		}
		
		pw.close();
	}
	
	public static void outputTopKNER(Instance[] predictions, String nerResult) throws IOException{
		PrintWriter pw = RAWF.writer(nerResult);
		for(int index=0;index<predictions.length;index++){
			Instance inst = predictions[index];
			ECRFInstance eInst = (ECRFInstance)inst;
			String[][] predEntities = eInst.getTopKPrediction();
			ArrayList<String> trueEntities = eInst.getOutput();
			Sentence sent = eInst.getInput();
			for(int k=0;k<predEntities.length;k++){
				if(predEntities[k]==null) break;
				pw.write("[InstanceId+Weight]:"+eInst.getInstanceId()+":"+eInst.getTopKScore()[k]+"\n");
				for(int i=0;i<sent.length();i++){
					int headIndex = sent.get(i).getHeadIndex()+1;
					pw.write((i+1)+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities.get(i)+" "+predEntities[k][i]+" "+headIndex+"\n");
				}
				pw.write("\n");
			}
		}
		pw.close();
	}
}
