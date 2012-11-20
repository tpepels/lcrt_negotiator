package agents;

import java.util.ArrayList;
import java.util.List;

import negotiator.Agent;
import negotiator.actions.Action;
import negotiator.actions.Offer;

public class LCRTAgent extends Agent {

	private ArrayList<Offer> history = new ArrayList<Offer>();
	private double lambda0 = .5; // Lambda needs an initial value
	private double lambda = .0;
	private double delta = .8;
	
	@Override
	public Action chooseAction() {
		// 
		return null;
	}
	
	public boolean terminateCondition(List<Offer> history, double acceptThredhold, double time, Offer offer, double reservation) {
		
		return true;
	}
	
	public boolean acceptCondition(List<Offer> history, double acceptThredhold, double time, Offer offer){
		return true;
	}
	
	private final double beta = 1., gamma = 1., weight = 1.;
	private double sigma;
	
	public void setLambda(double time) {
		if(time == 0) 
			lambda = lambda0 + (1 - lambda) * Math.pow(delta, beta);
		else
			lambda = lambda + weight * (1 - lambda) * Math.pow(sigma, (time * gamma));
	}
}
