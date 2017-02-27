package com.statnlp.projects.entity.utils;

import edu.stanford.nlp.process.WordShapeClassifier;

public class Extractor {

	
	public static String wordShapeOf(String word){
		
		return WordShapeClassifier.wordShape(word,WordShapeClassifier.WORDSHAPEJENNY1);
	}
	

}
