/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * 
 */
package com.statnlp.commons.types;

/**
 * @author wei_lu
 *
 */
public class Sentence extends TokenArray{
	
	private static final long serialVersionUID = 9100609441891803234L;
	
	private boolean haveEntityTypes;
	
	public Sentence(WordToken[] tokens) {
		super(tokens);
	}
	
	public void resetSent(WordToken[] tokens){
		this._tokens  = tokens;
	}
	
	
	@Override
	public WordToken get(int index) {
		return (WordToken)this._tokens[index];
	}
	
	public void setToken(int index, WordToken wt){
		this._tokens[index] = wt;
	}
	
	public void setRecognized(){
		this.haveEntityTypes = true;
	}
	
	public void setUnRecognized(){
		this.haveEntityTypes = false;
	}
	
	public boolean isRecognized(){
		return this.haveEntityTypes;
	}
	
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int k = 0; k<this._tokens.length; k++){
			if(k!=0) sb.append(' ');
			sb.append(this._tokens[k].getName());
		}
		sb.append("\n");
//		for(int k = 0; k<this._tokens.length; k++){
//			if(k!=0) sb.append(' ');
//			WordToken ewt = (WordToken)(this._tokens[k]);
//			sb.append(ewt.getHeadIndex()+" ");
//		}
//		sb.append("\n");
//		if(haveEntityTypes){
//			for(int k = 0; k<this._tokens.length; k++){
//				WordToken ewt = (WordToken)(this._tokens[k]);
//				sb.append(ewt.getEntity()+" ");
//			}
//		}
		return sb.toString();
	}

}
