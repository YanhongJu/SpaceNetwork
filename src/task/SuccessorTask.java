package task;

import java.util.List;

import api.Task;

public abstract class SuccessorTask<T> extends Task {
	private static final long serialVersionUID = 7837344432049161528L;
	private int targetSuccessorTaskArgIndex;
	private int missingArgNum;
	private List<T> arg;

	/**
	 * Constructor of successor task. Call from inside.
	 * 
	 * @param arg
	 *            List of needed arguments. All null when initialized.
	 * @param argNum
	 *            Number of needed arguments.
	 * @param targetSuccessorTaskId
	 *            Target successor task Id.
	 * @param targetSuccessorTaskArgIndex
	 *            Target argument index of target successor task.
	 */
	public SuccessorTask(List<T> arg, int argNum, String targetSuccessorTaskId,
			int targetSuccessorTaskArgIndex) {
		super();
		this.arg = arg;
		this.missingArgNum = argNum;
		this.setTargetTaskID(targetSuccessorTaskId);
		this.setTargetSuccessorTaskArgIndex(targetSuccessorTaskArgIndex);
	}

	/**
	 * Check if the task has all needed arguments and ready to run.
	 * 
	 * @return True if the task is ready to run. False otherwise.
	 */
	synchronized public boolean isRunnable() {
		if (missingArgNum == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the number of missing arguments.
	 * 
	 * @return the missingArgNum
	 */
	synchronized public int getMissingArgNum() {
		return missingArgNum;
	}

	/**
	 * Set the number of missing number of arguments.
	 * 
	 * @param missingArgNum
	 *            the missingArgNum to set
	 */
	synchronized public void setMissingArgNum(int missingArgNum) {
		this.missingArgNum = missingArgNum;
	}

	/**
	 * Get the list of arguments.
	 * 
	 * @return the List of arguments.
	 */
	synchronized public List<T> getArg() {
		return arg;
	}

	/**
	 * Set the argument at the index position.
	 * 
	 * @param Index
	 *            Position of the argument to add.
	 * @param value
	 *            Argument value.
	 */
	synchronized public void setArgAt(int Index, T value) {
		assert Index >= 0 && value != null;
		arg.set(Index, value);
		missingArgNum--;
	}

	/**
	 * Get the target successor task Id.
	 * 
	 * @return Target successor task Id.
	 */
	public String getTargetSuccessorTaskId() {
		return super.getTargetTaskID();
	}

	/**
	 * Set target successor task Id.
	 * 
	 * @param targetSuccessorTaskId
	 *            Target successor task Id.
	 */
	public void setTargetSuccessorTaskId(String targetSuccessorTaskId) {
		super.setTargetTaskID(targetSuccessorTaskId);
	}

	/**
	 * Get the argument index of the target successor task.
	 * 
	 * @return the targetSuccessorTaskArgIndex
	 */
	synchronized public int getTargetSuccessorTaskArgIndex() {
		return targetSuccessorTaskArgIndex;
	}

	/**
	 * Set the argument index of the target successor task.
	 * 
	 * @param targetSuccessorTaskArgIndex
	 *            the targetSuccessorTaskArgIndex to set
	 */
	synchronized public void setTargetSuccessorTaskArgIndex(
			int targetSuccessorTaskArgIndex) {
		this.targetSuccessorTaskArgIndex = targetSuccessorTaskArgIndex;
	}

}
