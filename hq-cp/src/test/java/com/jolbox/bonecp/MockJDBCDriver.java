/**
 *  Copyright 2010 Wallace Wadge
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * 
 */
package com.jolbox.bonecp;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/** A Fake jdbc driver for mocking purposes.
 * @author Wallace
 *
 */
public class MockJDBCDriver  implements Driver {

	private String acceptURL = MockConstant.MOCK_URL;

    /** Connection handle to return. */
	private volatile Connection connection = null;
	/** called to return. */
	private volatile MockJDBCAnswer mockJDBCAnswer;

	private static final MockJDBCDriver INSTANCE;

	static {
		try {
			INSTANCE = new MockJDBCDriver();
			DriverManager.registerDriver(INSTANCE);
		} catch (SQLException e) {
			throw new RuntimeException("register MockJDBCDriver failed", e);
		}
	}

	public static MockJDBCDriver getInstance() {
		return INSTANCE;
	}

	/**
	 * Default constructor
	 * @throws SQLException
	 */
	private MockJDBCDriver() throws SQLException {
		// default constructor
//		DriverManager.registerDriver(this);
//		System.err.println("MockJDBCDriver registered -1");
	}

	private MockJDBCDriver(String url) throws SQLException {
		acceptURL = url;
		DriverManager.registerDriver(this);
		System.err.println("MockJDBCDriver registered -2");
	}

	/** Stop intercepting requests.
	 * @throws SQLException
	 */
	public void unregister() throws SQLException {
		this.connection = null;
		this.mockJDBCAnswer = null;
		DriverManager.deregisterDriver(this);
	}

	/** Connection handle to return
	 * @param mockJDBCAnswer answer class
	 * @throws SQLException
	 */
	private MockJDBCDriver(MockJDBCAnswer mockJDBCAnswer) throws SQLException {
		this();
		this.mockJDBCAnswer = mockJDBCAnswer;
		System.err.println("MockJDBCDriver registered -3");
	}
	
	/** Return the connection when requested.
	 * @param connection
	 * @throws SQLException
	 */
	private MockJDBCDriver(Connection connection) throws SQLException {
		this();
		this.connection = connection;
		System.err.println("MockJDBCDriver registered");
	}
	/** {@inheritDoc}
	 * @see java.sql.Driver#acceptsURL(java.lang.String)
	 */
	@Override
	public synchronized boolean acceptsURL(String url) throws SQLException {
//        return true;
		return url.startsWith(acceptURL); // accept anything
	}

	/** {@inheritDoc}
	 * @see java.sql.Driver#connect(java.lang.String, java.util.Properties)
	 */
	 @Override
	public synchronized Connection connect(String url, Properties info) throws SQLException {
		if (url.startsWith("invalid") || url.equals("")){
			throw new SQLException("Mock Driver rejecting invalid URL");
		}
		if (this.connection != null){
			return this.connection;
		}

		if (this.mockJDBCAnswer == null){
			MockConnection conn =  new MockConnection();
			conn.connect();
			return conn;
		}

		return this.mockJDBCAnswer.answer();
	}

	/** {@inheritDoc}
	 * @see java.sql.Driver#getMajorVersion()
	 */
	 @Override
	public int getMajorVersion() {
		return 1;
	}

	/** {@inheritDoc}
	 * @see java.sql.Driver#getMinorVersion()
	 */
	 @Override
	public int getMinorVersion() {
		return 0;
	}

	/** {@inheritDoc}
	 * @see java.sql.Driver#getPropertyInfo(java.lang.String, java.util.Properties)
	 */
	 @Override
	public synchronized DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
			throws SQLException {
		return new DriverPropertyInfo[0];
	}

	/** {@inheritDoc}
	 * @see java.sql.Driver#jdbcCompliant()
	 */
	 @Override
	public boolean jdbcCompliant() {
		return true;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return null;
	}

	/** 
	 * Disable everything.
	 * @throws SQLException
	 * @throws SQLException
	 */
	public synchronized void disable() throws SQLException {
		this.connection = null;
		this.mockJDBCAnswer = null;
		this.acceptURL = MockConstant.MOCK_URL;
//		DriverManager.deregisterDriver(this);
//		System.err.println(">>>MockJDBCDriver deregisterDriver<<<");
	}

	/**
	 * @return the connection
	 */
	public synchronized Connection getConnection() {
		return this.connection;
	}

	/**
	 * @param connection the connection to set
	 */
	public synchronized  MockJDBCDriver setConnection(Connection connection) {
		this.connection = connection;
		return this;
	}

	/** Return the jdbc answer class
	 * @return the mockJDBCAnswer
	 */
	public synchronized  MockJDBCAnswer getMockJDBCAnswer() {
		return this.mockJDBCAnswer;
	}

	/** Sets the jdbc mock answer.
	 * @param mockJDBCAnswer the mockJDBCAnswer to set
	 */
	public synchronized MockJDBCDriver setMockJDBCAnswer(MockJDBCAnswer mockJDBCAnswer) {
		this.mockJDBCAnswer = mockJDBCAnswer;
		return this;
	}

	public MockJDBCDriver setAcceptUrl(String url) {
		this.acceptURL = url;
		return this;
	}

}