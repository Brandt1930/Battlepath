/**
 * Copyright (c) 2011-2012 Henning Funke.
 * 
 * This file is part of Battlepath.
 *
 * Battlepath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Battlepath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package entities;

import java.util.ArrayList;
import java.util.Collections;

import util.SafeList;
import util.Vector2D;

public class EntitySystem {
	
	ArrayList<EntityComparator> xOrderUnits = new ArrayList<EntityComparator>();
	ArrayList<EntityComparator> xOrderEntities = new ArrayList<EntityComparator>();
	ArrayList<EntityComparator> xOrderCollisionEntities = new ArrayList<EntityComparator>();
	ArrayList<Unit> selectedUnits = new ArrayList<Unit>();
	public ArrayList<Unit> units = new ArrayList<Unit>();
	public ArrayList<CollisionEntity> collisionEntities = new ArrayList<CollisionEntity>();
	
	public void arrange(SafeList<Entity> entities) {
		xOrderUnits.clear();
		xOrderEntities.clear();
		xOrderCollisionEntities.clear();
		selectedUnits.clear();
		units.clear();
		collisionEntities.clear();
		
		for(Entity e : entities) {
			EntityComparator ec = new EntityComparator(e,1);
			xOrderEntities.add(ec);
			if(e instanceof CollisionEntity) {
				collisionEntities.add((CollisionEntity)e);
				xOrderCollisionEntities.add(ec);
			}
			if(e instanceof Unit) {
				units.add((Unit)e);
				xOrderUnits.add(ec);
				if(((Unit)e).isSelected)
					selectedUnits.add((Unit) e);
			
			}
		}
		Collections.sort(xOrderEntities);
		Collections.sort(xOrderCollisionEntities);
		Collections.sort(xOrderUnits);
	}
	
	public ArrayList<Unit> selected() {
		return selectedUnits;
	}
	
	
	
	private EntityComparator getPivot(int dimension, double value) {
		return new EntityComparator(value,dimension);
	}
	
	
	private int getStartIndex(ArrayList<EntityComparator> list, double value, int dimension) {
		int startindex = Collections.binarySearch(list, getPivot(dimension,value));
		if(startindex<0) startindex = -startindex-1;
		
		while(startindex>0 && (dimVal(list.get(startindex-1).e,dimension) >= value))
			startindex--;
		
		return startindex;
	}
	
	private double dimVal(Entity e, int dimension) {
		if(dimension ==1) return e.pos.x;
		if(dimension ==2) return e.pos.y;
		return 0;
	}
	private int getEndIndex(ArrayList<EntityComparator> list, double value, int dimension) {
		int endindex = Collections.binarySearch(list, getPivot(dimension,value));
		if(endindex<0) endindex = (-endindex)-2;
		if(endindex < 0) endindex = 0;
		
		while((endindex<list.size()-1) && (dimVal(list.get(endindex+1).e,dimension) <= value))
			endindex++;
		
		return endindex;
	}

	
	
	private ArrayList<Entity> entitiesInRange(ArrayList<EntityComparator> xOrder, Vector2D pos, double range) {
		int startindex = getStartIndex(xOrder,pos.x-range,1);
		int endindex = getEndIndex(xOrder,pos.x+range,1);
		
		ArrayList<EntityComparator> yOrder = new ArrayList<EntityComparator>();
		for(int i=startindex; i<=endindex; i++) {
			EntityComparator ec = xOrder.get(i);
			ec.setDimension(2);
			yOrder.add(ec);
		}
		Collections.sort(yOrder);
		
		ArrayList<Entity> result = new ArrayList<Entity>();
		if(yOrder.size() == 0) return result;
		
		startindex = getStartIndex(yOrder,pos.y-range,2);
		endindex = getEndIndex(yOrder,pos.y+range,2);
		
		for(int i=startindex; i<=endindex; i++) {
			Entity e = yOrder.get(i).e;
			if(e.pos.distance(pos) <= range)
				result.add(e);
		}
		
		//Debug efficiency output
		/*
		System.out.println("count in total: " + xOrder.size());
		System.out.println("count in xrange: " + yOrder.size());
		System.out.println("count in yrange: " + result.size());
		*/
		return result;
	}
	
	public ArrayList<Entity> entitiesInRange(Vector2D pos, double range) {
		return entitiesInRange(xOrderEntities, pos, range);
	}
	
	public ArrayList<CollisionEntity> collisionEntitiesInRange(Vector2D pos, double range) {
		ArrayList<CollisionEntity> ces = new ArrayList<CollisionEntity>();
		ArrayList<Entity> es = entitiesInRange(xOrderCollisionEntities,pos,range);
		for(Entity e : es) {
			if(e instanceof CollisionEntity) ces.add((CollisionEntity)e);
		}
		return ces;
	}
	
	public ArrayList<Unit> unitsInRange(Vector2D pos, double range) {
		ArrayList<Unit> us = new ArrayList<Unit>();
		ArrayList<Entity> es = entitiesInRange(xOrderUnits,pos,range);
		for(Entity e : es) {
			if(e instanceof Unit) us.add((Unit)e);
		}
		return us;
	}
}
