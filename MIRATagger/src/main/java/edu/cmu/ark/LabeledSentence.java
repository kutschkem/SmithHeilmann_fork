package edu.cmu.ark;

import java.util.*;

public class LabeledSentence {

	public LabeledSentence(){
		tokens = new ArrayList<String>();
		stems = new ArrayList<String>();
		posLabels = new ArrayList<String>(); 
		trueLabels = new ArrayList<String>();
		predictions = new ArrayList<String>();
		wordShapes = new ArrayList<String>();
		mostFrequentSenses = null;
		articleID = "";
	}
	
	public String getArticleID() {
		return articleID;
	}

	public void setArticleID(String articleID) {
		this.articleID = articleID;
	}

	public String toString(){
		return this.taggedString();
	}
	
	public List<String> getWordShapes() {
		return wordShapes;
	}

	private List<String> tokens; //words and punctuation
	private List<String> stems; //stems aligned to the words (original tokens used if no stem known in WordNet)
	private List<String> posLabels; //part of speech labels
	private List<String> trueLabels; //true supersense labels (if available from training/test data)
	private List<String> predictions; //predictions made by the system
	private List<String> mostFrequentSenses; //most frequent sense labels using WordNet info
	private List<String> wordShapes; //cache of word shape features
	private String articleID;

	public String taggedString(){
		return taggedString(true);
	}
	
	
	/**
	 * creates 3-column format output
	 * 
	 * @param usePredictionsRatherThanGold
	 * @return
	 */
	public String taggedString(boolean usePredictionsRatherThanGold){
		String tok;
		String label;
		String res = "";
		String pos = "";
		for(int i=0; i<tokens.size(); i++){
			tok = tokens.get(i);
			pos = posLabels.get(i);
			if(usePredictionsRatherThanGold){
				label = predictions.get(i);
			}else{
				label = trueLabels.get(i);
			}
			res += tok +"\t"+pos+"\t"+label+"\n";
		}
		return res;
	}
	
	
	public void addToken(String token, String stem, String pos, String label) {
		stems.add(stem);
		tokens.add(token);
		posLabels.add(pos);
		trueLabels.add(label);
		predictions.add("");
		wordShapes.add(wordShape(token));
		mostFrequentSenses = null;
	}
	
	public List<String> getTokens() {
		return tokens;
	}
	public List<String> getStems() {
		return stems;
	}

	public List<String> getPOS(){
		return posLabels;
	}
	
	public List<String> getLabels() {
		return trueLabels;
	}
	
	public int length(){
		return tokens.size();
	}
	
	public List<String> getPredictions() {
		return predictions;
	}
	
	public boolean predictionsAreCorrect() {
		for(int i=0;i<trueLabels.size();i++){
			if(!predictions.get(i).equals(trueLabels.get(i))){
				return false;
			}
		}
		return true;
	}

	public void setMostFrequentSenses(List<String> mostFrequentSenses) {
		this.mostFrequentSenses = mostFrequentSenses;
	}

	public List<String> getMostFrequentSenses() {
		return mostFrequentSenses;
	}
	
	
	/**
	 * word shape feature extractor described by Ciaramita and Altun, 06
	 * 
	 * @param tok
	 * @return
	 */
	private String wordShape(String tok) {
		String res = "";
		
		
		char curChar;
		int prevCharType = -1;
		char charType = '~';
		boolean addedStar = false;
		for(int i=0; i<tok.length(); i++){
			curChar = tok.charAt(i);
			
			if(curChar >= 'A' && curChar <= 'Z'){
				charType = 'X';
			}else if(curChar >= 'a' && curChar <= 'z'){
				charType = 'x';
			}else if(curChar >= '0' && curChar <= '9'){
				charType = 'd';
			}else{
				charType = curChar;
			}
			
			if(charType == prevCharType){
				if(!addedStar){
					res += "*";
					addedStar = true;
				}
			}else{
				addedStar = false;
				res += charType;
			}
			
			prevCharType = charType;
		}

		
		
		
		return res;
	}

	public void setPredictions(List<String> predictions) {
		this.predictions = predictions;
		
	}
	
}
