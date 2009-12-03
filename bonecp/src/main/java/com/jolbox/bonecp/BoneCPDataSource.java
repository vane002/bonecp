package com.jolbox.bonecp;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.sql.DataSource;

import org.apache.log4j.Logger;


/**
 * DataSource for use with LazyConnection Provider etc.
 *
 * @author wallacew
 * @version $Revision$
 */
public class BoneCPDataSource implements DataSource {
    /** Config stuff. */
    private static final String CONFIG_STATUS = "Connection pool: URL = %s, username=%s, Min = %d, Max = %d, Acquire Increment = %d, Partitions = %d, idleConnection=%d, Max Age=%d";
    /** Config setting. */
    private PrintWriter logWriter = null;
    /** Config setting. */
    private String maxConnectionsPerPartition="50";
    /** Config setting. */
    private String minConnectionsPerPartition="10";
    /** Config setting. */
    private String preparedStatementCacheSize="50";
    /** Config setting. */
    private String statementsCachedPerConnection="30";
    /** Config setting. */
    private String acquireIncrement="2";
    /** Config setting. */
    private String partitions="2";
    /** Config setting. */
    private String idleConnectionTestPeriod="60";
    /** Config setting. */
    private String idleMaxAge="240";
    /** Config setting. */
    private String connectionTestStatement;
    /** Config setting. */
    private String jdbcUrl="JDBC URL NOT SET!";
    /** Config setting. */
    private String driverClass="DRIVER CLASS NOT SET!";
    /** Config setting. */
    private String username="USERNAME NOT SET!";
    /** Config setting. */
    private String password="PASSWORD NOT SET!";
    /** Pool handle. */
    private volatile BoneCP pool = null;
    /** Lock for init. */
    private ReadWriteLock rwl = new ReentrantReadWriteLock();
    /** Config setting. */
    private String releaseHelperThreads = "3";
    /** Class logger. */
    private static final Logger logger = Logger.getLogger(BoneCPDataSource.class);


    /**
     * Default empty constructor.
     *
     */
    public BoneCPDataSource() {
        // default constructor
    }
    /**
     * 
     *
     * @param config
     */
    public BoneCPDataSource(BoneCPConfig config) {
        this.setJdbcUrl(config.getJdbcUrl());
        this.setUsername(config.getUsername());
        this.setPassword(config.getPassword());
        this.setIdleConnectionTestPeriod(config.getIdleConnectionTestPeriod());
        this.setIdleMaxAge(config.getIdleMaxAge());
        this.setPartitionCount(config.getPartitionCount());
        this.setMaxConnectionsPerPartition(config.getMaxConnectionsPerPartition());
        this.setMinConnectionsPerPartition(config.getMinConnectionsPerPartition());
        this.setAcquireIncrement(config.getAcquireIncrement());
        this.setConnectionTestStatement(config.getConnectionTestStatement());
        this.setPreparedStatementCacheSize(config.getPreparedStatementsCacheSize());
        this.setStatementsCachedPerConnection(config.getStatementsCachedPerConnection());
        this.setReleaseHelperThreads(config.getReleaseHelperThreads());
    }

   
	/**
     * {@inheritDoc}
     *
     * @see javax.sql.DataSource#getConnection()
     */
    public Connection getConnection()
    throws SQLException {
        if (this.pool == null){
            maybeInit();
        }
        return this.pool.getConnection();
    }

    /**
     * Close the datasource. 
     *
     */
    public void close(){
        this.pool.shutdown();
    }

