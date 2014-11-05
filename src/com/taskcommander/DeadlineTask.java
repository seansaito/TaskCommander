package com.taskcommander;
import java.util.Date;

/**
 * A task that has a name and a deadline.
 * May also hold a Google API ID.
 * Related Google API: Tasks
 */
public class DeadlineTask extends DatedTask {
	private Date _endDate;	// Format yyyy-mm-ddTHH:MM:ss

	/**
	 * Creates a new DeadlineTask with given name and end time.
	 * Throws IllegalArgumentException if null arguments are given.
	 */
	public DeadlineTask(String name, Date endTime) {
		super(name, TaskType.DEADLINE, endTime);
		if (endTime != null) {
			_endDate = endTime;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	//@author Sean?
	/**
	 * Creates a new DeadlineTask with given name, end time and a Google ID.
	 * Throws IllegalArgumentException if null arguments are given.
	 */
	public DeadlineTask(String name, Date endTime, String googleID){
		super(name,TaskType.DEADLINE, endTime);
		this.setId(googleID);
		if (endTime != null) {
			_endDate = endTime;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	//@author A0128620M
	/**
	 * Creates a new DeadlineTask from a given DeadlineTask.
	 * Used for cloning.
	 */
	public DeadlineTask(DeadlineTask otherDeadlineTask){
		super(otherDeadlineTask.getName(),TaskType.DEADLINE, otherDeadlineTask.getEndDate());
		_endDate = otherDeadlineTask.getEndDate();
		this.setId(otherDeadlineTask.getId());
		this.setDone(otherDeadlineTask.isDone());
		this.setEdited(otherDeadlineTask.isEdited());
	}

	public Date getEndDate() {
		return _endDate;
	}

	public void setEndDate(Date endDate) {
		_endDate = endDate;
	}
	
	@Override
	public int compareTo(DeadlineTask otherTask) {
		return (_endDate.compareTo(otherTask.getEndDate()));
	}
	
	@Override
	public boolean equals(Object otherObject) {
		if (otherObject == null) {
			return false;
		}
		if (!(otherObject instanceof DeadlineTask)) {
			return false;
		}
		DeadlineTask otherDeadlineTask = (DeadlineTask) otherObject;

		if (this.getType().equals(otherDeadlineTask.getType()) && 
			this.getName().equals(otherDeadlineTask.getName()) && 
			this.getId() == otherDeadlineTask.getId() && 
			this.getEndDate().equals(otherDeadlineTask.getEndDate())) {
			return true;
		} else {
			return false;
		}
	}
}
