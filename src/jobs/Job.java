package jobs;

import space.Space;


/**
 * Job is an encapsulation of a big purely DAC problem.
 * 
 * @param <T>
 *            type of result for a job.
 */
public interface Job<T> {

	/**
	 * Execute the job.
	 * 
	 * @param space
	 *            the space where to execute the job.
	 */
	void execute(Space space);

	/**
	 * Collect the result of job from the space and update the result of job.
	 * 
	 * @param space
	 *            the space where to collect result from.
	 */
	void collectResult(Space space);

	/**
	 * Get the result of job execution.
	 * 
	 * @return T result of the job execution.
	 */
	T getResult();
}