    /**
     * @throws SQLException 
     * 
     *
     */
    private void maybeInit() throws SQLException {
        this.rwl.readLock().lock();
        if (this.pool == null){
            this.rwl.readLock().unlock();
            this.rwl.writeLock().lock();
            if (this.pool == null){ //read might have passed, write might not
                int minsize = parseNumber(this.minConnectionsPerPartition, 10);
                int maxsize = parseNumber(this.maxConnectionsPerPartition, 50);
                int releaseHelperThreads = parseNumber(this.releaseHelperThreads, 3);
                int acquireIncrement = parseNumber(this.acquireIncrement, 10);
                int partitions = parseNumber(this.partitions, 3);
                
                long idleConnectionTestPeriod = parseNumber(this.idleConnectionTestPeriod, 60);
                long idleMaxAge = parseNumber(this.idleMaxAge, 240); 
                int psCacheSize = parseNumber(this.preparedStatementCacheSize, 100);
                int statementsCachedPerConnection = parseNumber(this.statementsCachedPerConnection, 30);
                try {
                    Class.forName(this.driverClass);
                }
                catch (ClassNotFoundException e) {
                    throw new SQLException(e);
                }


                logger.debug(String.format(CONFIG_STATUS, this.jdbcUrl, this.username, minsize, maxsize, acquireIncrement, partitions, idleConnectionTestPeriod/1000, idleMaxAge/(60*1000)));

                BoneCPConfig config = new BoneCPConfig();
                config.setMinConnectionsPerPartition(minsize);
                config.setMaxConnectionsPerPartition(maxsize);
                config.setAcquireIncrement(acquireIncrement);
                config.setPartitionCount(partitions);
                config.setJdbcUrl(this.jdbcUrl);
                config.setUsername(this.username);
                config.setPassword(this.password);
                config.setIdleConnectionTestPeriod(idleConnectionTestPeriod);
                config.setIdleMaxAge(idleMaxAge);
                config.setConnectionTestStatement(this.connectionTestStatement);
                config.setPreparedStatementsCacheSize(psCacheSize);
                config.setStatementsCachedPerConnection(statementsCachedPerConnection);
                config.setReleaseHelperThreads(releaseHelperThreads);
                config.sanitize();

                this.pool = new BoneCP(config);
            }

            this.rwl.writeLock().unlock(); // Unlock write
        } else {
            this.rwl.readLock().unlock(); // Unlock read
        }
    }


    /** Calls Integer.parseInt but defaults to a given number on parse error.
     * @param number value to convert
	 * @param defaultValue value to return on no value being set (or error)
	 * @return the converted number (or default) 
	 */
	private int parseNumber(String number, int defaultValue) {
		int result = defaultValue;
		if (number != null) {
			try{
				result = Integer.parseInt(number);
			} catch (NumberFormatException e){
				// do nothing, use the default value
			}
		}
		return result;
	}

