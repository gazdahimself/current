/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.james.util.retry.naming;

import javax.naming.Context;
import javax.naming.NamingException;
import org.apache.james.util.retry.api.ExceptionRetryingProxy;
import org.apache.james.util.retry.api.RetryHandler;
import org.apache.james.util.retry.api.RetrySchedule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * <code>ExceptionRetryHandlerTest</code>
 */
public class NamingExceptionRetryHandlerTest {

    private Class<?>[] _exceptionClasses = null;
    private ExceptionRetryingProxy _proxy = null;
    private RetrySchedule _schedule = null;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
	_exceptionClasses = new Class<?>[]{NamingException.class};
	_proxy = new TestRetryingProxy();
	_schedule = new TestRetrySchedule();
    }

    private class TestRetryingProxy implements ExceptionRetryingProxy {

	/**
	 * @see org.apache.james.user.ldap.api.ExceptionRetryingProxy#getDelegate()
	 */
	@Override
	public Context getDelegate() throws NamingException {
	    return null;
	}

	/**
	 * @see org.apache.james.user.ldap.api.ExceptionRetryingProxy#newDelegate()
	 */
	@Override
	public Context newDelegate() throws NamingException {
	    return null;
	}

	/**
	 * @see org.apache.james.user.ldap.api.ExceptionRetryingProxy#resetDelegate()
	 */
	@Override
	public void resetDelegate() throws NamingException {
	}
    }

    private class TestRetrySchedule implements RetrySchedule {

	/**
	 * @see org.apache.james.user.ldap.api.RetrySchedule#getInterval(int)
	 */
	@Override
	public long getInterval(int index) {
	    return index;
	}
    }

    /**
     * Test method for {@link org.apache.james.user.ldap.ExceptionRetryHandler#ExceptionRetryHandler(java.lang.Class<?>[], org.apache.james.user.ldap.api.ExceptionRetryingProxy, org.apache.james.user.ldap.api.RetrySchedule, int)}.
     */
    @Test
    public final void testExceptionRetryHandler() {
	assertTrue(RetryHandler.class.isAssignableFrom(new NamingExceptionRetryHandler(
		_exceptionClasses, _proxy, _schedule, 0) {

	    @Override
	    public Object operation() throws Exception {
		return null;
	    }
	}.getClass()));
    }

    /**
     * Test method for {@link org.apache.james.user.ldap.ExceptionRetryHandler#perform()}.
     * @throws Exception 
     */
    @Test
    public final void testPerform() throws NamingException {
	Object result = new NamingExceptionRetryHandler(
		_exceptionClasses, _proxy, _schedule, 0) {

	    @Override
	    public Object operation() throws NamingException {
		return "Hi!";
	    }
	}.perform();
	assertEquals("Hi!", result);

	try {
	    new NamingExceptionRetryHandler(
		    _exceptionClasses, _proxy, _schedule, 0) {

		@Override
		public Object operation() throws Exception {
		    throw new NamingException();
		}
	    }.perform();
	} catch (NamingException ex) {
	    // no-op
	}
	assertEquals("Hi!", result);
    }
}
