package jobs;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import result.ValueResult;
import space.Space;
import tasks.FibonacciReadyTask;

/**
 * JobFibonacci is a class designed to solve the Fibonacci problem. It
 * calculates the Nth number in the Fibonacci sequence. This class implements
 * the {@link Job} interface.
 */
public class JobFibonacci implements Job<Integer> {

	private int N;
	private int result;

	/**
	 * The class constructor initializing the fields.
	 * 
	 * @param n
	 *            which Fibonacci number we need to calculate.
	 *
	 */
	public JobFibonacci(int n) {
		this.N = n;
	}

	/**
	 * Execute the job.
	 * 
	 * @param space
	 *            the space where to execute the job.
	 */

	@Override
	public void execute(Space space) {
		List<Integer> args = new ArrayList<Integer>();
		args.add(N);
		FibonacciReadyTask task = new FibonacciReadyTask(args);
		try {
			space.submitTask(task);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Collect the result of job from the space and update the result of job.
	 * 
	 * @param space
	 *            the space where to collect result from.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void collectResult(Space space) {
		ValueResult<Integer> r;
		try {
			r = (ValueResult<Integer>) space.getResult();
			result = r.getResultValue();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get the result of job execution.
	 * 
	 * @return T result of the job execution.
	 */
	@Override
	public Integer getResult() {
		return result;
	}

}
