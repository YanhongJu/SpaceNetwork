package result;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import api.Result;
import api.Task;
import config.Config;
import space.SpaceImpl;
import task.SuccessorTask;

/**
 * 
 * Value Result is to contain value result of a task execution.
 * 
 * @param <ValueType>
 *            is the result value type.
 */
public class ValueResult<ValueType> extends Result {
	private static final long serialVersionUID = -6238390667632362323L;

	/**
	 * Target Successor Task Id of this value result.
	 */
	private String TargetTaskId;

	/**
	 * Target Successor Argument Index of this value result.
	 */
	private int TargetArgIndex;

	/**
	 * The value result.
	 */
	private final ValueType value;

	/**
	 * Constructor of value result.
	 * 
	 * @param resultId
	 *            Result Id, same as its associated task Id.
	 * @param value
	 *            Result value.
	 * @param TargetTaskId
	 *            Id of target successor task waiting for the result.
	 * @param TargetArgIndex
	 *            Argument Index of the result value for target successor task.
	 * @param taskStartTime
	 *            Task start time.
	 * @param taskEndTime
	 *            Task End time.
	 */
	public ValueResult(String resultId, ValueType value, String TargetTaskId,
			int TargetArgIndex, long taskStartTime, long taskEndTime) {
		super(resultId, taskStartTime, taskEndTime);
		this.value = value;
		this.TargetArgIndex = TargetArgIndex;
		this.TargetTaskId = TargetTaskId;
		this.setResultType(VALUERESULT);
	}

	/**
	 * Get the target successor task ID.
	 * 
	 * @return the targetTaskId
	 */
	public String getTargetTaskId() {
		return TargetTaskId;
	}

	/**
	 * Get the target argument index of the target successor task.
	 * 
	 * @return the targetArgIndex
	 */
	public int getTargetArgIndex() {
		return TargetArgIndex;
	}

	/**
	 * Get the result value.
	 * 
	 * @return result value
	 */
	public ValueType getResultValue() {
		return value;
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
	 * 
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean process(SpaceImpl space,
			Map<String, Task> ComputerProxyRunningTaskMap,
			BlockingQueue<Result> TempResultQueue) {

		// Check if the value result is the final result. If yes, put the result
		// into Space's Result Queue.
		String taskID[] = TargetTaskId.split(":");
		if (taskID[taskID.length-1].equals("-1")) {
			space.addResult(this);
			return true;
		}

		// Get the result's target successor task. If the task is unavailabe at
		// the moment, put the result into Temporary Result Queue in Computer
		// Proxy.
		SuccessorTask<ValueType> successortask = (SuccessorTask<ValueType>) space
				.getSuccessorTask(TargetTaskId);
		if (successortask == null) {
			try {
				TempResultQueue.put(this);
				if (Config.STATUSOUTPUT) {
					System.out.println("Successor task " + TargetTaskId
							+ " is missing!");
				}
				return false;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// Set the argument in the target successor task at the target index.
		successortask.setArgAt(TargetArgIndex, this.value);

		// Check if the successor task has all needed arguments and ready to
		// run.
		if (successortask.isRunnable()) {
			// Check if the successor task can be run in Space.
			if (Config.SpaceExecutTaskFlag && successortask.isSpaceRunnable()) {
				// Space execute this successor task and store the result into
				// Computer Proxy Temporary Result Queue.
				space.spaceExecuteTask(successortask, TempResultQueue);
			} else {
				// The successor task is moved from Successor Task Queue to
				// Ready Task Queue.
				space.successorToReady(successortask);
			}
		}
		return true;
	}

}