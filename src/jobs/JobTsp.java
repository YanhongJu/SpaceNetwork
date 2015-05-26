package jobs;

import java.rmi.RemoteException;
import java.util.*;

import result.ValueResult;
import space.Space;
import tasks.TspReadyTask;
import tasks.TspData;

/**
 * JobTsp is a class designed to solve the traveling salesman problem.
 * This class implements the {@link Job} interface.
 * 
 * <p>
 * <i>Reference:</i><br>
 * <a href="http://en.wikipedia.org/wiki/Travelling_salesman_problem">
 * Traveling Salesman Problem
 * </a>
 * </p>
 */
public class JobTsp implements Job<List<Integer>> {
	
	/**
	 * The <b>TspData</b> object of the created job
	 */
	TspData data;
	
	/**
	 * The number of the cities
	 */
	private final int numOfCities;

	/**
	 * The coordinates of the cities
	 */
	private final double[][] cities;

	/**
	 * The result of the EuclideanTsp job
	 */
	private List<Integer> minTour;
	
	/**
	 * The class constructor initializing the fields.
	 * 
	 * @param CITIES
	 * <p>
	 * The coordinates of the cities. Each city has a coordinate such as
	 * { 8, 1 }. <b>CITIES</b> is an array of this kind of coordinates. A valid
	 * input is shown below.
	 * </p>
	 * <i><b>e.g.</b></i><br>
	 *{ { 8, 1 }, { 7, 7 }, { 3, 3 }, { 6, 6 }, { 3, 6 } }
	 */
	public JobTsp(double[][] CITIES) {
		numOfCities = CITIES.length;
		int dataLength = CITIES[0].length;
		cities = new double [numOfCities][dataLength];

		for (int i = 0; i < numOfCities; ++i)
			for (int j = 0; j < dataLength; ++j)
				cities[i][j] = CITIES[i][j];
	}

	/**
	 * Executes the job.
	 * 
	 * @param space
	 * the space where to execute the job
	 */
	@Override
	public void execute(Space space) {
		double[][] distance = calDistance(cities);
		List<Integer> ordered = new ArrayList<Integer> ();
		ordered.add(0);
		List<Integer> unordered = new ArrayList<Integer> ();
		for (int i=1; i<numOfCities; ++i)
			unordered.add(i);
		
		data = new TspData(-8, ordered, unordered);
		
		List<TspData> args = new ArrayList<TspData> ();
		args.add(data);
		
		TspReadyTask task = new TspReadyTask(args, numOfCities, distance);
		try {
			space.submitTask(task);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Collects the result of the job from the space and updates the
	 * result of job.
	 * 
	 * @param space
	 * the space where to collect result from
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void collectResult(Space space) {
		ValueResult<TspData> r;
		try {
			r = (ValueResult<TspData>) space.getResult();
			TspData result = r.getResultValue();
			minTour = result.getOrderedCities();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		minTour.add(0);
	}

	/**
	 * Gets the result of job execution.
	 * 
	 * @return
	 * the result of the job execution
	 */
	@Override
	public List<Integer> getResult() {
		return minTour;
	}
	
	/**
	 * Calculates the distance of the cities.
	 * 
	 * @param CITIES
	 * The coordinates of the cities.
	 * @return
	 * The two-dimensional array containing the distances.
	 * For example, distance[3][4] stands for the distance
	 * from the 3rd city to the 4th city.
	 */
	private double[][] calDistance(double[][] CITIES) {
		double[][] distance = new double[numOfCities][numOfCities];

		for (int i = 0; i < numOfCities; ++i)
			for (int j = 0; j < numOfCities; ++j)
				distance[i][j] = Math.sqrt(Math.pow(
						CITIES[i][0] - CITIES[j][0], 2)
						+ Math.pow(CITIES[i][1] - CITIES[j][1], 2));
		return distance;
	}
}
