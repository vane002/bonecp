package com.jolbox.bonecp;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Wrapper around JDBC Statement.
 *
 * @author wallacew
 * @version $Revision$
 */
public class StatementHandle implements Statement{
	/** Set to true if the connection has been "closed". */
	private volatile boolean logicallyClosed = false;
	/** A handle to the actual statement. */
	protected Statement internalStatement;
	/** List of resultSet that will be closed when this preparedStatement is logically closed. */ 
	private ConcurrentLinkedQueue<ResultSet> resultSetHandles = new ConcurrentLinkedQueue<ResultSet>();
	/** SQL Statement used for this statement. */
	protected String sql;
	/** Cache pertaining to this statement. */
	protected IStatementCache cache;
	/** Handle to the connection holding this statement. */
	protected ConnectionHandle connectionHandle;
	/** The key to use in the cache. */
	private String cacheKey ;


	/**
	 * Constructor to statement handle wrapper. 
	 *
	 * @param internalStatement handle to actual statement instance.
	 * @param sql statement used for this handle.
	 * @param cache Cache handle 
	 * @param connectionHandle Handle to the connection
	 * @param cacheKey  
	 */
	public StatementHandle(Statement internalStatement, String sql, IStatementCache cache, ConnectionHandle connectionHandle, String cacheKey) {
		this.sql = sql;
		this.internalStatement = internalStatement;
		this.cache = cache;


		this.cacheKey = cacheKey;
		this.connectionHandle = connectionHandle;
	}


	/**
	 * Constructor for empty statement (created via connection.createStatement) 
	 *
	 * @param internalStatement wrapper to statement
	 * @param connectionHandle Handle to the connection that this statement is tied to.
	 */
	public StatementHandle(Statement internalStatement, ConnectionHandle connectionHandle) {
		this.internalStatement = internalStatement;
		this.connectionHandle = connectionHandle;
		this.sql = null;
		this.cache = null;
	}


	@Override
	public void close() throws SQLException {
		if (!this.logicallyClosed){
			this.logicallyClosed = true;

			clearResultSetHandles(false);
			if (this.cache != null && this.cacheKey != null){
				this.cache.put(this.cacheKey, this);
			}

		}
	}


