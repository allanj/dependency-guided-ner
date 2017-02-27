package com.statnlp.projects.entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.projects.entity.utils.EntitySpan;

public class EntityChecker {

	
	public static void printAllEntities(String ecrfFile){
		PrintWriter pWriter;
		BufferedReader br;
		try {
			br = RAWF.reader(ecrfFile);
			pWriter = RAWF.writer("stat/testConnectedEntities.txt");
			String prev = "O";
			String prevLine = null;
			String line = null;
			int lineNum = 1;
			while((line = br.readLine())!=null){
				if(line.equals("")) {prev = "O";prevLine="";lineNum++; continue;}
				String[] vals = line.split("\\t");
				String entity = vals[3];
				if(entity.startsWith("B") && prev.startsWith("I")){
					pWriter.write("\n"+(lineNum-1)+" "+prevLine+"\n"+lineNum+" "+line+"\n");
				}
				prev = entity;
				prevLine = line;
				lineNum++;
//				else if(entity.startsWith("I")){
//					pWriter.write(line+"\n");
//				}
			}
			br.close();
			pWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<EntitySpan> checkAllIncomplete(Sentence sent){
		int start = 0; int end = -1;
		ArrayList<EntitySpan> elist = new ArrayList<EntitySpan>();
		String prevEntity = "";
		for(int i=1;i<sent.length();i++){
			String e = sent.get(i).getEntity();
			e = e.equals("O")? e: e.substring(2);
			prevEntity = sent.get(i-1).getEntity();
			prevEntity = prevEntity.equals("O")? prevEntity:prevEntity.substring(2);
			//Need to fix the case of continuous two entity, start:inclusive, end:exclusive..Fixed by the reader already
			if(!e.equals(prevEntity)){
				if(!e.equals("O")){
					if(!prevEntity.equals("O")){
						end = i;
						if(notIncomplete(sent,start,end))
							elist.add(new EntitySpan(prevEntity,start,end-1));
					}
					start = i;
				}else{
					end = i;
					if(notIncomplete(sent,start,end)){
						elist.add(new EntitySpan(prevEntity,start,end-1));
					}
				}
			}
		}
		String lastE = sent.get(sent.length()-1).getEntity();
		lastE = lastE.equals("O")? lastE: lastE.substring(2);
		if(!lastE.equals("O")){
			end = sent.length();
			if(notIncomplete(sent,start,end))
				elist.add(new EntitySpan(lastE,start,end-1));
		}
		return elist;
	}
	
	/**
	 * check this entity is incomplete or not
	 * @param sent: sentence
	 * @param start: inclusive
	 * @param end: exlucsive
	 * @return
	 */
	private static boolean notIncomplete(Sentence sent, int start, int end){
		return (end>start+1)  && !(sent.get(start).getHeadIndex()==end-1 || sent.get(end-1).getHeadIndex()==start);
	}
	
	/**
	 * Read alldata file from data/alldata/abc/train.output or dev.output or test.output
	 * @param file
	 * @throws IOException
	 */
	public static void checkEntityLength(String file) throws IOException {
		//entity, length, number
		HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();
		int maxEntityLen = 10;
		BufferedReader reader = RAWF.reader(file);
		String line = null;
		int total = 0;
		//int numE = 0; //number of entities in one sentence.
		ArrayList<String> esent = new ArrayList<String>();
		while((line = reader.readLine())!=null){
			if(line.equals("")) {
				int elen = 0;
				if(esent.size()==0) continue;
				for(int k=0; k<esent.size();k++){
					String e = esent.get(k);
					if(e.startsWith("B-")) {
						if(elen!=0){
							String prevE = esent.get(k-1);
							if(map.containsKey(prevE.substring(2))){
								int num = map.get(prevE.substring(2)).get(elen);
								map.get(prevE.substring(2)).set(elen, num+1);
							}else{
								ArrayList<Integer> lenNumList = new ArrayList<Integer>();
								for(int i=0;i<=maxEntityLen;i++) lenNumList.add(0);
								lenNumList.set(elen, 1);
								map.put(prevE.substring(2), lenNumList);
							}
						}
						elen = 1;
					}
					if(e.startsWith("I-")) elen++;
				}
				String prevE = esent.get(esent.size()-1);
				if(map.containsKey(prevE.substring(2))){
					int num = map.get(prevE.substring(2)).get(elen);
					map.get(prevE.substring(2)).set(elen, num+1);
				}else{
					ArrayList<Integer> lenNumList = new ArrayList<Integer>();
					for(int i=0;i<=maxEntityLen;i++) lenNumList.add(0);
					lenNumList.set(elen, 1);
					map.put(prevE.substring(2), lenNumList);
				}
				
				
				esent = new ArrayList<String>();
				//numE = 0;
				continue;
			}
			String[] vals = line.split("\\s+");
			String entity = vals[3];
			if(entity.equals("O"))  continue;
			esent.add(entity);
			if(entity.startsWith("B-")){
				//numE++;
				total++;
			}
		}
		reader.close();
		System.out.println("Number of total entities:"+total);
		int checkNum = 0;
		for(String entity: map.keySet()){
			ArrayList<Integer> nums = map.get(entity);
			System.out.print("Entity "+entity+":");
			for(int k=1;k<nums.size();k++){
				System.out.print("\t"+nums.get(k));
				checkNum+=nums.get(k);
			}
			System.out.println();
		}
		System.out.println("[checked]Number of total entities:"+checkNum);
	}
	
	public static void checkAllEntityLength() throws IOException{
		String[] datas = new String[]{"abc","cnn","mnb","nbc","pri","voa"};
		String[] types = new String[]{"train","dev","test"};
		for(String dat: datas){
			for(String type: types){
				System.out.println("Data TYPE:"+dat+"\t"+type);
				checkEntityLength("data/alldata/"+dat+"/"+type+".output");
				System.out.println();
			}
		}
	}
	
	/**
	 * If the span inside or is an entity
	 * @param sent
	 * @return
	 */
	public static boolean isEntity(Sentence sent, int index_1, int index_2) {
		int left = Math.min(index_1, index_2);
		int right = Math.max(index_2, index_1);
		for (int i = left; i < right; i++) {
			if (sent.get(i).getEntity().equals("O") || sent.get(i+1).getEntity().equals("O")) return false;
			if (!sent.get(i).getEntity().substring(1).equals(sent.get(i+1).getEntity().substring(1))) return false;
		}
		return true;
	}
	
	public static void main(String[] args) throws IOException{
//		printAllEntities(DPConfig.ecrftrain);
//		checkEntityLength("data/alldata/abc/test.output");
		checkAllEntityLength();
	}
}
