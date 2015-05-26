package api;

import java.io.Serializable;

/**
 * 
 * Task is a data structure containing a task to be executed by Computer.
 *
 */
public abstract class Task implements Serializable {
	private static final long serialVersionUID = -1946800150903851225L;
	/**
	 * Task Id, same as its assocaited resutl Id.
	 */
	private String TaskID;

	/**
	 * Target Task Id. The successor task Id of this task.
	 */
	private String targetTaskID;

	/**
	 * Flag of whether the task can be run in Space or not. False by Default.
	 */
	private boolean isSpaceRunnable;
	
	private int layer = 0;

	/**
	 * Constructor of Task.
	 * 
	 * @param TaskId
	 *            Task Id.
	 * @param targetTaskId
	 *            Target Successor Task Id.
	 */
	public Task(String TaskId, String targetTaskId) {
		this.TaskID = TaskId;
		this.targetTaskID = targetTaskId;
		this.isSpaceRunnable = false;
	}

	/**
	 * Constructor of Task
	 * 
	 * @param TaskId
	 *            Task Id.
	 */
	public Task(String TaskId) {
		this.TaskID = TaskId;
		this.targetTaskID = null;
		this.isSpaceRunnable = false;
	}

	/**
	 * Constructor of Task
	 * 
	 */
	public Task() {
		this.TaskID = null;
		this.targetTaskID = null;
		this.isSpaceRunnable = false;
	}

	/**
	 * Execute the task.
	 * 
	 * @return Result of the task execution.
	 */
	abstract public Result execute();

	/**
	 * @return the taskId
	 */
	public String getTaskID() {
		return TaskID;
	}

	/**
	 * @param taskId
	 *            the taskId to set
	 */
	public void setTaskID(String taskId) {
		TaskID = taskId;
	}

	/**
	 * 
	 * @return True if it can be executed directly within the Space.
	 */
	public boolean isSpaceRunnable() {
		return this.isSpaceRunnable;
	}

	/**
	 * @param isSpaceRunnable
	 *            Set whether the task can run in Space(True) or not(False).
	 */
	public void setSpaceRunnable(boolean isSpaceRunnable) {
		this.isSpaceRunnable = isSpaceRunnable;
	}

	/**
	 * Get the target Task Id.
	 * 
	 * @return the targetTaskId
	 */
	public String getTargetTaskID() {
		return targetTaskID;
	}

	/**
	 * Set the target task Id.
	 * 
	 * @param targetTaskId
	 *            the targetTaskId to set
	 */
	public void setTargetTaskID(String targetTaskId) {
		this.targetTaskID = targetTaskId;
	}

	public int getLayer() {
		return layer;
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}
	
	public abstract boolean isCoarse();
}
