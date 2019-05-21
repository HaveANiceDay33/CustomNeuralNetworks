package com.samuel;

import java.util.HashMap;

/**
 * Stores desired inputs and outputs for training data sets in the form of integer arrays
 * @author Samuel Munro
 *
 */
public class TrainingData {
	public HashMap<String, Float> augmentData;
	public int[] inputs;
	public int[] outArray;
	
	public TrainingData(int out[], int...inputs) {
		augmentData = new HashMap<>();
		this.inputs = inputs;
		this.outArray = out;
	}
}
