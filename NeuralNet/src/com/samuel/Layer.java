package com.samuel;

import java.util.ArrayList;

import org.lwjgl.opengl.Display;

public class Layer {
	
	private int verticalSpacing;
	private final int HORIZ_SPACING = 125;
	
	ArrayList<Node> nodes;
	public int numNodes, id;
	
	/**
	 * Layers contain nodes and belong to a network
	 * @param numNodes
	 * @param id
	 * @param net
	 */
	public Layer(int numNodes, int id, Network net) {
		nodes = new ArrayList<>();
		this.numNodes = numNodes;
		this.id = id;
		if(this.id == 0) {
			verticalSpacing = 40;
		} else {
			verticalSpacing = 80;
		}
		float minRange = (Display.getHeight()/2)-((numNodes-1)*verticalSpacing);
		
		for(int i = 0; i < numNodes; i++) {
			int nodeConnections = 0;
			if(net.layers.size() > 0) {
				nodeConnections = net.layers.get(id - 1).numNodes;
			}
			if(this.id == 0) {nodeConnections = 0;}
			
			nodes.add(new Node(300+id*HORIZ_SPACING, minRange + (i*verticalSpacing*2), nodeConnections, (this.id == 0) ? 0 : 1));
		}
	}
	
	public void draw(float delta) {
		for(Node n : nodes) {
			n.draw(delta);
		}
	}
}
