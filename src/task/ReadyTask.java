package task;

import java.util.List;

import api.Task;

/**
 * 
 * A Ready Task is a task that is ready to run. In Divide and Conquer context,
 * it is a Decompose task, which, after execution, either generates a Task
 * Result (further Decompose tasks and Compose tasks) or generates a Value
 * Result (Send argument to a Compose task).
 *
 * @param <T>
 *            Argument type of the task.
 */
public abstract class ReadyTask<T> extends Task {
	private static final long serialVersionUID = -998071752240333400L;
	
	/**
	 * Target Successor Task's Argument Index.
	 */
	private int targetSuccessorTaskArgIndex;
	
	/**
	 * Argument list.
	 */
	private List<T> arg;

	/**
	 * Constructor of Ready Task.
	 * 
	 * @param arg
	 *            List of arguments.
	 * @param targetSuccessorTaskArgIndex
	 *            Target Successoor Task Argument Index.
	 */
	public ReadyTask(List<T> arg, int targetSuccessorTaskArgIndex) {
		super();
		this.arg = arg;
		this.targetSuccessorTaskArgIndex = targetSuccessorTaskArgIndex;
	}

	/**
	 * Constructor of Ready Task. Call from Client.
	 * 
	 * @param arg
	 *            Argument List.
	 */
	public ReadyTask(List<T> arg) {
		super();
		this.arg = arg;
		this.targetSuccessorTaskArgIndex = -1;
	}

	/**
	 * Get the target successor task Id.
	 * 
	 * @return The target successor task Id.
	 */
	public String getTargetSuccessorTaskId() {
		return super.getTargetTaskID();
	}

	/**
	 * Set the target successor task Id.
	 * 
	 * @param targetSuccessorTaskId
	 *            The target successor task Id to be set.
	 */
	public void setTargetSuccessorTaskId(String targetSuccessorTaskId) {
		super.setTargetTaskID(targetSuccessorTaskId);
	}

	/**
	 * Get the argument list.
	 * 
	 * @return the arg
	 */
	public List<T> getArg() {
		return arg;
	}

	/**
	 * Check if the task cannot be further decomposed or not.
	 * 
	 * @return True is the task is atomic and cannot be further decomposed.
	 *         False otherwise.
	 */
	abstract public boolean isAtomic();

	/**
	 * 
	 * Get the target successor task's argument index.
	 * 
	 * @return the targetSuccessorTaskArgIndex
	 */
	public int getTargetSuccessorTaskArgIndex() {
		return targetSuccessorTaskArgIndex;
	}

	/**
	 * Set the target successor task argument's index.
	 * 
	 * @param targetSuccessorTaskArgIndex
	 *            the targetSuccessorTaskArgIndex to set
	 */
	public void setTargetSuccessorTaskArgIndex(int targetSuccessorTaskArgIndex) {
		this.targetSuccessorTaskArgIndex = targetSuccessorTaskArgIndex;
	}

}
