package automatedTestDriver.Integrated;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.taskcommander.Global;
import com.taskcommander.TaskCommander;

/**
 * This class contains all test cases for the method display of Integrated Testing.
 * 
 * 
 */
//@author A0105753J
@RunWith(Parameterized.class)
public class IntegratedDisplayTest {

	private String userCommand;
	private String expectedResult;


	public IntegratedDisplayTest(String userCommand, String expectedResult) {
		this.userCommand = userCommand;
		this.expectedResult = expectedResult;
	}

	/*
	 * These care test cases for display method
	 * Format would be:display TimePeriod Status
	 * 
	 */
	@Parameterized.Parameters
	public static Collection<Object[]>  cases() {
		String deleteCommand[] = {"display","Display", "DISPLAY", "diSPLay"};
		String Tasktype[] = {"floating", "deadline", "timed"};
		String TimePeriod = "Oct 31 - Nov 18";
		String openStatus = "open";
		String doneStatus = "done";

		Calendar calendar = Calendar.getInstance();
		Date today = calendar.getTime();
		calendar.add(Calendar.DAY_OF_YEAR, 1);
		Date tomorrow = calendar.getTime();
		calendar.setTimeInMillis(today.getTime());
		calendar.add(Calendar.MINUTE, 20);
		Date later = calendar.getTime();


		return Arrays.asList(new Object[][] {
				{ deleteCommand[0], "Displayed: All"},
				{ deleteCommand[1], "Displayed: All"},
				{ deleteCommand[2], "Displayed: All"},
				{ deleteCommand[3], "Displayed: All"},
				//{ deleteCommand[0]+" " +Tasktype[0], "Displayed: Type: Floating "},
				{ deleteCommand[0]+" " +Tasktype[1], "Displayed: Type: Deadline "},
				{ deleteCommand[0]+" " +Tasktype[2], "Displayed: Type: Timed  "},
				//{ deleteCommand[0]+" " +openStatus, "Displayed: Status: Open "},
				{ deleteCommand[0]+" " +doneStatus, "Displayed: Status: Done "},
				{ deleteCommand[0]+" " +TimePeriod, "Displayed:  Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]"},
				
				/*
				{ deleteCommand[0]+" " +Tasktype[0]+" " + " "+openStatus, "Displayed: Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]  Type: deadline Status: open "},
				{ deleteCommand[0]+" " +Tasktype[1]+" "+ TimePeriod+ " "+openStatus, "Displayed: Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]  Type: deadline Status: open "},
				{ deleteCommand[0]+" " +Tasktype[2]+" "+ TimePeriod+ " "+openStatus, "Displayed: Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]  Type: deadline Status: open "},
				
				{ deleteCommand[0]+" " +Tasktype[0]+" " + " "+doneStatus, "Displayed: Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]  Type: deadline Status: open "},
				{ deleteCommand[0]+" " +Tasktype[1]+" "+ TimePeriod+ " "+doneStatus, "Displayed: Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]  Type: deadline Status: open "},
				{ deleteCommand[0]+" " +Tasktype[2]+" "+ TimePeriod+ " "+doneStatus, "Displayed: Date: [Fri Oct 31 '14 "+ Global.timeFormat.format(today)+"-"+" Tue Nov 18 '14 "+Global.timeFormat.format(today)+ "]  Type: deadline Status: open "},
				*
				*/


		
		});
	}

	@Test
	public void testcontainsParameter() {
		assertEquals(expectedResult, TaskCommander.controller.executeCommand(userCommand)); 
	}
}
