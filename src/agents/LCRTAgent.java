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
	private double lambdaT = 0; // Acceptance treshold of agent at time t
	private double delta = .8;
	private double uMax = 1; // Maximum utility

	@Override
	public Action chooseAction() {
		for(Offer offer : history){
			setLambdaT(timeline.getTime());
			
		}
		return null;
	}

	public boolean terminateCondition(List<Offer> history, double acceptThredhold, double time, Offer offer,
			double reservation) {
		return false;
	}

	public boolean acceptCondition(List<Offer> history, double acceptThredhold, double time, Offer offer) {
		return true;
	}

	private final double beta = 1., gamma = 1., weight = 1.;
	private double sigma;

	public void setLambda(double time) {
		if (time == 0)
			lambda = lambda0 + (1 - lambda) * Math.pow(delta, beta);
		else
			lambda = lambda + weight * (1 - lambda) * Math.pow(sigma, (time * gamma));
	}

	public void setLambdaT(double time) {
		double alpha = 1; // Linear, boulware or conceder
		if (time < lambda) {
			lambdaT = uMax - (uMax - uMax * Math.pow(delta, 1 - lambda)) * Math.pow(time / lambda, alpha);
		} else {
			lambdaT = uMax * Math.pow(delta, 1 - time);
		}
	}
}
