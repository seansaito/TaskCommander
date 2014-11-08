package automatedTestDriver.Data;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.taskcommander.TaskCommander;

@RunWith(Parameterized.class)
public class DoneTest {
	private int index;
	private String expected;
	
	public DoneTest(int index, String expected) {
		this.index = index;
		this.expected = expected;
	}
	
	@Parameterized.Parameters
	public static Collection<Object[]> cases() {
		TaskCommander.data.clearTasks();
		
		Calendar cal = Calendar.getInstance();
		cal.set(2014, Calendar.NOVEMBER, 10, 15, 00);
		Date start = cal.getTime();
		cal.set(2014,  Calendar.NOVEMBER, 10, 16, 00);
		Date end = cal.getTime();
		String title = "Watch a Movie";
		
		TaskCommander.data.addFloatingTask(title);
		TaskCommander.data.addDeadlineTask(title, start);
		TaskCommander.data.addTimedTask(title, start, end);
		
		return Arrays.asList(new Object[][] {
				{ 0, "Done: \"Watch a Movie\"" },
				{ 1, "Done: [by Mon Nov 10 '14 15:00] \"Watch a Movie\"" },
				{ 2, "Done: [Mon Nov 10 '14 15:00-16:00] \"Watch a Movie\""},
				{ 3, "Index does not exist. Please type a valid index."},
				{ -1, "Index does not exist. Please type a valid index."}
		});
		
	}
	
	@Test
	public void testDone() {
		assertEquals(expected, TaskCommander.data.done(index));
	}
}