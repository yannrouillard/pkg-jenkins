/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.util;

import hudson.model.Run;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Bug;
import static org.mockito.Mockito.when;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author Ignacio Albors
 */
@RunWith(PowerMockRunner.class)
public class RunListTest extends TestCase {

	// RunList for byTimestamp tests
	private RunList rlist;

	// RunList<Run> is ordered from most to least recent
	private void setUpByTimestampRuns() {
		Run r1 = PowerMockito.mock(Run.class);
		Run r2 = PowerMockito.mock(Run.class);

		when(r1.getNumber()).thenReturn(1);
		when(r2.getNumber()).thenReturn(2);

		when(r1.getTimeInMillis()).thenReturn(new Long(200));      	
		when(r2.getTimeInMillis()).thenReturn(new Long(300));

		ArrayList<Run> list = new ArrayList<Run>();
		list.add(r2);
		list.add(r1);

		rlist = RunList.fromRuns(list);

	}

	@PrepareForTest({Run.class})
	public void testbyTimestampAllRuns() {

		setUpByTimestampRuns();

		RunList<Run> tested = rlist.byTimestamp(0, 400);
		assertEquals(2, tested.toArray().length);

	}

    @Bug(21159)
	@PrepareForTest({Run.class})
	public void testbyTimestampFirstRun() {
		setUpByTimestampRuns();
		// Only r1
		RunList<Run> tested = rlist.byTimestamp(150, 250);
		assertEquals(1, tested.toArray().length);
		assertEquals(1, tested.getFirstBuild().getNumber());

	}

	@PrepareForTest({Run.class})
	public void testbyTimestampLastRun() {

		setUpByTimestampRuns();
		// Only r2
		RunList<Run> tested = rlist.byTimestamp(250, 350);
		assertEquals(1, tested.toArray().length);
		assertEquals(2, tested.getFirstBuild().getNumber());
	}

}
