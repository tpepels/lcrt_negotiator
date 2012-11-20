package agents;

import java.util.ArrayList;
import java.util.List;

import negotiator.Agent;
import negotiator.Bid;
import negotiator.actions.Action;
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
	private double lambda0 = .5; // Lambda needs an initial value
	private double lambda = .0, lambdaT = 0, lT = 0; // Acceptance treshold of agent at time t
	private double delta = .8, uMax = 1, eta = 0.9; // Maximum utility
	//
	private double reservationValue = 0;
	//
	private int[][] offerCounter;
	private double[][] issueCounter;

	/**
	 * init is called when a next session starts with the same opponent.
	 */
	@Override
	public void init() {
		int numIssues = utilitySpace.getDomain().getIssues().size();
		offerCounter = new int[numIssues][];
		issueCounter = new double[numIssues][];
		ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
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
			Value v = null;
			try {
				v = b.getValue(i + 1);
			} catch (Exception e) {				
				e.printStackTrace();
			}
			switch (v.getType()) {
			case DISCRETE:
				ValueDiscrete vd = (ValueDiscrete) v;
				break;
			case REAL:
				ValueReal vr = (ValueReal) v;
				break;
			case INTEGER:
				ValueInteger vi = (ValueInteger) v;
				break;
			default:
				break;
			}
		}
	}

	public void ReceiveMessage(Action opponentAction) {
		// Accept, EndNegotiation, IllegalAction, Offer
		if (opponentAction instanceof Offer) {
			history.add((Offer) opponentAction);
			// update counters here
		}
	}

	@Override
	public Action chooseAction() {
		for (Offer offer : history) {
			setLT(timeline.getTime());
		}
		return null;
	}

	public boolean terminateCondition(List<Offer> history, double acceptThredhold, double time,
			Offer offer, double reservation) {
		return false;
	}

	public boolean acceptCondition(List<Offer> history, double acceptThredhold, double time,
			Offer offer) {
		return true;
	}

	public Bid nextBid(Offer oppOff) {
		ArrayList<Issue> issues = utilitySpace.getDomain().getIssues();
		for (Issue lIssue : issues) {
			switch (lIssue.getType()) {
			case DISCRETE:
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) lIssue;
				break;
			case REAL:
				IssueReal lIssueReal = (IssueReal) lIssue;
				break;
			case INTEGER:
				IssueInteger lIssueInteger = (IssueInteger) lIssue;
				break;
			default:
				break;
			}
		}
		return null;
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
}