	/** Clears out all result set handles by closing them and removing them from tracking.
	 * @param internalClose
	 * @throws SQLException on error
	 */
	protected void clearResultSetHandles(boolean internalClose) throws SQLException {
		if (!internalClose){
			this.resultSetHandles.clear();
		} else {
			ResultSet rs = null;
			while ((rs=this.resultSetHandles.poll()) != null) {
				rs.close();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#addBatch(java.lang.String)
	 */
	@Override
	public void addBatch(String sql)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.addBatch(sql);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * Checks if the connection is marked as being logically open and throws an exception if not.
	 * @throws SQLException if connection is marked as logically closed.
	 * 
	 *
	 */
	protected void checkClosed() throws SQLException {
		if (this.logicallyClosed) {
			throw new SQLException("Statement is closed");
		}
	}



	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#cancel()
	 */
	@Override
	public void cancel()
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.cancel();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#clearBatch()
	 */
	@Override
	public void clearBatch()
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.clearBatch();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#clearWarnings()
	 */
	@Override
	public void clearWarnings()
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.clearWarnings();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#execute(java.lang.String)
	 */
	@Override
	public boolean execute(String sql)
	throws SQLException {
		boolean result = false;
		checkClosed();
		try {
			result = this.internalStatement.execute(sql);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#execute(java.lang.String, int)
	 */
	@Override
	public boolean execute(String sql, int autoGeneratedKeys)
	throws SQLException {
		boolean result = false;
		checkClosed();
		try{
			result = this.internalStatement.execute(sql, autoGeneratedKeys);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result;

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#execute(java.lang.String, int[])
	 */
	@Override
	public boolean execute(String sql, int[] columnIndexes)
	throws SQLException {
		boolean result = false;
		checkClosed();
		try{
			result = this.internalStatement.execute(sql, columnIndexes);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#execute(java.lang.String, java.lang.String[])
	 */
	@Override
	public boolean execute(String sql, String[] columnNames)
	throws SQLException {
		boolean result = false;
		checkClosed();
		try{
			result = this.internalStatement.execute(sql, columnNames);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result;

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#executeBatch()
	 */
	@Override
	public int[] executeBatch()
	throws SQLException {
		int[] result = null;
		checkClosed();
		try{
			result = this.internalStatement.executeBatch();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; // never reached

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#executeQuery(java.lang.String)
	 */
	@Override
	public ResultSet executeQuery(String sql)
	throws SQLException {
		ResultSet result = null;
		checkClosed();
		try{
			result = trackResultSet(this.internalStatement.executeQuery(sql));
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result;

	}

	/**
	 * Adds the given resultset to a list.
	 *
	 * @param rs ResultSet to keep track of
	 * @return rs
	 */
	private ResultSet trackResultSet(ResultSet rs) {
		if (rs != null){
			this.resultSetHandles.add(rs);
		}
		return rs;
	}



	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#executeUpdate(java.lang.String)
	 */
	@Override
	public int executeUpdate(String sql)
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.executeUpdate(sql);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#executeUpdate(java.lang.String, int)
	 */
	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys)
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.executeUpdate(sql, autoGeneratedKeys);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#executeUpdate(java.lang.String, int[])
	 */
	@Override
	public int executeUpdate(String sql, int[] columnIndexes)
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.executeUpdate(sql, columnIndexes);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#executeUpdate(java.lang.String, java.lang.String[])
	 */
	@Override
	public int executeUpdate(String sql, String[] columnNames)
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.executeUpdate(sql, columnNames);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getConnection()
	 */
	@Override
	public Connection getConnection()
	throws SQLException {
		checkClosed();
		return this.connectionHandle;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getFetchDirection()
	 */
	@Override
	public int getFetchDirection()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getFetchDirection();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getFetchSize()
	 */
	@Override
	public int getFetchSize()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getFetchSize();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getGeneratedKeys()
	 */
	@Override
	public ResultSet getGeneratedKeys()
	throws SQLException {
		ResultSet result = null;
		checkClosed();
		try{
			result = trackResultSet(this.internalStatement.getGeneratedKeys());
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getMaxFieldSize()
	 */
	@Override
	public int getMaxFieldSize()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getMaxFieldSize();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getMaxRows()
	 */
	@Override
	public int getMaxRows()
	throws SQLException {
		int result=0;
		checkClosed();
		try{
			result = this.internalStatement.getMaxRows();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}


	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getMoreResults()
	 */
	@Override
	public boolean getMoreResults()
	throws SQLException {
		boolean result = false;
		checkClosed();
		try{
			result = this.internalStatement.getMoreResults();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getMoreResults(int)
	 */
	@Override
	public boolean getMoreResults(int current)
	throws SQLException {
		boolean result = false;
		checkClosed();

		try{ 
			result = this.internalStatement.getMoreResults(current);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getQueryTimeout()
	 */
	@Override
	public int getQueryTimeout()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getQueryTimeout();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getResultSet()
	 */
	@Override
	public ResultSet getResultSet()
	throws SQLException {
		ResultSet result = null;
		checkClosed();
		try{
			result = trackResultSet(this.internalStatement.getResultSet());
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getResultSetConcurrency()
	 */
	@Override
	public int getResultSetConcurrency()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getResultSetConcurrency();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getResultSetHoldability()
	 */
	@Override
	public int getResultSetHoldability()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getResultSetHoldability();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getResultSetType()
	 */
	@Override
	public int getResultSetType()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getResultSetType();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getUpdateCount()
	 */
	@Override
	public int getUpdateCount()
	throws SQLException {
		int result = 0;
		checkClosed();
		try{
			result = this.internalStatement.getUpdateCount();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#getWarnings()
	 */
	@Override
	public SQLWarning getWarnings()
	throws SQLException {
		SQLWarning result = null;
		checkClosed();
		try{
			result = this.internalStatement.getWarnings();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#isClosed()
	 */
	@Override
	public boolean isClosed()
	throws SQLException {

		return this.logicallyClosed;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#isPoolable()
	 */
	@Override
	public boolean isPoolable()
	throws SQLException {
		boolean result = false;
		checkClosed();
		try{
			result = this.internalStatement.isPoolable();
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result; 

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setCursorName(java.lang.String)
	 */
	@Override
	public void setCursorName(String name)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setCursorName(name);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setEscapeProcessing(boolean)
	 */
	@Override
	public void setEscapeProcessing(boolean enable)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setEscapeProcessing(enable);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setFetchDirection(int)
	 */
	@Override
	public void setFetchDirection(int direction)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setFetchDirection(direction);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setFetchSize(int)
	 */
	@Override
	public void setFetchSize(int rows)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setFetchSize(rows);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setMaxFieldSize(int)
	 */
	@Override
	public void setMaxFieldSize(int max)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setMaxFieldSize(max);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setMaxRows(int)
	 */
	@Override
	public void setMaxRows(int max)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setMaxRows(max);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setPoolable(boolean)
	 */
	@Override
	public void setPoolable(boolean poolable)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setPoolable(poolable);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Statement#setQueryTimeout(int)
	 */
	@Override
	public void setQueryTimeout(int seconds)
	throws SQLException {
		checkClosed();
		try{
			this.internalStatement.setQueryTimeout(seconds);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}

	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
	 */
	@Override
	public boolean isWrapperFor(Class<?> iface)
	throws SQLException {
		boolean result = false;
		try{
			result = this.internalStatement.isWrapperFor(iface);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.sql.Wrapper#unwrap(java.lang.Class)
	 */
	@Override
	public <T> T unwrap(Class<T> iface)
	throws SQLException {
		T result = null;
		try{
			result = this.internalStatement.unwrap(iface);
		} catch (Throwable t) {
			throw this.connectionHandle.markPossiblyBroken(t);

		}
		return result;

	}



	/**
	 * @throws SQLException 
	 * 
	 *
	 */
	public void internalClose() throws SQLException {
		clearResultSetHandles(true);

		this.internalStatement.close();
		if (this.cache != null){
			this.cache.clear();
		}
	}


	/**
	 * Marks this statement as being "open"
	 *
	 */
	public void setLogicallyOpen() {
		this.logicallyClosed = false;
	}

}
