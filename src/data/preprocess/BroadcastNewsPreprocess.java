package data.preprocess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.statnlp.commons.io.RAWF;
import com.statnlp.commons.types.Sentence;
import com.statnlp.commons.types.WordToken;

import edu.stanford.nlp.trees.EnglishGrammaticalStructure;


/**
 * This file is for reading the broadcast news only. 
 * The English broadcast news in ontonotes.
 * The file needed is 
 * @author allanjie
 *
 */
public class BroadcastNewsPreprocess {

	
//	public static String[] datasets = {"abc","cnn","mnb","nbc","pri","voa", "p25"};
	public static String[] datasets = {"abc"};
	public static String[] valid = {"PERSON", "ORG", "GPE"};
	public static String[] validConvert = {"person", "organization", "gpe"};
	public static String[] others = {"NORP","FAC","LOC","PRODUCT","DATE","TIME","PERCENT","MONEY","QUANTITY","ORDINAL","CARDINAL","EVENT","WORK_OF_ART","LAW","LANGUAGE"};
	public static HashSet<String> dataNames;
	public static HashSet<String> validSet;
	public static HashMap<String, String> validMap;
	public static HashSet<String> otherSet;
	public static String filePrefix = "F:/phd/data/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0_LDC2013T19/ontonotes-release-5.0/data/files/data/english/annotations/bn";
//	public static String filePrefix = "D:/Downloads/test";
	public static String tmpTreebank = "D:/Downloads/tmp/temp";
	public static String tmpOutput = "D:/Downloads/tmp/temp.output";
	public static String tmpError = "D:/Downloads/tmp/temp.error";
	public static String converterPath = "F:/Dropbox/SUTD/tools/pennconverter.jar";
	public static String converterLog = "F:/Dropbox/SUTD/tools/log.log";
	public static String outputPrefx = "E:/Framework/data/allanprocess/";
	
	public static boolean useStanfordConverter = true;
	//linux setup
//	public static String filePrefix = "/home/allan/data/ontonotes_subset";
//	public static String tmpTreebank = "/home/allan/temp/temp.txt";
//	public static String converterPath = "/home/allan/tools/pennconverter.jar";
//	public static String converterLog = "/home/allan/tools/log.log";
//	public static String outputPrefx = "/home/allan/jointdpe/data/allanprocess/";
	
	
	private static Sentence convertUsingStanfordNLP() throws IOException{
		PrintStream out = new PrintStream(new FileOutputStream(tmpOutput));
		System.setOut(out);
		EnglishGrammaticalStructure.main(new String[]{"-basic","-keepPunct","-conllx","-treeFile",tmpTreebank});
		System.setOut(System.out);
		BufferedReader depReader = RAWF.reader(tmpOutput);
		String depLine = null;
		ArrayList<WordToken> wts = new ArrayList<WordToken>();
		wts.add(new WordToken("ROOT", "ROOT", -1, "O", "NOLABEL"));
		while((depLine=depReader.readLine())!=null){
			if(depLine.equals("")) continue;
			String[] components = depLine.split("\\t+");
			int headIdx = Integer.valueOf(components[6]);
			String depLabel = components[7];
			wts.add(new WordToken(components[1], components[4], headIdx, "O", depLabel));
		}
		depReader.close();
		WordToken[] wta  = new WordToken[wts.size()];
		wts.toArray(wta);
		Sentence sent = new Sentence(wta);
		return sent;
	}
	
