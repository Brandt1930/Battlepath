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
package game;

import interaction.Input;
import interaction.KeyBindings;

import java.util.ArrayList;

import main.Battlepath;


import collision.CollisionSystem;
import collision.MovementSystem;

import util.Rectangle2D;
import util.SafeList;
import util.Vector2D;

import engine.Field;
import engine.GlobalInfo;
import engine.Pathplanner;
import entities.CollisionEntity;
import entities.Entity;
import entities.EntitySystem;
import entities.Projectile;
import entities.Unit;
import fx.EffectsSystem;

/**
 * Game class, managing all computations needed for the game
 */
public class Game {
	
	//TODO: Refactor, check whether we can/should make some of these private.
	public Field field;
	public Pathplanner pathPlanner;
	public CollisionSystem collisionSystem;
	public MovementSystem movementSystem;
	public EffectsSystem particleSystem;
	public Input input;
	public EntitySystem entitySystem = new EntitySystem();
	public SafeList<Entity> entities = new SafeList<Entity>();
	public ArrayList<Swarm> swarms = new ArrayList<Swarm>();
	public Rectangle2D selectionRect;
	public boolean found = false;
	
	public View view;
	public GameMode mode;
	
	boolean[] lastMouseState = new boolean[3];
	double lastShot = 0;
	
	public double dt;
	
	/**
	 * 
	 * @param startpos Start position of the first Unit.
	 */
	public Game(Vector2D startpos) {
		movementSystem = new MovementSystem(this);
		particleSystem = new EffectsSystem(this);
		entities.add(new Unit(startpos, this));
	}
	
	/**
	 * Method that should be called at least once for every frame, calls all process() methods of all game elements 
	 * @param dt time step that is computed. Passed to all elements.
	 */
	public void step(double dt) {
		this.dt = dt;
		processInput(dt);
		
		entitySystem.arrange(entities);
		
		if(entitySystem.selected().size() == 0) {
			this.setMode(GameMode.STRATEGY);
		}
		if(mode==GameMode.ACTION) {
			view.follow(entitySystem.selected().get(0));
		}
		
		for(Entity e : entities) {
			e.process(dt);
			
			if(e instanceof Unit) {
				if(selectionRect != null && !(found)) {
					if(selectionRect.inside(e.pos)) {
						((Unit) e).isSelected = true;
					}
					else {
						((Unit) e).isSelected = false;
					}
				}
			}
		}
		
		movementSystem.process(entitySystem.collisionEntities,entitySystem.units,dt);
		particleSystem.process(dt);
		
		entities.applyChanges();
		view.process(dt);
	}
	
	/**
	 * Sets the game mode. See GameMode.java for more info
	 * @param gm new mode
	 */
	public void setMode(GameMode gm) {
		switch(gm) {
		case ACTION:
			if(entitySystem.selected().size() != 0) {
				mode = gm;
				view.follow(entitySystem.selected().get(0));
			}
			break;
		case STRATEGY:
			mode = gm;
			view.unfollow();
			if(entitySystem.selected().size() != 0){
				entitySystem.selected().get(0).velocity = new Vector2D(0,0);
			}
			break;
		}
	}
	
	
	/**
	 * Switch between ACTION and STRATEGY mode.
	 */
	public void toggleMode() {
		if(mode == GameMode.ACTION) {
			setMode(GameMode.STRATEGY);
		}
		else if(mode == GameMode.STRATEGY) {
			setMode(GameMode.ACTION);
		}
	}
	
	/**
	 * Gets all units in range of the provided world position position
	 * @param pos world position
	 * @param range range radius
	 * @return set of units in range
	 */
	public ArrayList<Unit> getUnitsInRange(Vector2D pos, double range) {
		
		ArrayList<Unit> result = new ArrayList<Unit>();
		
		for(Entity e : entities) {
			if(!(e instanceof Unit)) continue;
			if(e.pos.distance(pos) < range) {
				result.add((Unit)e);
			}
		}
		return result;
	}
	
