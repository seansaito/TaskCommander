package com.taskcommander;
import java.util.Date;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.taskcommander.Global.CommandType;


/**
 * This class represents the Data component. Besides storing the data temporary, it also contains
 * all of the methods needed to manipulate the task objects within the temporary list. At the beginning 
 * the content of the permanent storage is pulled to the temporary one. After each command the data 
 * will be pushed to the permanent storage.
 * 
 * @author A0128620M, A0109194A
 */

public class Data {
	
	/* ========================= Constructor, Variables and Logger ================================== */

	/**
	 * Logger and related logging messages
	 * @author A0128620M
	 */
	private static Logger logger = Logger.getLogger(Controller.class.getName());	//TODO add logs
	
	/** 
	 * This Array contains all available task objects.
	 * @author A0128620M
	 */
	private ArrayList<Task> tasks;
	
	/**
	 * This Array contains all the deleted tasks, needed by the GoogleAPI.
	 * @author A0109194A
	 */
	private ArrayList<Task> deletedTasks;
	
	/**
	 * Stores all the tasks that were deleted in a clear command.
	 * @author A0109194A
	 */
	private Stack<ArrayList<Task>> clearedTasks;
	
	/**
	 * Stores history of added tasks for undo command.
	 * @author A0109194A
	 */
	private Stack<Task> addedTasks;
	
	/**
	 * Stores history of tasks before being updated.
	 * @author A0109194A
	 */
	private Stack<Task> preupdatedTasks;
	
	/**
	 * Stores the history of tasks after being updated.
	 * @author A0109194A
	 */
	private Stack<Task> updatedTasks;
	
	/**
	 * This Stack contains the history of all operations.
	 * @author A0109194A
	 */
	private Stack<CommandType> operationHistory;
	
	/**
	 * This Stack contains the history of all operations.
	 * @author A0109194A
	 */
	private Stack<Global.CommandType> undoHistory;
	
	/**
	 * This variable is initialized with the one and only instance of the Parser class.
	 * @author A0128620M
	 */
	private static Data theOne;
	
	/**
	 * Private Constructor, which is only called by the getInstance() method.
	 * @author A0109194A, A0128620M
	 */
	private Data() {
		tasks = new ArrayList<Task>();
		deletedTasks = new ArrayList<Task>();
		addedTasks = new Stack<Task>();
		preupdatedTasks = new Stack<Task>();
		updatedTasks = new Stack<Task>();
		clearedTasks = new Stack<ArrayList<Task>>();
		operationHistory = new Stack<Global.CommandType>();
		undoHistory = new Stack<Global.CommandType>();
		
		loadFromPermanentStorage();
	}

	/* ============================================ API ============================================= */
	
	/**
	 * This operation which returns either a new instance of the Controller or an existing one, if any.
	 * Therefore, it ensures that there will be only one instance of the Controller (see Singleton pattern)
	 * @author A0128620M
	 */
	public static Data getInstance(){
		if (theOne == null) {    
			theOne = new Data();
		}
		return theOne;
	}

	/* -------------------------------------- Add (Internal) ---------------------------------------- */

