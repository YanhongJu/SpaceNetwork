package tasks;

import java.util.ArrayList;
import java.util.List;

import api.Result;
import api.Task;
import result.TaskResult;
import result.ValueResult;
import task.ReadyTask;

/**
 * 
 * FibonacciReadyTask is a decompose task of Fibonacci problem in Divide and
 * Conquer context.
 *
 */
public class FibonacciReadyTask extends ReadyTask<Integer> {
	private static final long serialVersionUID = 4457744042261357273L;

	/**
	 * Number of arguments.
	 */
	private static final int argNum = 2;

	/**
	 * Constructor of Fibonacci ready task. Call from inside.
	 * 
	 * @param arg
	 *            Argument list.
	 * @param targetSuccessorTaskArgIndex
	 *            Target compose task argument index.
	 */
	public FibonacciReadyTask(List<Integer> arg, int targetSuccessorTaskArgIndex) {
		super(arg, targetSuccessorTaskArgIndex);
	}

	/**
	 * Constructor of Fibonacci ready task. Call from job.
	 * 
	 * @param arg
	 *            Argument list with a single number.
	 */
	public FibonacciReadyTask(List<Integer> arg) {
		super(arg);
	}

	/**
	 * A Fibonacci Ready Task is atomic and cannot further decompose when the
	 * argument is less than 2.
	 */
	@Override
	public boolean isAtomic() {
		int n = ((ArrayList<Integer>) getArg()).get(0);
		if (n < 2) {
			return true;
		}
		return false;
	}

	/**
	 * Execute the task and generate the corresponding result. If the task is
	 * atomic, the result is a Value Result. Otherwise it is a Task Result.
	 * 
	 */
	@Override
	public Result execute() {
		long taskStartTime = System.nanoTime();
		// If the number is less than 2, generate Value Result.
		if (isAtomic()) {
			Integer n = ((ArrayList<Integer>) getArg()).get(0);
			long taskEndTime = System.nanoTime();
			return new ValueResult<Integer>(this.getTaskID(), n,
					this.getTargetSuccessorTaskId(),
					this.getTargetSuccessorTaskArgIndex(), taskStartTime,
					taskEndTime);
		} else {
			// If the number is not less than 2, generate Task Result.
			Integer n = ((ArrayList<Integer>) getArg()).get(0);

			// Generate first child ready task. And make it as a running task.
			List<Integer> arg = new ArrayList<Integer>();
			arg.add(n - 1);
			int argIndex = 0;
			
			FibonacciReadyTask runningtask = new FibonacciReadyTask(arg,
					argIndex);
			runningtask.setLayer(getLayer()+1);
			
			List<Task> runningtasks = new ArrayList<Task>();
			runningtasks.add(runningtask);

			// Generate second child ready task.
			arg = new ArrayList<Integer>();
			arg.add(n - 2);
			argIndex = 1;
			
			FibonacciReadyTask subtask = new FibonacciReadyTask(arg, argIndex);
			subtask.setLayer(getLayer()+1);

			// Generate successor task.
			arg = new ArrayList<Integer>();
			for (int i = 0; i < argNum; i++) {
				arg.add(null);
			}
			FibonacciSuccessorTask successorTask = new FibonacciSuccessorTask(
					arg, argNum, this.getTargetSuccessorTaskId(),
					this.getTargetSuccessorTaskArgIndex());
			
			successorTask.setLayer(getLayer());
			
			successorTask.setSpaceRunnable(true);
			List<Task> subtasks = new ArrayList<Task>();
			subtasks.add(successorTask);
			subtasks.add(subtask);

			long taskEndTime = System.nanoTime();
			return new TaskResult(this.getTaskID(), subtasks, runningtasks,
					taskStartTime, taskEndTime);
		}
	}

}
