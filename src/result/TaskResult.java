package result;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import config.Config;
import api.Result;
import api.Task;
import space.SpaceImpl;

/**
 * 
 * Task Result is to contain task result of a task execution.
 *
 */
public class TaskResult extends Result {
	private static final long serialVersionUID = -1385375075612513989L;
	private List<Task> subTasks;
	private List<Task> runningTasks;

	/**
	 * Constructor of task result.
	 * 
	 * @param resultId
	 *            Result Id, same as its associated task Id.
	 * @param subTasks
	 *            Subtasks to be stored in Space. First task is a successor task
	 *            to be stored in Successor Task Queue, following by child ready
	 *            tasks to be stored in Ready Task Queue.
	 * @param runningTasks
	 *            The tasks to be cached in computer's local Ready Task Queue
	 *            and in Computer Proxy's Runnning Task Map.
	 * @param taskStartTime
	 *            Task start time.
	 * @param taskEndTime
	 *            Task end time.
	 */
	public TaskResult(String resultId, List<Task> subTasks,
			List<Task> runningTasks, long taskStartTime, long taskEndTime) {
		super(resultId, taskStartTime, taskEndTime);
		this.subTasks = subTasks;
		this.runningTasks = runningTasks;
		this.setResultType(TASKRESULT);
	}

	/**
	 * Get the subtasks.
	 * 
	 * @return the Subtasks
	 */
	public List<Task> getSubTasks() {
		return subTasks;
	}

	/**
	 * Get the Running tasks.
	 * 
	 * @return the runningTask
	 */
	public List<Task> getRunningTask() {
		return runningTasks;
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
	@Override
	public boolean process(final SpaceImpl space,
			final Map<String, Task> ComputerProxyRunningTaskMap,
			final BlockingQueue<Result> TempResultQueue) {
		if (Config.AmeliorationFlag) {
			// Store the running tasks in the Computer Proxy Running Task Map.
			for (int i = 0; i < runningTasks.size(); i++) {
				ComputerProxyRunningTaskMap.put(
						runningTasks.get(i).getTaskID(), runningTasks.get(i));
			}

			// First task in subtasks is a successor. Put it into Space's
			// Successor
			// Task Queue.
			space.addSuccessorTask(subTasks.get(0));

			// Put rest tasks in subtasks into Space's Ready Task Queue.
			for (int i = 1; i < subTasks.size(); i++) {
				space.addReadyTask(subTasks.get(i));
			}
		} else {
			for (int i = 0; i < runningTasks.size(); i++) {
				space.addReadyTask(runningTasks.get(i));
			}
			space.addSuccessorTask(subTasks.get(0));
			for (int i = 1; i < subTasks.size(); i++) {
				space.addReadyTask(subTasks.get(i));
			}
		}
		return true;
	}

}
