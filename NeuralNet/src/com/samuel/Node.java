package com.samuel;

import java.rmi.server.UID;
import java.util.HashMap;
import java.util.UUID;

import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.Texture;

import com.osreboot.ridhvl.HvlMath;
import com.osreboot.ridhvl.painter.HvlCursor;
import com.osreboot.ridhvl.painter.painter2d.HvlFontPainter2D;
import com.osreboot.ridhvl.painter.painter2d.HvlPainter2D;

public class Node {
	
	public static final float NODE_SIZE = 40;
	
	public float x, y, bias, value, connections, type, expected;
	public UID identifier;
	
	public HashMap<Integer, Float> connectionWeights;
	
	/**
	 * A node stores all information that is modified in the grand scope of the network. A type "0" node removes some attributes and graphical elements
	 * @param x
	 * @param y
	 * @param connections
	 * @param type
	 */
	public Node(float x, float y, int connections, int type) {
		this.x = x;
		this.y = y;
		this.type = type;
		this.identifier = new UID();
		 
		if(this.type == 0) {
			this.bias = 0;
			this.connections = 0;
		} else if (type == 1) {
			this.bias = HvlMath.randomFloatBetween(-1, 1);
			this.connections = connections;
		}

		this.connectionWeights = new HashMap<>();
		
		for(int i = 0; i < connections; i++) {
			this.connectionWeights.put(i, HvlMath.randomFloatBetween(-1, 1));
		}
	}
	
	public void draw(float delta, HvlFontPainter2D font, Texture t) {
		HvlPainter2D.hvlDrawQuadc(this.x, this.y, NODE_SIZE, NODE_SIZE, t, new Color(0f, (1-value), (value)));
		font.drawWordc(""+HvlMath.cropDecimals(this.value, 3), this.x, this.y, value > 0.5 ? Color.white : Color.black, 0.18f);
		for(Integer i : this.connectionWeights.keySet()) {
			//NetworkMain.font.drawWordc(""+HvlMath.cropDecimals(this.connectionWeights.get(i), 4), this.x, this.y-35-(i*20), Color.green, 0.18f);
		}
		if(this.type != 0) {
			//NetworkMain.font.drawWordc(""+HvlMath.cropDecimals(this.bias,4), this.x, this.y+40, Color.blue, 0.18f);
		}
	}
}
