package com.samuel;

import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlDrawLine;
import static com.osreboot.ridhvl.painter.painter2d.HvlPainter2D.hvlDrawQuad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.lwjgl.opengl.Display;
import org.newdawn.slick.Color;

import com.osreboot.ridhvl.HvlMath;
import com.osreboot.ridhvl.action.HvlAction0;
import com.osreboot.ridhvl.action.HvlAction1;
import com.osreboot.ridhvl.display.collection.HvlDisplayModeDefault;
import com.osreboot.ridhvl.menu.HvlComponentDefault;
import com.osreboot.ridhvl.menu.HvlMenu;
import com.osreboot.ridhvl.menu.component.HvlArrangerBox;
import com.osreboot.ridhvl.menu.component.HvlArrangerBox.ArrangementStyle;
import com.osreboot.ridhvl.menu.component.HvlButton;
import com.osreboot.ridhvl.menu.component.HvlComponentDrawable;
import com.osreboot.ridhvl.menu.component.HvlSpacer;
import com.osreboot.ridhvl.menu.component.HvlTextBox;
import com.osreboot.ridhvl.menu.component.collection.HvlLabeledButton;
import com.osreboot.ridhvl.painter.HvlCamera2D;
import com.osreboot.ridhvl.painter.HvlCursor;
import com.osreboot.ridhvl.painter.painter2d.HvlFontPainter2D;
import com.osreboot.ridhvl.template.HvlTemplateInteg2D;

/**
 * The purpose of this project is to run neural networks and solve problems!
 * It can support any network size and runs an algorithm to find solutions based
 * on training data.
 * 
 * @author Samuel Munro
 *
 */
public class Main extends HvlTemplateInteg2D{
	
	//Graphics init
	public static final int 
	CIRCLE_INDEX = 0,
	FONT_INDEX = 1;

	static HvlFontPainter2D font;

	static HvlMenu ui;
	static HvlCamera2D camera;
	static float camX, camY;
	static Color moveColor;

	static ArrayList<HvlTextBox> inputBoxes;
	static ArrayList<TrainingData> trainingDataXOR;
	static ArrayList<TrainingData> trainingDataHalfAdder;
	static ArrayList<TrainingData> trainingDataFullAdder;

	public static Network currentNetwork;
	public static ArrayList<TrainingData> currentTrainingData;

	Network xorGate;
	Network halfAdder;
	Network fullAdder;

	public static void main(String [] args){
		new Main();
	}

	public Main(){
		super(60, 1440, 720, "Neural Network Testing", new HvlDisplayModeDefault());
	}
	
	/**
	 * This method propogates values through the network to determine the values of each node, and ultimately the
	 * final output node(s). The parameter decides whether it uses human-entered input or input given by the training data
	 * @param training
	 */
	public void propogate(boolean training) {
		if(!training) {
			for(int i = 0; i < currentNetwork.layers.get(0).numNodes; i++) {
				currentNetwork.layers.get(0).nodes.get(i).value = Float.parseFloat(ui.getFirstArrangerBox().getChildOfType(HvlTextBox.class, i).getText());
			}
		}
		
		for(int l = 0; l < currentNetwork.layers.size(); l++) {
			if(currentNetwork.layers.get(l).id != 0) {
				for(Node n : currentNetwork.layers.get(l).nodes) {
					float tempValue = 0;
					//the value of a node is all of its weights multiplied by the previous layer's node values, plus the node's bias.
					for(int i = 0; i < n.connections; i++) {
						tempValue += (n.connectionWeights.get(i) * currentNetwork.layers.get(l-1).nodes.get(i).value); 
					}
					tempValue += n.bias;
					n.value = (float) (1/(1+Math.pow(Math.E, -tempValue))); 
				}
			}
		}
	}

	/**
	 * Saves inputs and target outputs for training and calls other helper methods
	 * @param data
	 */
	public void backpropogate(ArrayList<TrainingData> data) {
		
		for(TrainingData t : data) {
			for(int i = 0; i < currentNetwork.layers.get(0).numNodes; i++) {
				currentNetwork.layers.get(0).nodes.get(i).value = t.inputs[i];
			}
			for(int j = 0; j < currentNetwork.lastLayer().numNodes; j++) {
				currentNetwork.lastLayer().nodes.get(j).expected = t.outArray[j];
			}
			
			propogate(true);
			float error = findNetworkError();
			for(Layer l : currentNetwork.layers) {
				for(Node n : l.nodes) {
					saveData(t.augmentData, n, error);
				}
			}
			//System.out.println(t.augmentData.toString());
		}
		//System.out.println();
		averageAndApply(data);
		for(TrainingData t : data) {
			t.augmentData.clear();
		}
		
	}
	