	/**
	 * The sentence returned will have a root on the leftmost
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	private static Sentence convert() throws IOException, InterruptedException{
		ProcessBuilder pb = new ProcessBuilder("java","-jar", converterPath,"-log", converterLog,"-stopOnError=false"); 
		pb.redirectInput(new File(tmpTreebank));
		pb.redirectOutput(ProcessBuilder.Redirect.to(new File(tmpOutput)));
		pb.redirectError(ProcessBuilder.Redirect.to(new File(tmpError)));
		Process p = pb.start();
		p.waitFor();
		BufferedReader reader =  RAWF.reader(tmpError); //new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line = null;
		while ( (line = reader.readLine()) != null) {
			if(line.startsWith("Number of errors")){
				String trimmed = line.trim();
				String[] vals = trimmed.split("\\s+"); 
				
				if(!vals[vals.length-1].equals("0"))
					return null;
			}
		}
		reader.close();
		reader =  RAWF.reader(tmpOutput); //new BufferedReader(new InputStreamReader(p.getInputStream()));
		line = null;
		ArrayList<WordToken> wts = new ArrayList<WordToken>();
		wts.add(new WordToken("ROOT", "ROOT", -1, "O", "NOLABEL"));
		while ( (line = reader.readLine()) != null) {
			if(line.equals("")) continue;
			String trimmed = line.trim();
			String[] vals = trimmed.split("\\t");
			wts.add(new WordToken(vals[1], vals[3], Integer.valueOf(vals[6]), "O", vals[7]));
		}
		reader.close();
		WordToken[] wta  = new WordToken[wts.size()];
		wts.toArray(wta);
		Sentence sent = new Sentence(wta);
		return sent;
	}
	
	public static void process() throws IOException, InterruptedException{
		dataNames = new HashSet<String>();
		validSet = new HashSet<String>();
		validMap = new HashMap<String, String>();
		otherSet = new HashSet<String>();
		for(String file: datasets) dataNames.add(file);
		for(int i=0;i<valid.length;i++) { validSet.add(valid[i]);  validMap.put(valid[i], validConvert[i]);}
		for(String type: others) otherSet.add(type);
		
		File file = new File(filePrefix);
		String[] names = file.list();
		for(String data: names){
			if(dataNames.contains(data)){
				File subFile = new File(filePrefix+"/"+data); //the folder that abc/cnn/mnb
				String[] subNames = subFile.list();
				ArrayList<Sentence> sents = new ArrayList<Sentence>();
				for(String numberFolder: subNames){
					File theNumber = new File(filePrefix+"/"+data+"/"+numberFolder); //abc/00/
					if(theNumber.isDirectory()){
						String[] textFileList = theNumber.list();  //abc/00/0001.parse.. something like this.
//						System.err.println(Arrays.toString(textFileList));
						for(String textFile: textFileList){
//							if(textFile.endsWith(".onf"))
//								processTheONFFiles(filePrefix+"/"+data+"/"+numberFolder+"/"+textFile, sents);
							if(textFile.endsWith(".name")){
//								System.err.println(textFile);
								String[] codes = textFile.split("\\.");
								String preName = codes[0];
								for(int c=1; c<codes.length-1;c++) preName = preName+"."+codes[c];
								String parseFile = preName+".parse";
								processNameFiles(filePrefix+"/"+data+"/"+numberFolder+"/"+textFile, filePrefix+"/"+data+"/"+numberFolder+"/"+parseFile, sents);
							}
						}
					}
				}
				System.err.println("[Info] Finishing dataset:"+data+" numSents:"+sents.size());
				//print these sentences.
				printConll(data,sents);
			}
		}
	}
	
	
	private static void processNameFiles(String file, String parseFile, ArrayList<Sentence> sents) throws IOException, InterruptedException{
		BufferedReader reader = RAWF.reader(file);
		BufferedReader parseReader = RAWF.reader(parseFile);
		PrintWriter pw = null;
		String line = null;
		String parseLine = null;
		while((line = reader.readLine())!=null){
			if(line.startsWith("<DOC ")|| line.startsWith("</DOC")) continue;
			String rpline = line.replace("<ENAMEX", "</ENAMEX>");
			String[] vals = rpline.split("(\\s?)</ENAMEX>");
			int index = 1;
			ArrayList<ESpan> spans = new ArrayList<ESpan>();
			int left = -1;
			int right = -1;
			String type = "";
			for(int i=0;i<vals.length;i++){
				vals[i] = vals[i].trim();
				if(vals[i].equals("")) {
//					System.err.println(index+"\n"+vals[i+1]+"\n"+rpline+"\n");
					continue;
				}
				if(vals[i].startsWith("TYPE=")){
					String[] eAndWords = vals[i].split(">");
					if(eAndWords.length!=2)
						throw new RuntimeException("ws length is not 2:"+vals[i]+" \n"+rpline+"\n"+file); //cnn_0425: modify. 
					List<String> singleEntity = getTagValues(vals[i], "long");
					if(singleEntity.size()!=1) throw new RuntimeException("The length should be only one:"+vals[i]+"\n"+rpline+"\n"+file);
					String[] ewords = eAndWords[1].trim().split(" ");
					left = index;
					right = index+ewords.length-1;
					type = singleEntity.get(0);
					ESpan span = new ESpan(left, right, type);
					spans.add(span);
					if(!validSet.contains(span.entity) && !otherSet.contains(span.entity))
						throw new RuntimeException("cannot find this entity:"+span.entity);
					index += ewords.length;
				}else{
					String[] words = vals[i].trim().split(" ");
					index += words.length;
				}
//				System.err.println(index);
			}
//		    System.err.println(spans.toString());
		    //parse oneSentence here?
		    pw = RAWF.writer(tmpTreebank);
			while((parseLine=parseReader.readLine())!=null){
				if(parseLine.equals("")){
					pw.write("\n");
					pw.close();
					break;
				}
				pw.write(parseLine+"\n");
			}
			Sentence sent = useStanfordConverter? convertUsingStanfordNLP():convert();
//			System.err.println(sent.toString());
			if(sent!=null){
				//check the sentence length
				if(index!=sent.length()) throw new RuntimeException("The length is not the same: index:"+index+" sent len:"+sent.length()+"\n"+sent.toString()+"\n"+file);
				for(ESpan span: spans){
					for(int i=span.left; i<=span.right;i++) {
						String entity = null;
						if(validSet.contains(span.entity)){
							entity  = validMap.get(span.entity);
							entity = i==span.left? "B-"+entity:"I-"+entity;
						}else if(otherSet.contains(span.entity)){
							entity = i==span.left? "B-MISC":"I-MISC";
						}else{
							throw new RuntimeException("cannot find this entity:"+entity);
						}
						sent.get(i).setEntity(entity);
					}
				}
				sents.add(sent);
			}else{
				continue;
			}
		}
		parseReader.close();
		reader.close();
	}
	
	private static final Pattern TAG_REGEX = Pattern.compile("TYPE=\"(.+?)\">(.+?)</ENAMEX>");
	private static final Pattern LONG_REGEX = Pattern.compile("TYPE=\"(.+?)\"(.*?)>(.+)");
	
//	private static final Pattern CHECK_REGEX = Pattern.compile("<ENAMEX (.+?)>(.+?)</ENAMEX>");

	private static List<String> getTagValues(final String str, String len) {
	    final List<String> tagValues = new ArrayList<String>();
	    final Matcher matcher = len.equals("short")?TAG_REGEX.matcher(str): LONG_REGEX.matcher(str);
	    
//	    final Matcher matcher = CHECK_REGEX.matcher(str);
	    while (matcher.find()) {
//	    	System.err.println(matcher.end(2));
	    	//tagValues.add(matcher.group(0));

//	    	System.err.println(matcher.group(0));
//	    	System.err.println(matcher.group(1));
	    	tagValues.add(matcher.group(1));
	    	//tagValues.add(matcher.group(2));
	    }
	    return tagValues;
	}


	
	
	/**
	 * 
	 * @param datasetName: abc/cnn or something else
	 * @throws IOException
	 */
	private static void printConll(String datasetName, ArrayList<Sentence> sents) throws IOException{
		PrintWriter pw = RAWF.writer(outputPrefx+"/"+datasetName+"/all.conllx");
		System.err.println("   dataset:"+datasetName+":"+sents.size());
		for(Sentence sent: sents){
			for(int i=1; i<sent.length(); i++)
				pw.write(i+"\t"+sent.get(i).getName()+"\t_\t"+sent.get(i).getTag()+"\t"+sent.get(i).getTag()+"\t_\t"+sent.get(i).getHeadIndex()+"\t"+sent.get(i).getDepLabel()+"\t_\t_\t"+sent.get(i).getEntity()+"\n");
			pw.write("\n");
		}
	
		pw.close();
	}
	