    /**
     * {@inheritDoc}
     *
     * @see javax.sql.DataSource#getConnection(java.lang.String, java.lang.String)
     */
    public Connection getConnection(String username, String password)
    throws SQLException {
        throw new UnsupportedOperationException("getConnectionString username, String password) is not supported");
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.sql.CommonDataSource#getLogWriter()
     */
    public PrintWriter getLogWriter()
    throws SQLException {
        return this.logWriter;
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.sql.CommonDataSource#getLoginTimeout()
     */
    public int getLoginTimeout()
    throws SQLException {
        throw new UnsupportedOperationException("getLoginTimeout is unsupported.");
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.sql.CommonDataSource#setLogWriter(java.io.PrintWriter)
     */
    public void setLogWriter(PrintWriter out)
    throws SQLException {
        this.logWriter = out;
    }

    /**
     * {@inheritDoc}
     *
     * @see javax.sql.CommonDataSource#setLoginTimeout(int)
     */
    public void setLoginTimeout(int seconds)
    throws SQLException {
        throw new UnsupportedOperationException("setLoginTimeout is unsupported.");
    }

    /**
     * {@inheritDoc}
     *
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    public boolean isWrapperFor(Class<?> arg0)
    throws SQLException {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public Object unwrap(Class arg0)
    throws SQLException {
        return null;
    }




    /**
     * Gets max connections.
     *
     * @return max connections
     */
    public String getMaxConnectionsPerPartition() {
        return this.maxConnectionsPerPartition;
    }




    /**
     * Sets max connections. Called via reflection. 
     *
     * @param maxConnectionsPerPartition to set 
     */
    public void setMaxConnectionsPerPartition(String maxConnectionsPerPartition) {
        this.maxConnectionsPerPartition = maxConnectionsPerPartition;
    }

    /**
     * Sets max connections. Called via reflection. 
     *
     * @param maxConnectionsPerPartition to set 
     */
    public void setMaxConnectionsPerPartition(Integer maxConnectionsPerPartition) {
        this.maxConnectionsPerPartition = maxConnectionsPerPartition.toString();
    }



    /**
     * Gets minConnectionsPerPartition config setting.
     *
     * @return minConnectionsPerPartition
     */
    public String getMinConnectionsPerPartition() {
        return this.minConnectionsPerPartition;
    }




    /**
     * Sets minConnectionsPerPartition setting. Called via reflection.
     *
     * @param minConnectionsPerPartition 
     */
    public void setMinConnectionsPerPartition(String minConnectionsPerPartition) {
        this.minConnectionsPerPartition = minConnectionsPerPartition;
    }

    /**
     * Sets minConnectionsPerPartition setting. Called via reflection.
     *
     * @param minConnectionsPerPartition 
     */
    public void setMinConnectionsPerPartition(Integer minConnectionsPerPartition) {
        this.minConnectionsPerPartition = minConnectionsPerPartition.toString();
    }



    /**
     * Gets acquireIncrement setting. 
     *
     * @return acquireIncrement set in config
     */
    public String getAcquireIncrement() {
        return this.acquireIncrement;
    }




    /**
     * Sets acquireIncrement setting. Called via reflection.
     *
     * @param acquireIncrement acquire increment setting
     */
    public void setAcquireIncrement(String acquireIncrement) {
        this.acquireIncrement = acquireIncrement;
    }


    /**
     * Sets acquireIncrement setting. Called via reflection.
     *
     * @param acquireIncrement acquire increment setting
     */
    public void setAcquireIncrement(Integer acquireIncrement) {
        this.acquireIncrement = acquireIncrement.toString();
    }

    

    /**
     * Gets the number of partitions. 
     *
     * @return partitions set in config.
     */
    public String getPartitions() {
        return this.partitions;
    }




    /**
     * Sets the number of thread partitions set in config. Called via reflection.
     *
     * @param partitions to set
     */
    public void setPartitionCount(String partitions) {
        this.partitions = partitions;
    }


    /**
     * Sets the number of thread partitions set in config. Called via reflection.
     *
     * @param partitionCount to set
     */
    public void setPartitionCount(Integer partitionCount) {
        this.partitions = partitionCount.toString();
    }


    /**
     * Gets idleConnectionTestPeriod config setting. 
     *
     * @return idleConnectionTestPeriod set in config
     */
    public String getIdleConnectionTestPeriod() {
        return this.idleConnectionTestPeriod;
    }




    /**
     * Sets idle connection test period. Called via reflection.
     *
     * @param idleConnectionTestPeriod to set
     */
    public void setIdleConnectionTestPeriod(String idleConnectionTestPeriod) {
        this.idleConnectionTestPeriod = idleConnectionTestPeriod;
    }


    /**
     * Sets idle connection test period. Called via reflection.
     *
     * @param idleConnectionTestPeriod to set
     */
    public void setIdleConnectionTestPeriod(Long idleConnectionTestPeriod) {
        this.idleConnectionTestPeriod = idleConnectionTestPeriod.toString();
    }



    /**
     * Gets idle max age. 
     * 
     * @return idle max age 
     */
    public String getIdleMaxAge() {
        return this.idleMaxAge;
    }




    /**
     * Sets the idle maximum age. Called via reflection.
     *
     * @param idleMaxAge to set
     */
    public void setIdleMaxAge(String idleMaxAge) {
        this.idleMaxAge = idleMaxAge;
    }


    /**
     * Sets the idle maximum age. 
     *
     * @param idleMaxAge to set
     */
    public void setIdleMaxAge(Long idleMaxAge) {
        this.idleMaxAge = idleMaxAge.toString();
    }


    /**
     * Gets the JDBC connection url. 
     *
     * @return JDBC URL 
     */
    public String getJdbcUrl() {
        return this.jdbcUrl;
    }




    /**
     * Sets the JDBC connection url (called via reflection).
     *
     * @param jdbcUrl JDBC url connection string. 
     */
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }




    /**
     * Gets driver class set in config. 
     *
     * @return Driver class set in config
     */
    public String getDriverClass() {
        return this.driverClass;
    }




    /**
     * Sets driver to use (called via reflection).
     *
     * @param driverClass Driver to use
     */
    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }




    /**
     * Gets username set in the config.
     *
     * @return Username set in config
     */
    public String getUsername() {
        return this.username;
    }




    /**
     * Sets username (via reflection)
     *
     * @param username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }




    /**
     * Gets password set in config.
     *
     * @return Set password
     */
    public String getPassword() {
        return this.password;
    }




    /**
     * Sets password. c
     *
     * @param password to set 
     */
    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * Gets Connection test statement.
     *
     * @return connection test statement
     */
    public String getConnectionTestStatement() {
        return this.connectionTestStatement;
    }


    /**
     * Sets connection test statement. Called via reflection.
     *
     * @param connectionTestStatement to use.
     */
    public void setConnectionTestStatement(String connectionTestStatement) {
        this.connectionTestStatement = connectionTestStatement;
    }


    /**
     * Gets preparedStatementCacheSize to set.
     *
     * @return preparedStatementCacheSize
     */
    public String getPreparedStatementCacheSize() {
        return this.preparedStatementCacheSize;
    }


    /**
     * Sets cache size for prepared statements. Called via reflection 
     *
     * @param preparedStatementCacheSize to set
     */
    public void setPreparedStatementCacheSize(String preparedStatementCacheSize) {
        this.preparedStatementCacheSize = preparedStatementCacheSize;
    }

    /**
     * Sets cache size for prepared statements. Called via reflection 
     *
     * @param preparedStatementCacheSize to set
     */
    public void setPreparedStatementCacheSize(Integer preparedStatementCacheSize) {
        this.preparedStatementCacheSize = preparedStatementCacheSize.toString();
    }

    /**
     * Returns the total leased connections.
     *
     * @return total leased connections
     */
    public int getTotalLeased() {
        return this.pool.getTotalLeased();
    }
    
    /**
     * Gets no of release helper threads.
     *
     * @return release helper threads 
     */
    public String getReleaseHelperThreads() {
        return this.releaseHelperThreads;
    }
    
    /**
     * Sets no of release Helper threads to use. Called via reflection 
     *
     * @param releaseHelperThreads to set 
     */
    public void setReleaseHelperThreads(String releaseHelperThreads) {
        this.releaseHelperThreads = releaseHelperThreads;
    }
    
    /** Sets no of release Helper threads to use.  
    *
    * @param releaseHelperThreads to set 
    */
   public void setReleaseHelperThreads(Integer releaseHelperThreads) {
       this.releaseHelperThreads = releaseHelperThreads.toString();
   }
   
	/**
	 * Gets statementsCachedPerConnection.
	 *
	 * @return statementsCachedPerConnection
	 */
	public String getStatementsCachedPerConnection() {
		return this.statementsCachedPerConnection;
	}
	/**
	 * Sets statementsCachedPerConnection. Called via reflection.
	 *
	 * @param statementsCachedPerConnection to set
	 */
	public void setStatementsCachedPerConnection(
			String statementsCachedPerConnection) {
		this.statementsCachedPerConnection = statementsCachedPerConnection;
	}
	/**
	 * Sets statementsCachedPerConnection. Called via reflection.
	 *
	 * @param statementsCachedPerConnection to set
	 */
	public void setStatementsCachedPerConnection(
			Integer statementsCachedPerConnection) {
		this.statementsCachedPerConnection = this.statementsCachedPerConnection.toString();
	}
}
