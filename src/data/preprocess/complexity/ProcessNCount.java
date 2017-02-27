package data.preprocess.complexity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.statnlp.commons.io.RAWF;

public class ProcessNCount {

	public static String[] datasets = {"abc","cnn","mnb","nbc","pri","voa", "p25"};
	public static double[] time = new double[101];
	public static int[] timeNum = new int[101];
	
	private static final Pattern MY_REGEX = Pattern.compile("Time=(.+?)s");
	
	public static void readCountLog(String file) throws IOException{
		BufferedReader br = RAWF.reader(file);
		String line = null;
		int currentN = -1;
		while((line = br.readLine())!=null){
			if(line.startsWith("[Info] Now n is:")){
				String[] vals = line.split(":");
				currentN = Integer.valueOf(vals[1]);
				continue;
			}
			if(line.startsWith("#instances=")){
				String[] vals = line.split("=");
				timeNum[currentN] += Integer.valueOf(vals[1]);
				continue;
			}
			//Iteration 17: Obj=-2.356185156637    Time=0.012s 0.999981791389 Total time: 0.255s
			if(line.startsWith("Iteration 15")){
				double inferenceTime = findTime(line);
				if(inferenceTime==-1.0) throw new RuntimeException("The running time is not matched?\n"+line);
				time[currentN] += inferenceTime;
				continue;
			}
		}
		br.close();
	}
	
	private static double findTime(String str) {
		Matcher matcher = MY_REGEX.matcher(str);
		while (matcher.find()) {
			return Double.valueOf(matcher.group(1));
		}
		return -1;
	}
	
	public ProcessNCount() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) throws IOException{
		
//		for(String data: datasets)
//			readCountLog("data/countlog/lcrf-pred-dep-test-"+data+".log");
		
		//semi-pred-dep-noignore-test-abc.log
//		for(String data: datasets)
//			readCountLog("data/countlog/semi-pred-dep-noignore-test-"+data+".log");
		
//		for(String data: datasets)
//			readCountLog("data/countlog/model1-pred-dep-noignore-test-"+data+".log");
		
		
		for(String data: datasets)
			readCountLog("data/countlog/model2-pred-dep-noignore-test-"+data+".log");
		
		for(int i=1;i<=100; i++){
			System.out.println(time[i]/timeNum[i]);
		}
		
	}

}