	/**
	 * Split the data with 50/25/25 portion
	 * @throws IOException
	 */
	public static void splitTrainDevTest() throws IOException{
		for(String data: datasets){
			if(data.equals("p2.5_a2e") ||data.equals("p2.5_c2e") ) continue;
			BufferedReader reader = RAWF.reader(outputPrefx+data+"/all.conllx");
			String line = null;
			int numberOfSentence = 0;
			while((line = reader.readLine())!=null){
				if(line.equals("")) numberOfSentence++;
			}
			reader.close();
			System.err.println("dataset:"+data+" number:"+numberOfSentence);
			int testNum = numberOfSentence/4+1;
			int trainNum = numberOfSentence - testNum;
			
			System.err.println("split with:"+trainNum+"\t"+testNum);
			PrintWriter pwTrain = RAWF.writer(outputPrefx+data+"/train.conllx");
			//PrintWriter pwDev = RAWF.writer(outputPrefx+data+"/dev.conllx");
			PrintWriter pwTest = RAWF.writer(outputPrefx+data+"/test.conllx");
			reader = RAWF.reader(outputPrefx+data+"/all.conllx");
			int number = 0;
			String state = "train";
			while((line = reader.readLine())!=null){
				if(line.equals("")) {
					number++;
					if(state.equals("train")){
						pwTrain.write("\n");
						if(number==trainNum) { pwTrain.close(); state = "test"; number= 0;}
					}else if(state.equals("dev")){
//						pwDev.write("\n");
//						if(number==devNum) { pwDev.close(); state = "test"; number = 0;}
					}else if(state.equals("test")){
						pwTest.write("\n");
						if(number==testNum) { pwTest.close(); state = "done"; number = 0;}
					}
					continue;
				}
				if(state.equals("train")) pwTrain.write(line+"\n");
//				else if(state.equals("dev")) pwDev.write(line+"\n");
				else if(state.equals("test")) pwTest.write(line+"\n");
				
			}
			reader.close();
		}
	}
	
	/**
	 * To run this on the scratch
	 *   1. process()
	 *   2. splitTrainDevTest() on 50/25/25 portion.
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, InterruptedException{
//		System.err.println(convert());
//		process();
//		splitTrainDevTest();
	}

}