	/**
	 * Finds the network's total error by summing the errors of all the output nodes.
	 * @return
	 */
	public float findNetworkError() {
		float error = 0;
		for(Node n : currentNetwork.lastLayer().nodes) {
			error += (float) Math.pow((n.value - n.expected), 2);
		}
		return error;
	}
	
	/**
	 * Saves the data calculated by <code>calculateNudge</code> to hashmaps, where it is stored and eventually averaged with all other hashmaps 
	 * @param hash
	 * @param node
	 * @param error
	 */
	public void saveData(HashMap<String, Float> hash, Node node, float error) {
		float [] nudgesNode = calculateNudge(error, node);
		for(Integer i : node.connectionWeights.keySet()) {
			hash.put("weight"+(i+1)+"-"+node.identifier, nudgesNode[i]);
		}
		hash.put("bias-"+node.identifier, nudgesNode[nudgesNode.length-1]);
	}
	
	/**
	 * Calculates how much changing an individual value adjusts the network's overall error and returns an array with all of those values
	 * 
	 * @param standardError
	 * @param node
	 * @return
	 */
	public float[] calculateNudge(float standardError, Node node) {
		float[] nudge = new float[1+node.connectionWeights.size()];
		final float SLOPE_STEP = 0.1f;
		final float NUDGE_DISTANCE = 0.1f;

		for(Integer i : node.connectionWeights.keySet()) {
			node.connectionWeights.put(i, (float) (node.connectionWeights.get(i) + SLOPE_STEP));
			propogate(true);
			nudge[i] = NUDGE_DISTANCE * ((standardError - findNetworkError())/SLOPE_STEP);
			node.connectionWeights.put(i, (float) (node.connectionWeights.get(i) - SLOPE_STEP));
		}
		
		node.bias += SLOPE_STEP;
		propogate(true);
		nudge[nudge.length-1] = NUDGE_DISTANCE * ((standardError - findNetworkError())/SLOPE_STEP);
		node.bias -= SLOPE_STEP;
		
		return nudge;
	}
	
	/**
	 * Averages data from hashmaps and applies it all at once to the network
	 * @param data
	 */
	public void averageAndApply(ArrayList<TrainingData> data) {
		HashMap<String, Float> averages = new HashMap<>();
		for(Layer l : currentNetwork.layers) {
			for(Node n : l.nodes) {
				float biasChange = 0;
				for(int i = 0; i < n.connections; i++) {
					float weightChange = 0;
					for(int j = 0; j < data.size(); j++) {
						weightChange += data.get(j).augmentData.get("weight"+(i+1)+"-"+n.identifier);
					}
					weightChange/= data.size();
					averages.put("averageWeight"+(i+1)+"-"+n.identifier, weightChange);
				}
				for(int k = 0; k < data.size(); k++) {
					biasChange += data.get(k).augmentData.get("bias-"+n.identifier);
				}
				biasChange/=data.size();
				averages.put("averageBias-"+n.identifier, biasChange);
			}
		}
		//System.out.println(averages.toString());
		
		for(Layer l : currentNetwork.layers) {
			for(Node n : l.nodes) {
				for(int i = 0; i < n.connections; i++) {
					n.connectionWeights.put(i, averages.get("averageWeight"+(i+1)+"-"+n.identifier) + n.connectionWeights.get(i));
				}
				n.bias += averages.get("averageBias-"+n.identifier);
			}
		}
	}
	
	/**
	 * Shows the stats of each node when moused over
	 * @param n
	 */
	public static void showStats(Node n) {
		hvlDrawQuad(Display.getWidth() - 400, 0, 400, Display.getHeight(), new Color(50, 50, 50));
		font.drawWord(n.identifier.toString(), Display.getWidth()-375, 20, Color.white, 0.3f);
		font.drawWordc("Value: "+n.value, Display.getWidth()-200, 75, Color.magenta, 0.3f);
		font.drawWord("Weights:", Display.getWidth()-375, 100, Color.white, 0.3f);
		font.drawWord("Bias:", Display.getWidth()-200, 100, Color.white, 0.3f);
		font.drawWord(""+HvlMath.cropDecimals(n.bias, 4), Display.getWidth()-135, 103, Color.red, 0.26f);
		for(int i = 0; i < n.connections; i++) {
			font.drawWord(""+HvlMath.cropDecimals(n.connectionWeights.get(i), 4), Display.getWidth()-375, 140+(i*30), (i % 2 == 0) ? Color.green : Color.blue, 0.26f);
		}
	}

