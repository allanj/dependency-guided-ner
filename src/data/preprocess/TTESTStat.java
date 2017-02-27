package data.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import com.statnlp.commons.io.RAWF;
import com.statnlp.projects.entity.DPConfig;

/**
 * Compare two system.
 * @author 1001981
 *
 */
public class TTESTStat {

	public static String tmpEval1 = "tmpfolder/tmp1.eval";
	public static String tmpEval2 = "tmpfolder/tmp2.eval";
	public static String tmpLog = "tmpfolder/tmp.log";
	public static ArrayList<ArrayList<String>> res1 = new ArrayList<ArrayList<String>>();
	public static ArrayList<ArrayList<String>> res2 = new ArrayList<ArrayList<String>>();
	
	
	public static void testFiles(String eval1, String eval2, boolean leftBetter) throws IOException{
		BufferedReader br1 = RAWF.reader(eval1);
		BufferedReader br2 = RAWF.reader(eval2);
		
		String line1 = null;
		String line2 = null;
		res1 = new ArrayList<ArrayList<String>>();
		res2 = new ArrayList<ArrayList<String>>();
		ArrayList<String> sent1 = new ArrayList<String>();
		ArrayList<String> sent2 = new ArrayList<String>();
		while((line1 = br1.readLine())!=null){
			line2 = br2.readLine();
			if(line1.equals("")){
				res1.add(sent1);
				res2.add(sent2);
				sent1 = new ArrayList<String>();
				sent2 = new ArrayList<String>();
				continue;
			}
			sent1.add(line1);
			sent2.add(line2);
		}
		br1.close();
		br1.close();
		
		int better = 0;
		int total = 200000;
		for(int it=1; it<=total; it++){
			Random rand = new Random();
			PrintWriter pw1 = RAWF.writer(tmpEval1);
			PrintWriter pw2 = RAWF.writer(tmpEval2);
			for(int i=0;i<res1.size(); i++){
				int idx = rand.nextInt(res1.size());
				for(int w=0;w<res1.get(idx).size();w++){
					pw1.write(res1.get(idx).get(w)+"\n");
				}
				pw1.write("\n");
				for(int w=0;w<res2.get(idx).size();w++){
					pw2.write(res2.get(idx).get(w)+"\n");
				}
				pw2.write("\n");
			}
			pw1.close();
			pw2.close();
			double f1 = getScore(tmpEval1);
			double f2 = getScore(tmpEval2);
			if(leftBetter && f1>f2) better++;
			if(!leftBetter && f1<f2) better++;
			if(it%100==0)
				System.out.println("iteration:"+it+" better num:"+better+" p-value:"+(1-better*1.0/it));
		}
		System.out.println("p-value:"+(1-better*1.0/total));
		
	}
	
	private static double getScore(String evalFile) throws IOException{
		//System.err.println("perl data/semeval10t1/conlleval.pl < "+evalFile);
		ProcessBuilder pb = null;
		if(DPConfig.windows){
			pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
		}else{
			pb = new ProcessBuilder("data/semeval10t1/conlleval.pl"); 
		}
		pb.redirectInput(new File(evalFile));
		pb.redirectOutput(new File(tmpLog));
		//pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		Process p = pb.start();
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return getAcc(tmpLog);
	}
	
	private static double getAcc(String log) throws IOException{
		BufferedReader br = RAWF.reader(log);
		String line = null;
		double acc = -1;
		while((line = br.readLine())!=null){
			if(line.startsWith("accuracy:")){
				String[] vals = line.split("\\s+");
				acc = Double.valueOf(vals[vals.length-1]);
			}
		}
		br.close();
		if(acc==-1) throw new RuntimeException("no acc returned?");
		return acc;
	}
	
	public static void main(String[] args) throws IOException{
		
//		//our sample should be groups of F-score.
//		double[] sample1 = { 1  , 2  , 3   ,4 , 3, 5, 6.1, 3.4, 2.9, 4.4};
//		double[] sample2 = { 5.2, 4.2, 7.24,4 , 5, 6, 4.1, 5.9, 7.0, 8.0};
//		double t_statistic;
//		TTest ttest = new TTest();
//		t_statistic = ttest.pairedTTest(sample1, sample2);
//		System.out.println(Double.toString( t_statistic) );
//		testFiles("data/result_cv/all/semi.model0.pred.depf-true.noignore.eval.all.txt", 
//				"data/result_cv/all/semi.model2.pred.depf-true.noignore.eval.all.txt");
		DPConfig.windows = false;

		String data = args[0];//"all"; //args[0]
		boolean leftBetter = args[1].equals("left")? true:false; //false
		String gold = args[2];
		String leftModel = args[3];//"model0";
		String rightModel = args[4];//"model1";
		String dep = args[5];
		String leftGold = gold;
		String rightGold = gold;
		String logType = gold;
		String tmpEval1Type = leftGold;
		
		String sub = ".all";
		if(data.equals("all")) sub = ".all";
		else sub = "";
		
		if(gold.equals("pred") && leftModel.equals("model0") && dep.equals("false")){
			leftGold = "gold";
			logType = "gold&pred";
			tmpEval1Type = logType;
		}
		tmpEval1 = "tmpfolder/"+data+".semi."+leftModel+"."+rightModel+"."+tmpEval1Type+".depf-"+dep+".noignore.eval"+sub+"1.eval";
		tmpEval2 = "tmpfolder/"+data+".semi."+leftModel+"."+rightModel+"."+rightGold+".depf-"+dep+".noignore.eval"+sub+"2.eval";
		tmpLog = "tmpfolder/"+data+".semi."+leftModel+"."+rightModel+"."+logType+".depf-"+dep+".noignore.eval"+sub+".log";
		testFiles("data/result_cv/"+data+"/semi."+leftModel+"."+leftGold+".depf-"+dep+".noignore.eval"+sub+".txt", 
				"data/result_cv/"+data+"/semi."+rightModel+"."+rightGold+".depf-"+dep+".noignore.eval"+sub+".txt", leftBetter);

	}

}
