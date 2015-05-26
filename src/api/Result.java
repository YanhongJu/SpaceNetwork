package api;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import space.SpaceImpl;
import universe.UniverseImpl;

/**
 * Result is for containing result of a task execution, including the result Id,
 * task start time and end time. Each result is associated with its
 * corresponding task, both of them have the same ID.
 * 
 */
public abstract class Result implements Serializable {
	private static final long serialVersionUID = 3823660517030582914L;

	/**
	 * Value Result Type Identifier.
	 */
	public static final int VALUERESULT = 0;

	/**
	 * Task Result Type Identifier.
	 */
	public static final int TASKRESULT = 1;

	/**
	 * Result ID, same as its associated task ID.
	 */
	private String ResultId;

	/**
	 * Coarse flag.
	 */
	private boolean coarse;

	/**
	 * Task start time.
	 */
	private long taskStartTime;

	/**
	 * Task end time.
	 */
	private long taskEndTime;

	/**
	 * Result type.
	 */
	private int ResultType;

	/**
	 * Constructor of Result.
	 * 
	 * @param resultId
	 *            Result Id.
	 * @param taskStartTime
	 *            Task start time.
	 * @param taskEndTime
	 *            Task end time.
	 */
	public Result(String resultId, long taskStartTime, long taskEndTime) {
		this.ResultId = resultId;
		this.taskStartTime = taskStartTime;
		this.taskEndTime = taskEndTime;
	}

	/**
	 * Get the task Runtime.
	 * 
	 * @return the taskRuntime The Difference between task end time and task
	 *         start time.
	 */
	public long getTaskRuntime() {
		return taskEndTime - taskStartTime;
	}

	/**
	 * Get the task start time.
	 * 
	 * @return the taskStartTime Task start time.
	 */
	public long getTaskStartTime() {
		return taskStartTime;
	}

	/**
	 * Set the task start time.
	 * 
	 * @param taskStartTime
	 *            the taskStartTime to set
	 */
	public void setTaskStartTime(long taskStartTime) {
		this.taskStartTime = taskStartTime;
	}

	/**
	 * Get the task end time.
	 * 
	 * @return the taskEndTime Task end time.
	 */
	public long getTaskEndTime() {
		return taskEndTime;
	}

	/**
	 * Set the task end time.
	 * 
	 * @param taskEndTime
	 *            the taskEndTime to set
	 */
	public void setTaskEndTime(long taskEndTime) {
		this.taskEndTime = taskEndTime;
	}

	/**
	 * Get the result Id.
	 * 
	 * @return the resultId Result Id.
	 */
	public String getResultId() {
		return ResultId;
	}

	/**
	 * Get the result type.
	 * 
	 * @return the resultType 0 if it is Value Result. 1 if it is Task Result.
	 */
	public int getResultType() {
		return this.ResultType;
	}

	/**
	 * Set the Result Type. 0 for Value Result, 1 for Task Result.
	 * 
	 * @param resultType
	 *            the resultType to set
	 */
	public void setResultType(int resultType) {
		this.ResultType = resultType;
	}

	/**
	 * Process the result. Call from Computer Proxy in Space.
	 * 
	 * @param space
	 *            The Space implementation in which the result is to be
	 *            processed.
	 * @param ComputerProxyRunningTaskMap
	 *            The Running Task Map in the Computer Proxy, where the
	 *            associated task is stored.
	 * @param resultQueue
	 *            Intermediate Result Queue in which the result can be stored
	 *            after Space Direct Execution.
	 * @return The status of processing. True if processed successfully, false
	 *         otherwise.
	 */
	public abstract void process(final SpaceImpl space,
			final Map<String, Task> runningTaskMap,
			final BlockingQueue<Result> resultQueue);

	/**
	 * Process the Result. Call from Space Proxy in Universe.
	 * 
	 * @param universe
	 *            Universe
	 * @param runningTaskMap
	 *            The running Task Map in the Space Proxy, where the associated
	 *            task is stored.
	 * @return The status of processing. True if processed successfully, false
	 *         otherwise.
	 */
	public abstract void process(final UniverseImpl universe,
			final Map<String, Task> runningTaskMap);

	/**
	 * Check if the Result is coarse or not.
	 * 
	 * @return True is the Result is coarse. False otherwise.
	 */
	public boolean isCoarse() {
		return this.coarse;
	}

	/**
	 * Set if the Result is coarse or not.
	 * 
	 * @param coarse
	 *            True if the Result is coarse. False otherwise.
	 * 
	 */
	public void setCoarse(boolean coarse) {
		this.coarse = coarse;
	}

	/**
	 * Output format of Result runtime.
	 */
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(getClass());
		stringBuilder.append("\n\tExecution time:\t").append(getTaskRuntime());
		return stringBuilder.toString();
	}

}
