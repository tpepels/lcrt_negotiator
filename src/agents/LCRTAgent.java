package agents;

import java.util.ArrayList;
import java.util.List;

import negotiator.Agent;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;

public class LCRTAgent extends Agent {

	private ArrayList<Offer> history = new ArrayList<Offer>();
	private double lambda0 = .5; // Lambda needs an initial value
	private double lambda = .0;
	private double lambdaT = 0; // Acceptance treshold of agent at time t
	private double delta = .8;
	private double uMax = 1; // Maximum utility
	private Offer omegaBest = null; // To notify NB component of proposing this
									// bid next time
	private double ru0 = 0;
	private double rut = 0;

	@Override
	public Action chooseAction() {
		Action action = null;
		//TODO: Determine negotation outcome to offer (Tom's part)
		Offer offer = null;
		setLambdaT(timeline.getTime());
		//TODO: Set rut here? 
		if(history.isEmpty()){
			if(terminateCondition(history, timeline.getTime(), offer)){
				action =  offer;
			}else{
				action = new EndNegotiation();
			}
		}else{
			if(acceptCondition(history, timeline.getTime(), offer) &&
					!terminateCondition(history, timeline.getTime(), offer)){
				action = new Accept(getAgentID());
			}else if(!acceptCondition(history, timeline.getTime(), offer) &&
					terminateCondition(history, timeline.getTime(), offer)){
				action = new EndNegotiation();
			}else if(acceptCondition(history, timeline.getTime(), offer) &&
					terminateCondition(history, timeline.getTime(), offer)){
				if(getUtility(history.get(history.size() - 1).getBid()) > rut){
					action = new Accept(getAgentID());
				}else{
					action = new EndNegotiation();
				}
			}else{
				action = offer;
			}
		}
		return action;
	}

	public boolean terminateCondition(List<Offer> history, double time, Offer offer) {
		return false;
	}

	public boolean acceptCondition(List<Offer> history, double time, Offer offer) {
		Offer omega1 = history.get(history.size() - 1);
		//
		Offer bestOutcome = null;
		double maxUtil = Double.MIN_VALUE;
		for(Offer curOffer : history){
			double curUtil = getUtility(curOffer.getBid());
			if(curUtil > maxUtil){
				maxUtil = curUtil;
				bestOutcome = curOffer;
			}
		}
		//
		double omega1Util = getUtility(omega1.getBid());
		double opponentOfferUtil = getUtility(offer.getBid());
		double bestOutcomeUtil = getUtility(bestOutcome.getBid());
		if (omega1Util > lambdaT || omega1Util > opponentOfferUtil) {
			return true;
		} else if (bestOutcomeUtil > lambdaT || bestOutcomeUtil > opponentOfferUtil) {
			omegaBest = bestOutcome;
			return false;
		} else {
			return false;
		}
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
