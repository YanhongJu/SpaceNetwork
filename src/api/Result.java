package api;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import space.SpaceImpl;

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
	 * Set the result ID, same as its assoicated Task Id.
	 * 
	 * @param resultId
	 *            the resultId to set
	 */
	public void setResultId(String resultId) {
		ResultId = resultId;
	}

	/**
	 * Get the result type.
	 * 
	 * @return the resultType 0 if it is Value Result. 1 If it is Task Result.
	 */
	public int getResultType() {
		return ResultType;
	}

	/**
	 * Set the Result Type. 0 for Value Result, 1 for Task Result.
	 * 
	 * @param resultType
	 *            the resultType to set
	 */
	public void setResultType(int resultType) {
		ResultType = resultType;
	}

	/**
	 * Process the result. Call from Space.
	 * 
	 * @param space
	 *            The Space implemetation in which the result is to be
	 *            processed.
	 * @param ComputerProxyRunningTaskMap
	 *            The Running Task Map in the Computer Proxy, where the
	 *            associated task is stored.
	 * @param TempResultQueue
	 *            Temporary Result Queue in which the result can be stored if
	 *            result processing failed.
	 * @return The status of processing. True if processed successfully, false
	 *         otherwise.
	 */
	public abstract boolean process(final SpaceImpl space,
			final Map<String, Task> ComputerProxyRunningTaskMap,
			final BlockingQueue<Result> TempResultQueue);

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
