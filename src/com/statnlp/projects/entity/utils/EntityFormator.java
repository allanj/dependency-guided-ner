package com.statnlp.projects.entity.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Sentence;

public class EntityFormator {

	public static void OntoNotes2Conll(List<Instance> insts, String output){
		try {
			PrintWriter pw = RAWF.writer(output);
			for(Instance inst:insts){
				Sentence sent = (Sentence)inst.getInput();
				for(int i=0;i<sent.length();i++){
					pw.write((i+1)+"\t"+sent.get(i).getName()+"\t-\t"+sent.get(i).getTag()+"\t"+sent.get(i).getTag()+"\t-\t"+sent.get(i).getHeadIndex()+"\t"+sent.get(i).getDepLabel()+"\t"+sent.get(i).getEntity()+"\n");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
