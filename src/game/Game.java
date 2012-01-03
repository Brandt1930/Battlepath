package game;

import interaction.Input;
import interaction.KeyBindings;

import java.util.ArrayList;

import collision.CollisionDetection;

import util.Vector2D;

import engine.Field;
import engine.Pathplanner;


public class Game {
	
	public Field f;
	Pathplanner p;
	public Unit u;
	public ArrayList<Projectile> projectiles = new ArrayList<Projectile>();
	public ArrayList<Particle> particles = new ArrayList<Particle>();
	CollisionDetection cd;
	
	public Input input;
	
	public Game(Field f, Vector2D startpos) {
		this.f = f;
		this.p = new Pathplanner(f);
		u = new Unit(startpos, this);
		cd = new CollisionDetection(f);
	}
	

	
	public void step(double dt) {
		
		processInput();
		
		u.process(dt);
		ArrayList<Projectile> delList = new ArrayList<Projectile>();
		for (int i=0;i<projectiles.size();i++) {
			Projectile proj = projectiles.get(i);
			proj.process(dt);
			//I am aware of the fact that this is executed multiple times - this is just testing until we have collision
			// (btw: looks cooler than single execution^^)
			Unit dummy = new Unit(proj.pos,this);
			dummy.velocity = proj.direction.scalar(dt);
			dummy.radius = 0.1;
			if(cd.collide(dummy,dummy.velocity) != null) {
				for (int j = 0;j<=1120;j++)
					particles.add(new Particle(proj.pos, Vector2D.fromAngle(j*18, 1), 0.6, Math.random()*5));
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
					new Projectile(u.pos, input.cursorPos.subtract(u.pos)));
		
		if(input.isPressed(KeyBindings.MOVE_LEFT)) u.velocity.x = 8;
		else if(input.isPressed(KeyBindings.MOVE_RIGHT)) u.velocity.x = -8;
		else u.velocity.x = 0;
		
		if(input.isPressed(KeyBindings.MOVE_DOWN)) u.velocity.y = 8;
		else if(input.isPressed(KeyBindings.MOVE_UP)) u.velocity.y = -8;
		else u.velocity.y = 0;
		
		
	}
	
}
