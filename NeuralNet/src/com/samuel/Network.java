package com.samuel;

import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlDrawLine;

import java.util.ArrayList;
import java.util.HashMap;

import org.newdawn.slick.Color;
import org.newdawn.slick.opengl.Texture;

import com.osreboot.ridhvl.HvlMath;
import com.osreboot.ridhvl.painter.painter2d.HvlFontPainter2D;

public class Network {
	public ArrayList<Layer> layers;
	
	/**
	 * Parent object for entire network
	 * @param nodesPerLayer
	 */
	public Network(int... nodesPerLayer) {
		layers = new ArrayList<>();
		layers.add(new Layer(nodesPerLayer[0], 0, this));
		for(int i = 1; i < nodesPerLayer.length - 1; i++) {
			layers.add(new Layer(nodesPerLayer[i], i, this));
		}
		layers.add(new Layer(nodesPerLayer[nodesPerLayer.length-1], nodesPerLayer.length-1, this));
	}
	
	public Network() {
		layers = new ArrayList<Layer>();
	}
	
	/**
	 * Returns a color based on value. More blue -> more positive, More green -> more negative
	 * @param xArg
	 * @author OS_REBOOT
	 * @return
	 */
	protected static Color toInverseTemperature(float xArg){
	    float r = HvlMath.mapl(xArg, 1f, 0f, 0f, 1f);
	    float g = HvlMath.limit(1f - Math.abs(xArg), 0f, 1f);
	    float b = HvlMath.mapl(xArg, -1f, 0f, 0f, 1f);
	    return new Color(g, r, b, Math.abs(xArg));
	} 
	
	/**
	 * Handles all network drawing
	 * @param delta
	 */
	public void draw(float delta, HvlFontPainter2D font, Texture t, float fontSize) {
		for(int i = 0; i < layers.size(); i++) {
			for(int j = 0; j < layers.get(i).numNodes; j++) {
				for(int k = 0; k < layers.get(i).nodes.get(j).connections; k++) {
					if(i > 0) {
						hvlDrawLine(layers.get(i-1).nodes.get(k).x, layers.get(i-1).nodes.get(k).y, 
								layers.get(i).nodes.get(j).x, layers.get(i).nodes.get(j).y, toInverseTemperature(layers.get(i).nodes.get(j).connectionWeights.get(k)));
					}
				}
			}
		}
		
		for(Layer l : layers) {
			l.draw(delta, font, t, fontSize);
		}
		
	}
	
	public Layer lastLayer() {
		return this.layers.get(this.layers.size()-1);
	}
	/*
	public static void deleteNetwork(Network n) {
		n = null;
	}
	*/
	private static void printNetwork(Network n) {
		for(Layer l : n.layers) {
			System.out.println("LAYER---");
			for(Node o : l.nodes) {
				
				System.out.println(o.identifier +": "+o.value);
				
			}
		}
		
		System.out.println();
	}
	
	public static Network deepCopy(Network old) {
		//printNetwork(old);
		Network n = new Network();
		for(Layer l : old.layers) {
			Layer nL = new Layer(0, l.id, n);
			nL.numNodes = l.numNodes;
			n.layers.add(nL);
			for(Node o : l.nodes) {
				Node nN = new Node(o.x, o.y, o.connections, o.type);
				nN.bias = o.bias;
				HashMap<Integer, Float> newWeights = new HashMap<>();
				for(Integer i : o.connectionWeights.keySet()) {
					newWeights.put(i, o.connectionWeights.get(i));
				}
				nN.connectionWeights = newWeights;
				nN.value = o.value;
				nN.expected = nN.expected;
				nL.nodes.add(nN);
			}
		}
		//printNetwork(n);
		return n;
	}
	
	
}	
