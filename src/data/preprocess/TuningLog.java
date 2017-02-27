package data.preprocess;

import java.io.BufferedReader;
import java.io.IOException;

import com.statnlp.commons.io.RAWF;

/**
 * This class is for reading the tuning log to read the best L2 parameter
 * @author allanjie
 *
 */
public class TuningLog {

//	public static String[] newstypes = new String[]{"abc","cnn","mnb","nbc","p25","pri", "voa"};
	public static String[] newstypes = new String[]{"cnn"};
	public static String[] models = new String[]{"model1"};
//	public static String[] models = new String[]{"semi"};
	public static String[] l2vals = new String[]{"0.0001", "0.001", "0.01", "0.1", "1"};
	public static String[] deps = new String[]{"nodep", "dep"};
//	public static String linear_dataPrefix = "F:/Dropbox/SUTD/Work (1)/AAAI17/exp/Tunning/LinearCRF-bn/";
//	public static String semi_dataPrefix =  "F:/Dropbox/SUTD/Work (1)/AAAI17/exp/Tunning/SemiCRF/";
	
	public static String linear_dataPrefix = "F:/Dropbox/SUTD/Work (1)/AAAI17/exp/cvtuning/";
	public static String semi_dataPrefix =  "F:/Dropbox/SUTD/Work (1)/AAAI17/exp/cvtuning/";
	public static boolean cross_validation = true;
//	public static 
	
	
	public static void findBestAcc() throws IOException{
		
		for(String news : newstypes){
			for(String model: models){
				for(String dep : deps){
					findBestAcc(model, dep, news);
				}
			}
			System.out.println();
		}
	}
	
	public static void findBestAcc(String model, String dep, String news) throws IOException{
		double bestAcc = -1;
		String bestL2 = null;
		String devType = cross_validation? "cv":"dev";
		String dataPrefix = model.equals("lcrf")?linear_dataPrefix:semi_dataPrefix;
		for(int l=0; l<l2vals.length; l++){
			String file = model.equals("lcrf")? dataPrefix+model+"-"+dep+"-reg"+l2vals[l]+"-"+devType+"-"+news+".log" :dataPrefix+model+"-"+dep+"-reg"+l2vals[l]+"-noignore-"+devType+"-"+news+".log";
			double acc = cross_validation? getCVAcc(file):getAcc(file);
			if(acc > bestAcc){
				bestAcc = acc;
				bestL2 = l2vals[l];
			}
		}
//		System.out.println("Best L2 for model:"+model+" dep:"+dep+" data:"+news+" is:"+bestL2);
		System.out.print(bestL2+"\t");
	}
	
	
	/**
	 * return the f-score accuracy of the file
	 * @param log
	 * @return
	 * @throws IOException
	 */
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
	
	/**
	 * Return the overall f-score from the cv tuning log
	 * @param log
	 * @return
	 * @throws IOException
	 */
	private static double getCVAcc(String log) throws IOException{
		BufferedReader br = RAWF.reader(log);
		String line = null;
		double[] accs = new double[10];
		int index = 0;
		while((line = br.readLine())!=null){
			if(line.startsWith("accuracy:")){
				String[] vals = line.split("\\s+");
				accs[index] = Double.valueOf(vals[vals.length-1]);
				System.out.println(accs[index]);
				index++;
			}
		}
		br.close();
		if(index!=10) throw new RuntimeException("no 10 fold accuracy?");
		double acc = 0;
		for(double acy: accs) acc+=acy;
		acc/=10;
		System.out.println("average:"+acc);
		return acc;
	}
	
	public static void main(String[] args) throws IOException{
		findBestAcc();
	}
}
