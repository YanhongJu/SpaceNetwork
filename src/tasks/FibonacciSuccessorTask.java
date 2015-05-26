package tasks;

import java.util.List;

import api.Result;
import result.ValueResult;
import task.SuccessorTask;

/**
 * 
 * Fibonacci Successor Task is the compose task of Fibonacci problem in Divide
 * and Conquer context.
 *
 */
public class FibonacciSuccessorTask extends SuccessorTask<Integer> {
	private static final long serialVersionUID = -4763943868568664027L;

	/**
	 * Constructor of Fibonacci successor task. Call from inside.
	 * 
	 * @param arg
	 *            Argument of the task. A single integer.
	 * @param argNum
	 *            Number of needed argument. 2
	 * @param targetSuccessorTaskId
	 *            Target successor task id, same as its parent target id.
	 * @param targetSuccessorTaskArgIndex
	 *            Target argument index, same as its parent target argument
	 *            index.
	 */
	public FibonacciSuccessorTask(List<Integer> arg, int argNum,
			String targetSuccessorTaskId, int targetSuccessorTaskArgIndex) {
		super(arg, argNum, targetSuccessorTaskId, targetSuccessorTaskArgIndex);
	}

	/**
	 * Execute the task and generate a Value Result. Calculate the sum of its
	 * arguments.
	 */
	@Override
	public Result execute() {
		long taskStartTime = System.nanoTime();
		List<Integer> list = this.getArg();
		Integer sum = 0;
		for (Integer i : list) {
			sum += i;
		}
		long taskEndTime = System.nanoTime();
		return new ValueResult<Integer>(this.getTaskID(), sum,
				this.getTargetTaskID(), this.getTargetSuccessorTaskArgIndex(),
				taskStartTime, taskEndTime);
	}
	
	@Override
	public boolean isCoarse() {
		if (getLayer() <= 3)
			return true;
		else
			return false;
	}
}
