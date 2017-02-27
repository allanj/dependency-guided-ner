package com.statnlp.projects.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Entity implements Comparable<Entity>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, Entity> Entities = new HashMap<String, Entity>();
	public static final Map<Integer, Entity> Entities_INDEX = new HashMap<Integer, Entity>();
	
	public static Entity get(String form){
		if(!Entities.containsKey(form)){
			Entity label = new Entity(form, Entities.size());
			Entities.put(form, label);
			Entities_INDEX.put(label.id, label);
		}
		return Entities.get(form);
	}
	
	public static Entity get(int id){
		return Entities_INDEX.get(id);
	}
	
	public String form;
	public int id;
	
	private Entity(String form, int id) {
		this.form = form;
		this.id = id;
	}

	@Override
	public int hashCode() {
		return form.hashCode();
	}

	public String getForm(){
		return this.form;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Entity))
			return false;
		Entity other = (Entity) obj;
		if (form == null) {
			if (other.form != null)
				return false;
		} else if (!form.equals(other.form))
			return false;
		return true;
	}
	
	public String toString(){
		return String.format("%s(%d)", form, id);
	}

	@Override
	public int compareTo(Entity o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(Entity o1, Entity o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}
