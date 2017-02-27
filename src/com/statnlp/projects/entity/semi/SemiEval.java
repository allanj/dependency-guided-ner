package com.statnlp.projects.entity.semi;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class SemiEval {

	
	public static boolean windows = false;
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void evalNER(Instance[] testInsts, String nerOut) throws IOException, InterruptedException{
		PrintWriter pw = RAWF.writer(nerOut);
		for(int index=0;index<testInsts.length;index++){
			SemiCRFInstance eInst = (SemiCRFInstance)testInsts[index];
			String[] predEntities = eInst.toEntities(eInst.getPrediction());
			String[] trueEntities = eInst.toEntities(eInst.getOutput());
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				pw.write(sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities[i]+" "+predEntities[i]+"\n");
			}
			pw.write("\n");
		}
		pw.close();
		evalNER(nerOut);
	}
	
	
	private static void evalNER(String outputFile) throws IOException, InterruptedException{
		try{
			System.err.println("perl data/semeval10t1/conlleval.pl < "+outputFile);
			ProcessBuilder pb = null;
			if(windows){
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
	
	public static void writeNERResult(Instance[] predictions, String nerResult) throws IOException{
		PrintWriter pw = RAWF.writer(nerResult);
		for(int index=0;index<predictions.length;index++){
			Instance inst = predictions[index];
			SemiCRFInstance eInst = (SemiCRFInstance)inst;
			String[] predEntities = eInst.toEntities(eInst.getPrediction());
			String[] trueEntities = eInst.toEntities(eInst.getOutput());
			Sentence sent = eInst.getInput();
			for(int i=0;i<sent.length();i++){
				int headIndex = sent.get(i).getHeadIndex()+1;
				pw.write((i+1)+" "+sent.get(i).getName()+" "+sent.get(i).getTag()+" "+trueEntities[i]+" "+predEntities[i]+" "+headIndex+"\n");
			}
			pw.write("\n");
		}
		
		pw.close();
	}
}