	/**
	 * Initializes the networks, training data, graphics, camera, and menu properties
	 */
	@Override
	public void initialize() {
		getTextureLoader().loadResource("circle");
		getTextureLoader().loadResource("osFont");
		font =  new HvlFontPainter2D(getTexture(FONT_INDEX), HvlFontPainter2D.Preset.FP_INOFFICIAL,.5f,8f,0); //font definition
		camX = Display.getWidth()/2;
		camY = Display.getHeight()/2;
		moveColor = new Color(150, 150, 150, 0.1f);
		camera = new HvlCamera2D(camX, camY, 0, 1, HvlCamera2D.ALIGNMENT_CENTER); //Camera definition
		ui = new HvlMenu();
		inputBoxes = new ArrayList<>();

		xorGate = new Network(2, 2, 1);
		halfAdder = new Network(2, 2,2);
		fullAdder = new Network(3, 5, 2);

		trainingDataXOR = new ArrayList<>(
				Arrays.asList(new TrainingData(new int[] {0}, 1, 1),  new TrainingData(new int[] {1}, 0, 1), new TrainingData(new int[] {0}, 0, 0), new TrainingData(new int[] {1} ,1, 0))
				);
		
		trainingDataHalfAdder = new ArrayList<>(
				Arrays.asList(new TrainingData(new int[]{0,0}, 0, 0), new TrainingData(new int[]{1,0}, 0, 1), new TrainingData(new int[]{1,0}, 1, 0), new TrainingData(new int[]{0,1}, 1, 1))
				);
		
		trainingDataFullAdder = new ArrayList<>(
				Arrays.asList(new TrainingData(new int[]{0,0}, 0, 0, 0), new TrainingData(new int[]{1,0}, 0, 0, 1), new TrainingData(new int[]{1,0}, 0, 1, 0), new TrainingData(new int[]{0,1}, 0, 1, 1),
						new TrainingData(new int[]{1,0}, 1, 0, 0), new TrainingData(new int[]{0, 1}, 1, 0, 1), new TrainingData(new int[]{0, 1}, 1, 1, 0), new TrainingData(new int[]{1,1},1, 1, 1))
				);

		currentNetwork = fullAdder;
		currentTrainingData = trainingDataFullAdder;

		HvlComponentDefault.setDefault(HvlLabeledButton.class, new HvlLabeledButton.Builder().setWidth(200).setHeight(30).setFont(font).setTextColor(Color.white).setTextScale(0.25f).setOnDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height,Color.lightGray);	
			}
		}).setOffDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height,Color.darkGray);
			}
		}).setHoverDrawable(new HvlComponentDrawable() {
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height,Color.gray);
			}
		}).build());

		HvlComponentDefault.setDefault(new HvlTextBox.Builder().setWidth(100).setHeight(30).setFont(Main.font).setTextColor(Color.darkGray).setTextScale(0.25f).setOffsetY(5).setOffsetX(5).setText("").setNumbersOnly(true).setFocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.lightGray);	
			}
		}).setUnfocusedDrawable(new HvlComponentDrawable() {	
			@Override
			public void draw(float delta, float x, float y, float width, float height) {
				hvlDrawQuad(x,y,width,height, Color.white);	
			}
		}).build());


		ui.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.VERTICAL).setWidth(150).setHeight(50).setX(100).setY(10).setyAlign(0).setxAlign(0).build());
		for(int i = 0; i < currentNetwork.layers.get(0).numNodes; i++) {
			font.drawWord("Input "+i+":", 0, 30+(i*10), Color.white, 0.20f);
			HvlTextBox h = new HvlTextBox.Builder().build();
			ui.getFirstArrangerBox().add(h);
			inputBoxes.add(h);
			ui.getFirstArrangerBox().add(new HvlSpacer(10,10));
		}


		ui.add(new HvlArrangerBox.Builder().setStyle(ArrangementStyle.HORIZONTAL).setWidth(800).setHeight(650).setX(Display.getWidth()/2).setY(Display.getHeight()/2).build());
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlLabeledButton.Builder().setText("Propogate").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				boolean anyWrong = true;
				for(HvlTextBox h : inputBoxes) {
					if(h.getText().equals("") || h.getText().equals("-") || h.getText().equals(".") || h.getText().equals(".-")){
						anyWrong = true;
					} else {
						anyWrong = false;
					}
				}
				if(anyWrong == false) {
					propogate(false);
				}
			}
		}).build());
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlSpacer(30, 30));
		ui.getChildOfType(HvlArrangerBox.class, 1).add(new HvlLabeledButton.Builder().setText("Backpropogate").setClickedCommand(new HvlAction1<HvlButton>() {
			@Override
			public void run(HvlButton a) {
				
				for(int i = 0; i < 10000; i++) {
					backpropogate(currentTrainingData);
				}
			}
		}).build());

		HvlMenu.setCurrent(ui);
	}

	/**
	 * Updates the canvas with graphics and text
	 */
	@Override
	public void update(float delta) {
		if(HvlMenu.getCurrent() == ui) {
			for(int i = 0; i < currentNetwork.layers.get(0).numNodes; i++) {
				font.drawWord("Input "+(i+1)+":", 10, 15+(i*40), Color.white, 0.23f);
			}
			if(HvlCursor.getCursorX() > Display.getWidth()-30 && HvlCursor.getCursorX() < Display.getWidth()-10) {camX += 20 * delta;}
			if(HvlCursor.getCursorX() < 30 && HvlCursor.getCursorX() > 10) {camX -= 20 * delta;}
			if(HvlCursor.getCursorY() > Display.getHeight()-30 && HvlCursor.getCursorY() < Display.getHeight()-10) {camY += 20 * delta;}
			if(HvlCursor.getCursorY() < 30 && HvlCursor.getCursorY() > 10) {camY -= 20 * delta;}
			if(HvlCursor.getCursorX() <= 0 || HvlCursor.getCursorX() >= Display.getWidth() || HvlCursor.getCursorY() <= 0 || HvlCursor.getCursorY() >= Display.getHeight()) {
				camX+=0;
				camY+=0;
			}
			hvlDrawQuad(10, 10, Display.getWidth(), 20, moveColor);
			hvlDrawQuad(10, 10, 20, Display.getHeight()-20, moveColor);
			hvlDrawQuad(10, Display.getHeight()-30, Display.getWidth()-20, 20, moveColor);
			hvlDrawQuad(Display.getWidth()-30, 10, 20, Display.getHeight()-20, moveColor); 
			font.drawWordc("Hover over a node to see its Value, Bias, and Weights.", Display.getWidth()/2, 60, Color.white, 0.25f);
			font.drawWord("Total Network Error (Backpropogation Only): "+HvlMath.cropDecimals((float) Math.sqrt(findNetworkError()), 6), 40, Display.getHeight() - 60, Color.white, 0.23f);
			font.drawWord("POSITIVE", 600, Display.getHeight()-60, Color.blue,0.23f);
			font.drawWord("NEGATIVE", 710, Display.getHeight()-60, Color.green,0.23f);
			
			camera.setX(camX);
			camera.setY(camY);
			camera.doTransform(new HvlAction0() { //THIS THING ALLOWS THE Main.zoom TO WORK
				@Override
				public void run() {
					currentNetwork.draw(delta);
				}
			});
			
			for(Layer l : currentNetwork.layers) {
				for(Node n : l.nodes) {
					if(HvlCursor.getCursorX() > n.x - Node.NODE_SIZE/2 - (camera.getX() - Display.getWidth()/2) && 
							HvlCursor.getCursorX() < n.x + Node.NODE_SIZE/2 - (camera.getX() - Display.getWidth()/2)&& 
							HvlCursor.getCursorY() > n.y - Node.NODE_SIZE/2 + (Display.getHeight()/2 - camera.getY())&& 
							HvlCursor.getCursorY() < n.y + Node.NODE_SIZE/2 + (Display.getHeight()/2 - camera.getY())) {
						showStats(n);
					}
				}
			}
		}

		HvlMenu.updateMenus(delta);
	}
}
