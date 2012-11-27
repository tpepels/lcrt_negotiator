package agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.BidIterator;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;

public class LCRTAgent extends Agent {

	private ArrayList<Offer> history = new ArrayList<Offer>();
	private ArrayList<Issue> issues;
	private double lambda0 = .5; // Lambda needs an initial value
	private double lambda = .0, lambdaT = 0, lT = 0; // Acceptance treshold of agent at time t
	private double delta = .8, uMax = 1;
	private double epsilon = 0.01;
	private double eta = 0.9; 
	private double reservationValue = 0;
	//
	private int[][] offerCounter;
	private double[][] issueCounter;
	private Offer omegaBest = null; // To notify NB component of proposing this
									// bid next time
	private double ru0 = 0;
	private double rut = 0;

	/**
	 * init is called when a next session starts with the same opponent.
	 */
	@Override
	public void init() {
		int numIssues = utilitySpace.getDomain().getIssues().size();
		offerCounter = new int[numIssues][];
		issueCounter = new double[numIssues][];
		issues = utilitySpace.getDomain().getIssues();
		for (Issue iss : issues) {
			int i = iss.getNumber() - 1;
			switch (iss.getType()) {
			case DISCRETE:
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) iss;
				offerCounter[i] = new int[lIssueDiscrete.getNumberOfValues()];
				issueCounter[i] = new double[lIssueDiscrete.getNumberOfValues()];
				break;
			case REAL:
				IssueReal lIssueReal = (IssueReal) iss;
				offerCounter[i] = new int[lIssueReal.getNumberOfDiscretizationSteps()];
				issueCounter[i] = new double[lIssueReal.getNumberOfDiscretizationSteps()];
				break;
			case INTEGER:
				IssueInteger lIssueInteger = (IssueInteger) iss;
				offerCounter[i] = new int[lIssueInteger.getUpperBound()
						- lIssueInteger.getLowerBound()];
				issueCounter[i] = new double[lIssueInteger.getUpperBound()
						- lIssueInteger.getLowerBound()];
				break;
			default:
				break;
			}
		}
		if (utilitySpace.getReservationValue() != null)
			reservationValue = utilitySpace.getReservationValue();
	}
	
	private void updateCounters(Offer offer) {
		Bid b = offer.getBid();
		for(int i = 0; i < offerCounter.length; i++) {
			Issue iss = (Issue) utilitySpace.getDomain().getObjective(i);
			Value v = null;
			try {
				v = b.getValue(i + 1);
			} catch (Exception e) {				
				e.printStackTrace();
			}
			switch (v.getType()) {
			case DISCRETE:
				ValueDiscrete vd = (ValueDiscrete) v;
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) iss;
				offerCounter[iss.getNumber() - 1][lIssueDiscrete.getValueIndex(vd.getValue())]++;
				break;
			case REAL:
				ValueReal vr = (ValueReal) v;
				IssueReal ireal = (IssueReal) iss;
				double binsize = (ireal.getUpperBound() - ireal.getLowerBound()) / (double)ireal.getNumberOfDiscretizationSteps();
				double bin = vr.getValue() / binsize;
				offerCounter[iss.getNumber() - 1][(int)bin]++;
				break;
			case INTEGER:
				ValueInteger vi = (ValueInteger) v;
				offerCounter[iss.getNumber() - 1][vi.getValue()]++;
				break;
			default:
				break;
			}
		}
	}
	
	private void updateIssueCounters() {
		for (int i = 0; i < offerCounter.length; i++) {
			for (int j = 0; j < offerCounter[i].length; j++) {
				issueCounter[i][j] = issueCounter[i][j] + Math.pow(eta, offerCounter[i][j]); 
			}
		}
	}

	private double calculateFOmega(Bid b) {
		double f = 0;
		
		for (int i = 0; i < offerCounter.length; i++) {
			Issue iss = (Issue) utilitySpace.getDomain().getObjective(i);
			Value v = null;
			try {
				v = b.getValue(i + 1);
			} catch (Exception e) {				
				e.printStackTrace();
			}
			switch (v.getType()) {
			case DISCRETE:
				ValueDiscrete vd = (ValueDiscrete) v;
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) iss;
				f += issueCounter[iss.getNumber() - 1][lIssueDiscrete.getValueIndex(vd.getValue())];
				break;
			case REAL:
				ValueReal vr = (ValueReal) v;
				IssueReal ireal = (IssueReal) iss;
				double binsize = (ireal.getUpperBound() - ireal.getLowerBound()) / (double)ireal.getNumberOfDiscretizationSteps();
				double bin = vr.getValue() / binsize;
				f += issueCounter[iss.getNumber() - 1][(int)bin];
				break;
			case INTEGER:
				ValueInteger vi = (ValueInteger) v;
				f += issueCounter[iss.getNumber() - 1][vi.getValue()];
				break;
			default:
				break;
			}
		}
		
		return f;
	}
	
	public void ReceiveMessage(Action opponentAction) {
		// Accept, EndNegotiation, IllegalAction, Offer
		if (opponentAction instanceof Offer) {
			history.add((Offer) opponentAction);
			// update counters here
			updateCounters((Offer) opponentAction);
			updateIssueCounters();
		}
	}
	
	public Action chooseAction() {
		Action action = null;
		Offer offer;
		//Determine negotation outcome to offer (Tom's part)
		if(omegaBest != null){
			offer = omegaBest;
			omegaBest = null;
		}else{
			offer = new Offer(this, nextBid());
		}
		updateRut();
		setLT(timeline.getTime());
		 
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
		if(ru0 > lT || ru0 > getUtility(offer.getBid())) {
			return true;
		}
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

	public Bid nextBid() {
		BidIterator iterator = new BidIterator(utilitySpace.getDomain());
		ArrayList<Bid> bids = new ArrayList<Bid>();
		ArrayList<Double> fOmega = new ArrayList<Double>();
		double highestF = Double.MIN_VALUE;
		Bid omegaMax = null;
		while (iterator.hasNext()) {
			Bid bid = iterator.next();
			bids.add(bid);
			double f = calculateFOmega(bid); 
			fOmega.add(new Double(f));
			if (f > highestF) {
				highestF = f;
				omegaMax = bid;
			}
		}
		Random random = new Random();
		if (random.nextDouble() >= epsilon) {
			return omegaMax;
		} else {
			Bid bid = bids.get(random.nextInt(bids.size()));
			while (bid.equals(omegaMax)) {
				bid = bids.get(random.nextInt(bids.size()));
			}
			return bid;
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

	public void setLT(double time) {
		double alpha = 1; // Linear, boulware or conceder
		if (time < lambda) {
			lT = uMax - (uMax - uMax * Math.pow(delta, 1 - lambda))
					* Math.pow(time / lambda, alpha);
		} else {
			lT = uMax * Math.pow(delta, 1 - time);
		}
	}
	
	public void updateRut(){
		if(rut == 0){
			rut = ru0 * delta;
		}else{
			rut *= delta; 
		}
	}
}