	/**
	 * This operation adds a Timed Task to the temporary tasks list.
	 * 
	 * @param 	taskName  
	 * @param 	startDate    
	 * @param 	endDate   
	 * @return 	feedback for UI
	 * @author 	A0128620M, A0109194A
	 */
	public String addTimedTask(String taskName, Date startDate, Date endDate) {
		TimedTask timedTask = new TimedTask(taskName,startDate,endDate);
		
		saveToOperationHistory(Global.CommandType.ADD);
		tasks.add(timedTask);
		addedTasks.push(timedTask);
		
		saveToPermanentStorage();	// TODO: Observer?
		
		if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
			return String.format(Global.MESSAGE_ADDED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
		} else {
			return String.format(Global.MESSAGE_ADDED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
		}
	}
	
	/**
	 * This operation adds a Deadline Task to the tasks list.
	 * 
	 * @param 	taskName     
	 * @param 	endDate    
	 * @return 	feedback for UI
	 * @author 	A0128620M, A0109194A
	 */
	public String addDeadlineTask(String taskName, Date endDate) {
		DeadlineTask deadlineTask = new DeadlineTask(taskName, endDate);

		saveToOperationHistory(Global.CommandType.ADD);
		tasks.add(deadlineTask);
		addedTasks.push(deadlineTask);
		
		saveToPermanentStorage();
		
		return String.format(Global.MESSAGE_ADDED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
	}
	
	/**
	 * This operation adds a Floating Task to the tasks list.
	 * 
	 * @param 	taskName        
	 * @return 	feedback for UI
	 * @author 	A0128620M, A0109194A
	 */
	public String addFloatingTask(String taskName) {
		FloatingTask floatingTask = new FloatingTask(taskName);
		
		saveToOperationHistory(Global.CommandType.ADD);
		tasks.add(floatingTask);
		addedTasks.push(floatingTask);
		
		saveToPermanentStorage();
		
		return String.format(Global.MESSAGE_ADDED,"\"" + floatingTask.getName() + "\"");
	}
	
	/* --------------------------------------- Add (Google) ----------------------------------------- */
	
	/**
	 * This operation adds a Task to the tasks list by forwarding the task's attributes to the respective
	 * add method. It is usually called by the SyncHandler class.
	 * 
	 * @param 	task     
	 * @return 	feedback for UI
	 * @author A0109194A
	 */
	public String addTask(Task task) {
		logger.log(Level.INFO, "Called addTask(Task task)");
		assert task.getId() != null;
		switch ( task.getType()) {
		case FLOATING:
			FloatingTask floatingTask = (FloatingTask) task;
			return addFloatingTask(floatingTask.getName(), task.getId());
		case DEADLINE:
			DeadlineTask deadlineTask = (DeadlineTask) task;
			return addDeadlineTask(deadlineTask.getName(), deadlineTask.getEndDate(), task.getId());
		default:
			TimedTask timedTask = (TimedTask) task;
			return addTimedTask(timedTask.getName(), timedTask.getStartDate(), timedTask.getEndDate(), task.getId());
		}
	}
	
	/**
	 * This operation adds a Timed Task to the tasks list and sets an ID to it.
	 * 
	 * @param 	taskName
	 * @param 	startDate
	 * @param 	endDate
	 * @param 	id
	 * @return 	feedback for UI
	 * @author A0109194A
	 */
	public String addTimedTask(String taskName, Date startDate, Date endDate, String googleID) {	
		TimedTask timedTask = new TimedTask(taskName,startDate,endDate, googleID);
		tasks.add(timedTask);
		
		saveToPermanentStorage();
		
		if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
			return String.format(Global.MESSAGE_ADDED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
		} else {
			return String.format(Global.MESSAGE_ADDED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
		}
	}
	
	/**
	 * This operation adds a Deadline Task to the tasks list and sets a GoogleID to it.
	 * 
	 * @param taskName
	 * @param endDate
	 * @param googleID
	 * @return feedback for UI
	 * @author A0109194A
	 */
	public String addDeadlineTask(String taskName, Date endDate, String googleID) {
		DeadlineTask deadlineTask= new DeadlineTask(taskName, endDate, googleID);
		tasks.add(deadlineTask);
		
		saveToPermanentStorage();
		
		return String.format(Global.MESSAGE_ADDED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
	}
	
	/**
	 * This operation adds a Floating Task  to the tasks list and sets a GoogleID to it.
	 * 
	 * @param taskName
	 * @param googleID
	 * @return feeback for UI
	 * @author A0109194A
	 */
	public String addFloatingTask(String taskName, String googleID) {
		FloatingTask floatingTask = new FloatingTask(taskName, googleID);
		tasks.add(floatingTask);
		
		saveToPermanentStorage();
		
		return String.format(Global.MESSAGE_ADDED,"\"" + floatingTask.getName() + "\"");
	}
	
	/* ------------------------------------- Update (Internal) -------------------------------------- */

	/**
	 * This operation updates a TimedTask with the given index and replaces the old taskName, 
	 * startDate or endDate respectively and changes the taskType if needed.
	 * If a given date or name parameter equals null, the old value remains.
	 * 
	 * @param index  index of the task to delete, as a string
	 * @param taskName     description of task
	 * @param startDate    
	 * @param endDate      
	 * @return             feedback for UI
	 * @author 	A0128620M, A0109194A
	 */
	public String updateToTimedTask(int index, String name, Date startDate, Date endDate) {
		assert index < tasks.size();
		if (tasks.isEmpty()) {
			return String.format(Global.MESSAGE_EMPTY);
		} 
		
		Task relatedTask = tasks.get(index);
		
		if  (!relatedTask.getType().equals(Task.TaskType.TIMED)) {
			assert startDate != null;
			assert endDate != null;
			if (name == null) {
				name = relatedTask.getName();
			}
			TimedTask timedTask = new TimedTask(name,startDate,endDate);
			// timedTask.setEdited(tasks.get(index).isEdited());  TODO: @Sean: do we need that?
			timedTask.setDone(relatedTask.isDone());
		
			saveToOperationHistory(Global.CommandType.UPDATE);
			deletedTasks.add(relatedTask);
			preupdatedTasks.push(relatedTask);
			tasks.remove(index);
			tasks.add(index, timedTask);
			updatedTasks.push(timedTask);
			
			saveToPermanentStorage();
			
			if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
				return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
			} else {
				return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
			}	
		} else {
			TimedTask timedTask = (TimedTask) relatedTask;
			
			saveToOperationHistory(Global.CommandType.UPDATE);
			preupdatedTasks.push(timedTask);
			if (name != null) {
				timedTask.setName(name);
			}
			if (startDate != null) {
				timedTask.setStartDate(startDate);
			}
			if (endDate != null) {
				timedTask.setEndDate(endDate);
			}
			timedTask.setEdited(true);
			updatedTasks.push(timedTask);
			
			saveToPermanentStorage();
			
			if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
				return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
			} else {
				return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
			}	
		}
		
	}
	
	/**
	 * This operation updates a DeadlineTask with the given index and replaces the old taskName, 
	 * startDate or endDate respectively and changes the taskType if needed.
	 * If a given date or name parameter equals null, the old value remains.
	 * 
	 * @param index   index of the task to delete, as a string
	 * @param taskName     description of task   
	 * @param endDate      
	 * @return             feedback for UI
	 * @author 	A0128620M, A0109194A
	 */
	public String updateToDeadlineTask(int index, String name, Date endDate) {
		assert index < tasks.size();
		if (tasks.isEmpty()) {
			return String.format(Global.MESSAGE_EMPTY);
		} 
		
		Task relatedTask = tasks.get(index);

		if  (relatedTask.getType() != Task.TaskType.DEADLINE) {
			assert endDate != null;
			if (name == null) {
				name = relatedTask.getName();
			}
			DeadlineTask deadlineTask = new DeadlineTask(name,endDate);
			// deadlineTask.setEdited(relatedTask.isEdited());  TODO: @Sean: do we need that?
			deadlineTask.setDone(relatedTask.isDone());
		
			saveToOperationHistory(Global.CommandType.UPDATE);
			deletedTasks.add(relatedTask);
			preupdatedTasks.push(relatedTask);
			tasks.remove(index);
			tasks.add(index, deadlineTask);
			updatedTasks.push(deadlineTask);
			
			saveToPermanentStorage();
			
			return String.format(Global.MESSAGE_UPDATED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
		} else {
			DeadlineTask deadlineTask = (DeadlineTask) relatedTask;
			
			saveToOperationHistory(Global.CommandType.UPDATE);
			preupdatedTasks.push(deadlineTask);
			if (name != null) {
				deadlineTask.setName(name);
			}
			if (endDate != null) {
				deadlineTask.setEndDate(endDate);
			}
			deadlineTask.setEdited(true);
			updatedTasks.push(deadlineTask);
			
			saveToPermanentStorage();
			
			return String.format(Global.MESSAGE_UPDATED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
			}
	}
	
	/**
	 * This operation updates a FloatingTask with the given index and replaces the old taskName, 
	 * startDate or endDate respectively and changes the taskType if needed.
	 * If a given date or name parameter equals null, the old value remains.
	 * 
	 * @param index   index of the task to delete, as a string
	 * @param taskName     description of task    
	 * @return             feedback for UI
	 * @author 	A0128620M, A0109194A
	 */
	public String updateToFloatingTask(int index, String name) {
		assert index < tasks.size();
		if (tasks.isEmpty()) {
			return String.format(Global.MESSAGE_EMPTY);
		} 
		
		Task relatedTask = tasks.get(index);
		
		if  (relatedTask.getType() != Task.TaskType.FLOATING) {
			if (name == null) {
				name = relatedTask.getName();
			}
			FloatingTask floatingTask = new FloatingTask(name);
			// floatingTask.setEdited(relatedTask.isEdited());  TODO: @Sean: do we need that?
			floatingTask.setDone(relatedTask.isDone());
		
			saveToOperationHistory(Global.CommandType.UPDATE);
			deletedTasks.add(relatedTask);
			preupdatedTasks.push(relatedTask);
			tasks.remove(index);
			tasks.add(index, floatingTask);
			updatedTasks.push(floatingTask);
			
			saveToPermanentStorage();
			
			return String.format(Global.MESSAGE_UPDATED,"\"" + floatingTask.getName() + "\"");
		} else {
			FloatingTask floatingTask = (FloatingTask) relatedTask;
			
			saveToOperationHistory(Global.CommandType.UPDATE);
			preupdatedTasks.push(floatingTask);
			if (name != null) {
				floatingTask.setName(name);
			}
			floatingTask.setEdited(true);
			updatedTasks.push(floatingTask);
			
			saveToPermanentStorage();
			
			return String.format(Global.MESSAGE_UPDATED,"\"" + floatingTask.getName() + "\"");
			}
	}
	
	/* ------------------------------------- Update (Google) --------------------------------------- */
	
	/**
	 * This operation updates a task with a TimedTask object as a parameter
	 * It is usually called by the SyncHandler
	 * 
	 * @param 		index
	 * @param 		task
	 * @return		Feedback for user
	 * @author 		A0109194A
	 */
	public String updateToTimedTask(int index, TimedTask task) {
		Task relatedTask = tasks.get(index);
		
		if (relatedTask.getType() != Task.TaskType.TIMED) {
			TimedTask timedTask = new TimedTask(task.getName(), task.getStartDate(), task.getEndDate());
			timedTask.setEdited(relatedTask.isEdited());
			timedTask.setDone(relatedTask.isDone());
			timedTask.setUpdated(task.getUpdated());
			deletedTasks.add(relatedTask);
			tasks.remove(index);
			tasks.add(index, timedTask);
			saveToPermanentStorage();
			return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate()) + "]"+ " \"" + timedTask.getName() + "\"");
		} else {
			TimedTask timedTask = (TimedTask) tasks.get(index);
			if (task.getName() != null) {
				timedTask.setName(task.getName());
			}
			if (task.getStartDate() != null) {
				timedTask.setStartDate(task.getStartDate());
			}
			if (task.getEndDate() != null) {
				timedTask.setEndDate(task.getEndDate());
			}
			timedTask.setEdited(true);
			timedTask.setUpdated(task.getUpdated());
			saveToPermanentStorage();
			if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
				return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
			} else {
				return String.format(Global.MESSAGE_UPDATED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
			}		
		}
	}
		
	/**
	 * This operation updates a task with a DeadlineTask object as a parameter
	 * It is usually called by the SyncHandler
	 * 
	 * @param 		index
	 * @param 		task
	 * @return		Feedback for user
	 * @author 		A0109194A
	 */
	public String updateToDeadlineTask(int index, DeadlineTask task) {
		if  (tasks.get(index).getType() != Task.TaskType.DEADLINE) {
			Task toChange = tasks.get(index);
			DeadlineTask deadlineTask = new DeadlineTask(task.getName(),task.getEndDate());
			deadlineTask.setEdited(tasks.get(index).isEdited());
			deadlineTask.setDone(tasks.get(index).isDone());
			deadlineTask.setUpdated(task.getUpdated());
			deletedTasks.add(toChange);
			tasks.remove(index);
			tasks.add(index, deadlineTask);
			saveToPermanentStorage();
			return String.format(Global.MESSAGE_UPDATED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
		} else {
			DeadlineTask deadlineTask = (DeadlineTask) tasks.get(index);
			preupdatedTasks.push(deadlineTask);
			if (task.getName() != null) {
				deadlineTask.setName(task.getName());
			}
			if (task.getEndDate() != null) {
				deadlineTask.setEndDate(task.getEndDate());
			}
			deadlineTask.setEdited(true);
			deadlineTask.setUpdated(task.getUpdated());
			saveToPermanentStorage();
			return String.format(Global.MESSAGE_UPDATED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
			}
	}
	
	/**
	 * This operation updates a task with a FloatingTask object as a parameter
	 * It is usually called by the SyncHandler
	 * 
	 * @param 		index
	 * @param 		task
	 * @return		Feedback for user
	 * @author 		A0109194A
	 */
	public String updateToFloatingTask(int index, FloatingTask task) {
		if  (tasks.get(index).getType() != Task.TaskType.FLOATING) {
			Task toChange = tasks.get(index);
			FloatingTask floatingTask = new FloatingTask(task.getName());
			floatingTask.setEdited(tasks.get(index).isEdited());
			floatingTask.setDone(tasks.get(index).isDone());
			floatingTask.setUpdated(task.getUpdated());
			deletedTasks.add(toChange);
			tasks.remove(index);
			tasks.add(index, floatingTask);
			saveToPermanentStorage();
			return String.format(Global.MESSAGE_UPDATED,"\"" + floatingTask.getName() + "\"");
		} else {
			FloatingTask floatingTask = (FloatingTask) tasks.get(index);
			preupdatedTasks.push(floatingTask);
			if (task.getName() != null) {
				floatingTask.setName(task.getName());
			}
			floatingTask.setEdited(true);
			floatingTask.setUpdated(task.getUpdated());
			saveToPermanentStorage();
			return String.format(Global.MESSAGE_UPDATED,"\"" + floatingTask.getName() + "\"");
		}
	}
	

	/* -------------------------------------- Done (Internal) --------------------------------------- */

	/**
	 * This operation marks a task as done.
	 * 
	 * @param index        index of the done task 
	 * @return             feedback for UI
	 * @author 	A0128620M
	 */
	public String done(int index) {
		if (tasks.isEmpty()) {
			return String.format(Global.MESSAGE_EMPTY);
		} 

		if (index > tasks.size() - Global.INDEX_OFFSET || index < 0 ) {
			return String.format(Global.MESSAGE_NO_INDEX, index);
		}
		
		Task doneTask = tasks.get(index);
		if (doneTask.isDone()) {
			return String.format(Global.MESSAGE_ALREADY_DONE);
		} else {
			doneTask.markDone();
			saveToPermanentStorage();
			switch ( doneTask.getType()) {
			case FLOATING:
				FloatingTask floatingTask = (FloatingTask) doneTask;
				return String.format(Global.MESSAGE_DONE,"\"" + floatingTask.getName() + "\"");
			case DEADLINE:
				DeadlineTask deadlineTask = (DeadlineTask) doneTask;
				return String.format(Global.MESSAGE_DONE,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
			default:
				TimedTask timedTask = (TimedTask) doneTask;// TODO: find better solution than default
				if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
					return String.format(Global.MESSAGE_DONE,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
				} else {
					return String.format(Global.MESSAGE_DONE,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
				}			
			}
		}
	}
	
	/* ------------------------------------- Open (Internal) -------------------------------------- */
	/**
	 * This operation marks a task as open.
	 * 
	 * @param index        index of the open tasks   
	 * @return             feedback for UI
	 * @author 	A0128620M
	 */
	public String open(int index) {
		if (tasks.isEmpty()) {
			return String.format(Global.MESSAGE_EMPTY);
		} 

		if (index > tasks.size() - Global.INDEX_OFFSET || index < 0 ) {
			return String.format(Global.MESSAGE_NO_INDEX, index);
		}
		
		Task openTask = tasks.get(index);
		if (!openTask.isDone()) {
			return String.format(Global.MESSAGE_ALREADY_OPEN);
		} else {
			openTask.markOpen();
			saveToPermanentStorage();
			switch ( openTask.getType()) {
			case FLOATING:
				FloatingTask floatingTask = (FloatingTask) openTask;
				return String.format(Global.MESSAGE_OPEN,"\"" + floatingTask.getName() + "\"");
			case DEADLINE:
				DeadlineTask deadlineTask = (DeadlineTask) openTask;
				return String.format(Global.MESSAGE_OPEN,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
			default:
				TimedTask timedTask = (TimedTask) openTask;// TODO: find better solution than default
				if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
					return String.format(Global.MESSAGE_OPEN,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
				} else {
					return String.format(Global.MESSAGE_OPEN,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
				}	
			}
		}
	}
	
	/* ------------------------------------- Delete (Internal) -------------------------------------- */
	/**
	 * This operation deletes the task with the given index (as shown with 'display' command).
	 * Does not execute if there are no lines and if a wrong index is given.
	 * 
	 * @param index        Index of the task to delete, as a string. 
	 * @return             Feedback for user.
	 * @author 	A0128620M, A0109194A
	 */
	public String deleteTask(int index) {
		if (tasks.isEmpty()) {
			return String.format(Global.MESSAGE_EMPTY);
		} 

		if (index > tasks.size() - Global.INDEX_OFFSET || index < 0 ) {
			return String.format(Global.MESSAGE_NO_INDEX, index);
		} else {
			Task deletedTask = tasks.get(index);
			saveToOperationHistory(Global.CommandType.DELETE);
			deletedTasks.add(deletedTask);
			tasks.remove(index);
			saveToPermanentStorage();
			switch ( deletedTask.getType()) {		// TODO: Extract to method
			case FLOATING:
				FloatingTask floatingTask = (FloatingTask) deletedTask;
				return String.format(Global.MESSAGE_DELETED,"\"" + floatingTask.getName() + "\"");
			case DEADLINE:
				DeadlineTask deadlineTask = (DeadlineTask) deletedTask;
				return String.format(Global.MESSAGE_DELETED,"[by "+ Global.dayFormat.format(deadlineTask.getEndDate())+ " "+ Global.timeFormat.format(deadlineTask.getEndDate()) + "]"+ " \"" + deadlineTask.getName() + "\"");
			default:
				TimedTask timedTask = (TimedTask) deletedTask;// TODO: find better solution than default
				if(Global.dayFormat.format(timedTask.getStartDate()).equals(Global.dayFormat.format(timedTask.getEndDate()))) {
					return String.format(Global.MESSAGE_DELETED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\""); 
				} else {
					return String.format(Global.MESSAGE_DELETED,"["+ Global.dayFormat.format(timedTask.getStartDate())+ " "+ Global.timeFormat.format(timedTask.getStartDate())+ "-"+ Global.dayFormat.format(timedTask.getEndDate()) +" "+ Global.timeFormat.format(timedTask.getEndDate())+ "]"+ " \"" + timedTask.getName() + "\"");
				}			
			}
		}
	}
	
	/* ------------------------------------- Delete (Google) -------------------------------------- */
	
	/**
	 * This operation deletes the task directly from the tasks list without the index.
	 * Used to delete tasks when syncing.
	 * 
	 * @param 	task
	 * @return 	boolean value on whether the delete was successful.
	 * @author A0109194A
	 */
	public boolean deleteTask(Task task) {
		if (tasks.isEmpty()) {
			return false;
		} else {
			deletedTasks.add(task);
			tasks.remove(task);
			saveToPermanentStorage();
			return true;
		}
	}
	
	/* ------------------------------------- Undo (Internal) -------------------------------------- */
	
	/**
	 * This operation undoes the latest command
	 * It supports Add, Delete, Update, and Clear commands
	 * @return Feedback for UI
	 * @author A0109194A
	 */
	public String undo() {
		if (operationHistory.empty()) {
			return Global.MESSAGE_UNDO_EMPTY;
		}
		Global.CommandType type = operationHistory.pop();
		Global.CommandType undoCommand;
		switch(type) {
		case ADD:
			undoCommand = Global.CommandType.DELETE;
			saveToUndoHistory(undoCommand);
			undoAdd();
			break;
		case DELETE:
			undoCommand = Global.CommandType.ADD;
			saveToUndoHistory(undoCommand);
			undoDelete();
			break;
		case UPDATE:
			undoCommand = Global.CommandType.UPDATE;
			saveToUndoHistory(undoCommand);
			undoUpdate();
			break;
		case CLEAR:
			undoCommand = Global.CommandType.UNCLEAR;
			saveToUndoHistory(undoCommand);
			undoClear();
			break;
		default:
			undo(); //Calls undo again to look for one of the four commands above
		}
		saveToPermanentStorage();
		return String.format(Global.MESSAGE_UNDONE, type);
	}
	
	/**
	 * This operation undoes the add command
	 * @return Boolean
	 * @author A0109194A
	 */
	private boolean undoAdd() {
		Task toDelete = addedTasks.pop();
		switch (toDelete.getType()) {
		case TIMED:
			tasks.remove((TimedTask) toDelete);
			return true;
		case DEADLINE:
			tasks.remove((DeadlineTask) toDelete);
			return true;
		case FLOATING:
			tasks.remove((FloatingTask) toDelete);
			return true;
		}
		return false;
	}
	
	/**
	 * This operation undoes the delete command
	 * @return Boolean
	 * @author A0109194A
	 */
	private boolean undoDelete() {
		Task toAdd = deletedTasks.get(deletedTasks.size() - 1);
		deletedTasks.remove(deletedTasks.size() - 1);
		switch (toAdd.getType()) {
		case TIMED:
			tasks.add((TimedTask) toAdd);
			return true;
		case DEADLINE:
			tasks.add((DeadlineTask) toAdd);
			return true;
		case FLOATING:
			tasks.add((FloatingTask) toAdd);
			return true;
		}
		return false;
	}
	
	/**
	 * This operation undoes the update command
	 * @return Boolean
	 * @author A0109194A
	 */
	private boolean undoUpdate() {
		Task updated = updatedTasks.pop();
		Task beforeUpdate = preupdatedTasks.pop();
		if (updated.getType() != beforeUpdate.getType()) {
			deletedTasks.remove(deletedTasks.size() - 1);
		}
		
		int index = 0;
		switch (updated.getType()) {
		case TIMED:
			index = tasks.indexOf((TimedTask) updated);
			break;
		case DEADLINE:
			index = tasks.indexOf((DeadlineTask) updated);
			break;
		case FLOATING:
			index = tasks.indexOf((FloatingTask) updated);
			break;
		}
		
		tasks.remove(index);
		switch (beforeUpdate.getType()) {
		case TIMED:
			tasks.add(index, (TimedTask) beforeUpdate);
			return true;
		case DEADLINE:
			tasks.add(index, (DeadlineTask) beforeUpdate);
			return true;
		case FLOATING:
			tasks.add(index, (FloatingTask) beforeUpdate);
			return true;
		}
		return false;
	}

	/**
	 * This operation undoes the Clear command
	 * @return Boolean
	 * @author A0109194A
	 */
	private boolean undoClear() {
		ArrayList<Task> toRestore = clearedTasks.pop();
		tasks.addAll(toRestore);
		return true;
	}
	
	public void saveToOperationHistory(Global.CommandType type) {
		operationHistory.push(type);
	}
	
	public void saveToUndoHistory(Global.CommandType type) {
		undoHistory.push(type);
	}
	
	/* ------------------------------------- Clear (Internal) -------------------------------------- */
	/**
	 * This operation clears all tasks from memory.
	 * 
	 * @param userCommand 
	 * @return             Feedback for user.
	 * @author 	A0128620M, A0109194A
	 */
	public String clearTasks() {
		ArrayList<Task> cleared = new ArrayList<Task>();
		cleared.addAll(tasks);
		deletedTasks.addAll(tasks);
		clearedTasks.push(cleared);
		tasks.clear();
		saveToOperationHistory(Global.CommandType.CLEAR);
		saveToPermanentStorage();
		return String.format(Global.MESSAGE_CLEARED);
	}
	
	/* ---------------------------------------- Get Methods ----------------------------------------- */	
	
	/**
	 * This operation returns a sorted list consisting of copies of all tasks of the tasks list.
	 *  
	 * @return 	ArrayList<Task>
	 * @author 	A0128620M
	 */
	public ArrayList<Task> getCopiedTasks() {
		ArrayList<FloatingTask> floatingTasks = new ArrayList<FloatingTask>();
		ArrayList<DatedTask> datedTasks = new ArrayList<DatedTask>();
		ArrayList<Task> allTasks = new ArrayList<Task>();
		
		for(Task task: tasks) {
			if(task.getType().equals(Task.TaskType.FLOATING)) {
				floatingTasks.add(new FloatingTask((FloatingTask) task));
			} else if (task.getType().equals(Task.TaskType.DEADLINE)) {
				datedTasks.add(new DeadlineTask((DeadlineTask) task));
			} else if (task.getType().equals(Task.TaskType.TIMED)) {
				datedTasks.add(new TimedTask((TimedTask) task));
			}
		}
		
		Collections.sort(floatingTasks);
		allTasks.addAll(floatingTasks);
		Collections.sort(datedTasks);
		allTasks.addAll(datedTasks);
		
		return allTasks;
	}
	
	/**
	 * This operation returns a sorted list consisting of copies of those tasks of the tasks list 
	 * which satisfy the given DateTime, TaskType, Status and Search restrictions.
	 * 
	 * @param isDateRestricted
	 * @param startDate
	 * @param endDate
	 * @param isTaskTypeRestricted
	 * @param areFloatingTasksDisplayed
	 * @param areDeadlineTasksDisplayed
	 * @param areTimedTasksDisplayed
	 * @param isStatusRestricted
	 * @param areDoneTasksDisplayed
	 * @param areOpenTasksDisplayed
	 * @param isSearchRestricted
	 * @param searchedWordsAndPhrases
	 * @return 	ArrayList<Task>
	 * @author 	A0128620M
	 */
	public ArrayList<Task> getCopiedTasks(boolean isDateRestricted, Date startDate, Date endDate, boolean isTaskTypeRestricted, boolean areFloatingTasksDisplayed, boolean areDeadlineTasksDisplayed, boolean areTimedTasksDisplayed, boolean isStatusRestricted, boolean areDoneTasksDisplayed, boolean areOpenTasksDisplayed, boolean isSearchedWordRestricted, ArrayList<String> searchedWordsAndPhrases) {
		ArrayList<FloatingTask> floatingTasks = new ArrayList<FloatingTask>();
		ArrayList<DatedTask> datedTasks = new ArrayList<DatedTask>();
		ArrayList<Task> concernedTasks = new ArrayList<Task>();
		boolean containsSearchedWords = false;
		
		for(Task task: tasks) {

			// Step 1: Check SearchedWords
			if (isSearchedWordRestricted) {
				containsSearchedWords = true;
				for(String searchedWord : searchedWordsAndPhrases) {
					if (!task.getName().contains(searchedWord)) {
						logger.log(Level.INFO, "Doesn't contain the word");
						containsSearchedWords = false;
						break;
					}
				} 
			}
			
			if (!isSearchedWordRestricted || containsSearchedWords) {	

				// Step 2: Check Status
				if (!isStatusRestricted || (isStatusRestricted && areDoneTasksDisplayed == task.isDone() )) {
					// Step 3: Check Type
					if(task.getType() == Task.TaskType.FLOATING && (!isTaskTypeRestricted || (isTaskTypeRestricted && areFloatingTasksDisplayed))) {	
						// Step 4: Check DatePeriod
						if (!isDateRestricted) {
							floatingTasks.add(new FloatingTask((FloatingTask) task));
						}
					} else if (task.getType() == Task.TaskType.DEADLINE && (!isTaskTypeRestricted || (isTaskTypeRestricted && areDeadlineTasksDisplayed))) {
						DeadlineTask deadlineTask = (DeadlineTask) task;
						if (!isDateRestricted || (isDateRestricted && (startDate == null || deadlineTask.getEndDate().compareTo(startDate) >= 0) && (deadlineTask.getEndDate().compareTo(endDate) <= 0) )) { //TODO: Refactor Date Comparison methods
							datedTasks.add(new DeadlineTask((DeadlineTask) task));
						}
					} else if (task.getType() == Task.TaskType.TIMED && (!isTaskTypeRestricted || (isTaskTypeRestricted && areTimedTasksDisplayed))) {
						TimedTask timedTask = (TimedTask) task;
						if (!isDateRestricted || (isDateRestricted && (startDate == null || timedTask.getStartDate().compareTo(startDate) >= 0) && (timedTask.getEndDate().compareTo(endDate) <= 0) )) {
							datedTasks.add(new TimedTask((TimedTask) task));
						}
					}				
				}	
			}
		}
		
		Collections.sort(floatingTasks);
		concernedTasks.addAll(floatingTasks);
		Collections.sort(datedTasks);
		concernedTasks.addAll(datedTasks);
		
		return concernedTasks;
	}
	
	/**
	 * This operation returns the index of the given task object within the tasks ArrayList.
	 * @return  index
	 * @author 	A0128620M
	 */
	public int getIndexOf(Task task) {
		return tasks.indexOf(task);
	}
	
	/**
	 * This operation checks if the tasks ArrayList contains the given task.
	 * @return  index
	 * @author 	A0128620M
	 */
	public boolean contains(Task task) {
		return tasks.contains(task);
	}

	public ArrayList<Task> getDeletedTasks() {
		return deletedTasks;
	}
	
	public ArrayList<String> getAllIds() {
		ArrayList<String> idList = new ArrayList<String>();
		for (Task t : tasks) {
			idList.add(t.getId());
		}
		return idList;
	}
	
	/**
	 * This operation returns all
	 * @return  ArrayList<Task>
	 * @author 	A0128620M
	 */
	public ArrayList<Task> getAllTasks() {		//TODO: @Sean still encessary?
		return tasks;
	}
	
	/**
	 * This operation saves the temporary tasks ArrayList to the permanent storage.
	 * @author A0112828H
	 */
	public void saveToPermanentStorage() {
		TaskCommander.storage.writeToFile(tasks);
	}
	
	/**
	 * This operation loads the content from the permanent storage to the tasks ArrayList.
	 * @author A0112828H
	 */
	public void loadFromPermanentStorage() {
		tasks = TaskCommander.storage.readFromFile(); 
	}	
}

	