	/**
	 * Processes all user inputs, mouse and keyboard. Gets its information about that from Input class.
	 * @param dt time step
	 */
	public void processInput(double dt) {
		
		ArrayList<Unit> selected = entitySystem.selected();
		
		//Mouse
		if(mode == GameMode.STRATEGY) {
			if(input.mouseButtonPressed[0]  && !lastMouseState[0]) {
				found = false;
				for(Entity e : entities) {
					if(!(e instanceof Unit)) continue;
					Unit u = (Unit)e;
					if(input.getCursorPos().distance(e.pos) < e.getRadius()) {
						if(!u.isSelected) {
							((Unit) e).isSelected = true;
						}
						found = true;
					}
				}
				
				selectionRect = new Rectangle2D(input.getCursorPos(), input.getCursorPos());
			}
			
			if(!input.mouseButtonPressed[0]) {
				selectionRect = null;
			}
			
			if(input.mouseButtonPressed[0]  && lastMouseState[0]) {
				if(selectionRect != null)
					selectionRect.bottomright = input.getCursorPos();
			}
			
			if(input.mouseButtonPressed[2] && !lastMouseState[0] && selected.size() != 0) {
				for(Entity e : selected) {
					Unit u = (Unit)e;
					u.moveTo(input.getCursorPos());
				}
			}
		}
		
		if(mode == GameMode.ACTION) {
			if((input.mouseButtonPressed[0] && selected.size() != 0 && GlobalInfo.time - lastShot > 0.3)) {
				selected.get(0).shoot(input.getCursorPos().subtract(selected.get(0).pos).normalize());
				lastShot = GlobalInfo.time;
			}
		}
		
		System.arraycopy(input.mouseButtonPressed, 0, lastMouseState, 0, input.mouseButtonPressed.length);
		
		//Keyboard part one (input.isPressed)
		
		if(selected.size() != 0 & mode == GameMode.ACTION) {
			if(input.isPressed(KeyBindings.MOVE_LEFT)) selected.get(0).velocity.x = 1;
			else if(input.isPressed(KeyBindings.MOVE_RIGHT)) selected.get(0).velocity.x = -1;
			else selected.get(0).velocity.x = 0;
			
			if(input.isPressed(KeyBindings.MOVE_DOWN)) selected.get(0).velocity.y = -1;
			else if(input.isPressed(KeyBindings.MOVE_UP)) selected.get(0).velocity.y = 1;

			else selected.get(0).velocity.y = 0.0;
			
			if(selected.get(0).velocity.length() > 0)
				selected.get(0).velocity = selected.get(0).velocity.normalize().scalar(selected.get(0).speed);
		}
		
		if(mode == GameMode.STRATEGY) {
			if(input.isPressed(KeyBindings.SCROLL_LEFT)) view.velocity.x = -20;
			else if(input.isPressed(KeyBindings.SCROLL_RIGHT)) view.velocity.x = 20;
			else view.velocity.x = 0;
			
			if(input.isPressed(KeyBindings.SCROLL_UP)) view.velocity.y = 20;
			else if(input.isPressed(KeyBindings.SCROLL_DOWN)) view.velocity.y = -20;
			else view.velocity.y = 0;
		}
		
		//Keyboard part two (input.getKeyBuffer)
		
		for(int key : input.getKeyBuffer()) {
			switch(key) {
				case KeyBindings.ZOOM_IN:
					view.zoom(1.25*view.getTargetZoom(), true);
					break;
				case KeyBindings.ZOOM_OUT:
					view.zoom(0.8*view.getTargetZoom(), true);
					break;
				/*DEBUGGING KEYS*/
				case 't':
					toggleMode();
					break;
				case 'r':
					entities.add(new Unit(Battlepath.findStartPos(field), this));
					break;
			}
		}
		
	}
	
	/**
	 * Produces a shot at given world position and direction
	 * @param start world position of the shot to start
	 * @param direction direction to shoot
	 */
	public void emitShot(Vector2D start, Vector2D direction) {
		Projectile p = new Projectile(start, direction, this);
		entities.add(p);
	}
	
}
