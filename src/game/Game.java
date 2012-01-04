package game;

import interaction.Input;
import interaction.KeyBindings;

import java.util.ArrayList;

import collision.CollisionSystem;

import util.Vector2D;

import engine.Field;
import engine.Pathplanner;


public class Game {
	
	public Field f;
	Pathplanner p;
	public Unit u;
	public ArrayList<Projectile> projectiles = new ArrayList<Projectile>();
	public ArrayList<Particle> particles = new ArrayList<Particle>();
	CollisionSystem collisionSystem;
	public double dt;
	
	public Input input;
	
	public Game(Field f, Vector2D startpos) {
		this.f = f;
		this.p = new Pathplanner(f);
		u = new Unit(startpos, this);
		collisionSystem = new CollisionSystem(f,this);
	}
	

	
	public void step(double dt) {
		this.dt = dt;
		processInput();
		
		u.process(dt);
		ArrayList<Projectile> delList = new ArrayList<Projectile>();
		for (int i=0;i<projectiles.size();i++) {
			Projectile proj = projectiles.get(i);
			proj.process(dt);

			if(collisionSystem.collide(proj) != null) {
				for (int j = 0;j<=120;j++)
					particles.add(new Particle(proj.pos, Vector2D.fromAngle(j*18, 1), 0.6, Math.random()*5, this));
				delList.add(proj);
			}
		}
		projectiles.removeAll(delList);
		for (int i=0;i<particles.size();i++) {
			Particle part = particles.get(i);
			part.process(dt);
			if(part.destroyed)
				particles.remove(i);
		}
	}
	
	public void processInput() {
		if(input.getMouse1Click()) u.moveTo(input.cursorPos);
		if(input.getMouse2Click())
			projectiles.add(
					new Projectile(u.pos, input.cursorPos.subtract(u.pos),this));
		
		if(input.isPressed(KeyBindings.MOVE_LEFT)) u.velocity.x = 5;
		else if(input.isPressed(KeyBindings.MOVE_RIGHT)) u.velocity.x = -5;
		else u.velocity.x = 0;
		
		if(input.isPressed(KeyBindings.MOVE_DOWN)) u.velocity.y = 5;
		else if(input.isPressed(KeyBindings.MOVE_UP)) u.velocity.y = -5;
		else u.velocity.y = 0;
		
		
	}
	
}
