package com.taskcommander;
import java.util.Date;

import com.google.api.client.util.DateTime;
import com.taskcommander.Global.TaskType;

/*
 * A task that has a name, a start date and an end date.
 * May also hold a Google API ID.
 * Related Google API: Calendar
 * 
 * @author Michelle Tan, ANDREAS, Sean Saito
 */

public class TimedTask extends Task {
	private Date _startDate;
	private Date _endDate;
	
	/*
	 * Creates a new TimedTask with given name, start time and end time.
	 * Throws IllegalArgumentException if null arguments are given.
	 */
	public TimedTask(String name, Date startTime, Date endTime){
		super(name, TaskType.TIMED);
		if (startTime != null && endTime !=null) {
			_startDate = startTime;
			_endDate = endTime;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	public Date getStartDate() {
		return _startDate;
	}
	
	public Date getEndDate() {
		return _endDate;
	}
	
	public void setStartDate(Date startDate) {
		_startDate = startDate;
	}
	
	public void setEndDate(Date endDate) {
		_endDate = endDate;
	}
	
	/*
	 * Compares the starting time of this task to the given task 
	 * in a chronological manner.
	 * Edit by Sean Saito
	 */
	@Override
	public int compareTo(Task otherTask) {
		return (_startDate.compareTo(otherTask.getStartDate()));
	}
}