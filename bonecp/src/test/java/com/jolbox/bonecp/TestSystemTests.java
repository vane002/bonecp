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


package com.jolbox.bonecp;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;

import javax.naming.RefAddr;
import javax.naming.Reference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.cache.LoadingCache;
import com.jolbox.bonecp.hooks.CoverageHook;
import com.jolbox.bonecp.hooks.CustomHook;



@SuppressWarnings("all") 
public class TestSystemTests {

	private BoneCPConfig config;
	private MockJDBCDriver driver;

	@Before
	public void beforeTest() throws SQLException{
		driver = new MockJDBCDriver(new MockJDBCAnswer() {
			
			public Connection answer() throws SQLException {
				return new MockConnection();
			}
		});
		config = CommonTestUtils.getConfigClone();
		config.setJdbcUrl(CommonTestUtils.url);
		config.setUsername(CommonTestUtils.username);
		config.setPassword(CommonTestUtils.password);
		config.setIdleConnectionTestPeriod(10000);
		config.setIdleMaxAge(0);
		config.setStatementsCacheSize(0);
		config.setReleaseHelperThreads(0);
		config.setStatementsCachedPerConnection(30);
	}

	@After
	public void teardown() throws SQLException{
		driver.disable();
	}
		
	/** Mostly for code coverage. 
	 * @throws IOException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 * @throws ClassNotFoundException 
	 * @throws NoSuchFieldException 
	 * @throws ExecutionException */
	@Test
	public void testDataSource() throws SQLException, IOException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, ExecutionException {
		config.setAcquireIncrement(5);
		config.setMinConnectionsPerPartition(30);
		config.setMaxConnectionsPerPartition(100);
		config.setPartitionCount(1);
		
		
		BoneCPDataSource dsb = new BoneCPDataSource(config);
		dsb.setPartitionCount(1); 
		dsb.setAcquireRetryDelay(-1);
		dsb.setAcquireRetryAttempts(0);
		dsb.setMaxConnectionsPerPartition(100);
		dsb.setMinConnectionsPerPartition(30);
		dsb.setTransactionRecoveryEnabled(true);
		dsb.setConnectionHook(new CoverageHook());
		dsb.setLazyInit(false);
		dsb.setStatementsCachedPerConnection(30);
		dsb.setStatementsCacheSize(30);
		dsb.setReleaseHelperThreads(0);
		dsb.setDriverClass("com.jolbox.bonecp.MockJDBCDriver");
		dsb.isWrapperFor(String.class);
		dsb.setIdleMaxAge(0L);
		dsb.setAcquireIncrement(5);
		dsb.setIdleConnectionTestPeriod(0L);
		dsb.setConnectionTestStatement("test");
		dsb.setInitSQL(CommonTestUtils.TEST_QUERY);
		dsb.setCloseConnectionWatch(true);
		dsb.setLogStatementsEnabled(false);
		dsb.getConnection().close();
		assertNotNull(dsb.getConfig());
		assertNotNull(dsb.toString());
		dsb.setConnectionHookClassName("bad class name");
		assertEquals("bad class name", dsb.getConnectionHookClassName());
		assertNull(dsb.getConnectionHook());
		
		dsb.setConnectionHookClassName("com.jolbox.bonecp.hooks.CustomHook");
		assertTrue(dsb.getConnectionHook() instanceof CustomHook);
		
		File tmp = File.createTempFile("bonecp", "");
		dsb.setLogWriter(new PrintWriter(tmp));
		assertNotNull(dsb.getLogWriter());
		try {
			dsb.setLoginTimeout(0);
			fail("Should throw exception");
		} catch (UnsupportedOperationException e) {
			// do nothing
		}
		
		try {
			dsb.getLoginTimeout();
			fail("Should throw exception");
		} catch (UnsupportedOperationException e) {
			// do nothing
		}
	
		try {
			dsb.getParentLogger();
			fail("Should throw exception");
		} catch (UnsupportedOperationException e) {
			// do nothing
		}
		
			Connection c = dsb.getConnection("test", "test");
			assertNotNull(c);
		
		BoneCPDataSource dsb2 = new BoneCPDataSource(); // empty constructor test
		dsb2.setDriverClass("inexistent");
		try{
			dsb2.getConnection();
			fail("Should fail");
		} catch (SQLException e){
			// do nothing
		}
		
		
		LoadingCache<UsernamePassword, BoneCPDataSource> mockLoadingCache = createNiceMock(LoadingCache.class);
		expect(mockLoadingCache.get((UsernamePassword)anyObject())).andThrow(new ExecutionException("", new Exception())).once();
		Field f = BoneCPDataSource.class.getDeclaredField("multiDataSource");
		f.setAccessible(true);
		f.set(dsb, mockLoadingCache);

		replay(mockLoadingCache);
		try{
			dsb.getConnection("foo", "bar");
			fail("Should throw exception");
		} catch(SQLException e){
			// do nothing
		}
		
		assertNull(dsb.unwrap(String.class));
		assertEquals("com.jolbox.bonecp.MockJDBCDriver", dsb.getDriverClass());
		dsb.setClassLoader(getClass().getClassLoader());
		dsb.loadClass("java.lang.String");
		assertEquals(getClass().getClassLoader(), dsb.getClassLoader());
		
		BoneCPDataSource dsb3 = new BoneCPDataSource(); // empty constructor test
		assertTrue(dsb3.getPool() == null);
		assertEquals(0, dsb3.getTotalLeased());
	
	}
	
	@Test(expected=SQLException.class)
	public void testDBConnectionInvalidJDBCurl() throws SQLException{
		CommonTestUtils.logTestInfo("Test trying to start up with an invalid URL.");
		config.setJdbcUrl("invalid JDBC URL");
		new BoneCP(config);
		CommonTestUtils.logPass();
	}


	@Test
	public void testGetReleaseSingleThread() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Test simple get/release connection from 1 partition");

		config.setMinConnectionsPerPartition(30);
		config.setMaxConnectionsPerPartition(100);
		config.setAcquireIncrement(5);
		config.setPartitionCount(1);
		BoneCP dsb = new BoneCP(config);


		for (int i=0; i<60; i++){
			Connection conn = dsb.getConnection();
			conn.close();
		}
		assertEquals(0, dsb.getTotalLeased());
		assertEquals(30, dsb.getTotalFree());


		dsb.shutdown();
		CommonTestUtils.logPass();
	}


	/** Test that requesting connections from a partition that is empty will fetch it from other partitions that still have connections. */
	@Test
	public void testPartitionDrain() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Test connections obtained from alternate partition");

		config.setAcquireIncrement(1);
		config.setMinConnectionsPerPartition(10);
		config.setMaxConnectionsPerPartition(10);
		config.setPartitionCount(2);
		BoneCP dsb = new BoneCP(config);
		for (int i=0; i < 20; i++){
			dsb.getConnection();
		}
		assertEquals(20, dsb.getTotalLeased());
		assertEquals(0, dsb.getTotalFree());
		dsb.close();
		CommonTestUtils.logPass();
	}

	@Test
	public void testMultithreadSinglePartition() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Test multiple threads hitting a single partition concurrently");
		config.setAcquireIncrement(5);
		config.setMinConnectionsPerPartition(30);
		config.setMaxConnectionsPerPartition(100);
		config.setPartitionCount(1);
		config.setDisableConnectionTracking(true);
		config.setReleaseHelperThreads(0);
		BoneCPDataSource dsb = new BoneCPDataSource(config);
		dsb.setDriverClass("com.jolbox.bonecp.MockJDBCDriver");

		CommonTestUtils.startThreadTest(100, 100, dsb, 0, false);
		assertEquals(0, dsb.getTotalLeased());
		dsb.close();
		CommonTestUtils.logPass();
	}

	@Test
	public void testMultithreadMultiPartition() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Test multiple threads hitting a multiple partitions concurrently");
		config.setAcquireIncrement(5);
		config.setMinConnectionsPerPartition(10);
		config.setMaxConnectionsPerPartition(25);
		config.setPartitionCount(5);
		config.setReleaseHelperThreads(0);
		config.setDisableConnectionTracking(true);
		BoneCPDataSource dsb = new BoneCPDataSource(config);
		dsb.setDriverClass("com.jolbox.bonecp.MockJDBCDriver");
		CommonTestUtils.startThreadTest(100, 1000, dsb, 0, false);
		assertEquals(0, dsb.getTotalLeased());
		dsb.close();
		CommonTestUtils.logPass();
	}

	@Test
	public void testMultithreadMultiPartitionWithConstantWorkDelay() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Test multiple threads hitting a partition and doing some work on each connection");
		config.setAcquireIncrement(1);
		config.setMinConnectionsPerPartition(10);
		config.setMaxConnectionsPerPartition(10);
		config.setPartitionCount(1);

		BoneCPDataSource dsb = new BoneCPDataSource(config);
		dsb.setDriverClass("com.jolbox.bonecp.MockJDBCDriver");

		CommonTestUtils.startThreadTest(15, 10, dsb, 20, false);
		assertEquals(0, dsb.getTotalLeased());
		dsb.close();
		CommonTestUtils.logPass();
	}

	@Test
	public void testMultithreadMultiPartitionWithRandomWorkDelay() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Test multiple threads hitting a partition and doing some work of random duration on each connection");
		config.setAcquireIncrement(5);
		config.setMinConnectionsPerPartition(10);
		config.setMaxConnectionsPerPartition(25);
		config.setPartitionCount(5);

		BoneCPDataSource dsb = new BoneCPDataSource(config);
		dsb.setDriverClass("com.jolbox.bonecp.MockJDBCDriver");

		CommonTestUtils.startThreadTest(100, 10, dsb, -50, false);
		assertEquals(0, dsb.getTotalLeased());
		dsb.close();
		CommonTestUtils.logPass();
	}
	
	/** Tests that new connections are created on the fly. */
	@Test
	public void testConnectionCreate() throws InterruptedException, SQLException{
		CommonTestUtils.logTestInfo("Tests that new connections are created on the fly");
		config.setMinConnectionsPerPartition(10);
		config.setMaxConnectionsPerPartition(20);
		config.setAcquireIncrement(5);
		config.setPartitionCount(1);
		config.setReleaseHelperThreads(0);
		config.setPoolAvailabilityThreshold(0);

		BoneCP dsb = new BoneCP(config);

		assertEquals(10, dsb.getTotalCreatedConnections());
		assertEquals(0, dsb.getTotalLeased());

		Connection[] con = new Connection[10];
		for (int i=0; i < 10; i++){
			con[i] = dsb.getConnection(); // keep track of it to avoid finalizer
		}
		assertEquals(10, dsb.getTotalLeased());

		for (int i=0; i < 10; i++) {
			Thread.yield();
			Thread.sleep(200); // give time for pool watch thread to fire up
			if (dsb.getTotalCreatedConnections() == 15) {
				break;
			}
		}
		assertEquals(15, dsb.getTotalCreatedConnections());
		assertEquals(10, dsb.getTotalLeased());
		assertEquals(5, dsb.getTotalFree());
		
		for (Connection c : con){
			c.close();
		}
		
		dsb.shutdown();
		CommonTestUtils.logPass();
	}

	@Test
	public void testClosedConnection() throws InterruptedException, SQLException{
		BoneCP dsb = null ;
		CommonTestUtils.logTestInfo("Tests that closed connections trigger exceptions if use is attempted.");
		config.setMinConnectionsPerPartition(10);
		config.setMaxConnectionsPerPartition(20);
		config.setAcquireIncrement(5);
		config.setPartitionCount(1);
		try{
			dsb = new BoneCP(config);
			Connection conn = dsb.getConnection();
			conn.prepareCall(CommonTestUtils.TEST_QUERY);
			conn.close();
			try{
				conn.prepareCall(CommonTestUtils.TEST_QUERY);
				fail("Should have thrown an exception");
			} catch (SQLException e){
				CommonTestUtils.logPass();
			}
		} finally{
			if (dsb != null){
				dsb.shutdown();
			}
		}
	}

	/**
	 * Tests general methods.
	 * @throws CloneNotSupportedException 
	 */
	@Test
	public void testCloneEqualsHashCode() throws CloneNotSupportedException{
		config.setAcquireIncrement(5);
		config.setMinConnectionsPerPartition(30);
		config.setMaxConnectionsPerPartition(100);
		config.setPartitionCount(1);
		
		
		BoneCPDataSource dsb = new BoneCPDataSource(config);
		BoneCPDataSource clone = (BoneCPDataSource) dsb.clone();
		
		assertTrue(clone.hasSameConfiguration(dsb));
		
		assertFalse(clone.hasSameConfiguration(null));
		assertTrue(clone.hasSameConfiguration(dsb));
		
		clone.setJdbcUrl("something else");
		assertFalse(clone.hasSameConfiguration(dsb));
	}
	
	@Test
	public void testGetObjectInstance() throws Exception{
		config.setAcquireIncrement(5);
		config.setMinConnectionsPerPartition(30);
		config.setMaxConnectionsPerPartition(100);
		config.setPartitionCount(1);
		
		Reference mockRef = createNiceMock(Reference.class);
		Enumeration<RefAddr> mockEnum = createNiceMock(Enumeration.class);
		RefAddr mockRefAddr = createNiceMock(RefAddr.class);
		expect(mockRef.getAll()).andReturn(mockEnum).anyTimes();
		expect(mockEnum.hasMoreElements()).andReturn(true).times(2);
		
		expect(mockEnum.nextElement()).andReturn(mockRefAddr).anyTimes();
		expect(mockRefAddr.getType()).andReturn("driverClassName").once().andReturn("password").times(2);
		expect(mockRefAddr.getContent()).andReturn("com.jolbox.bonecp.MockJDBCDriver").once().andReturn("abcdefgh").once();
		replay(mockRef, mockEnum, mockRefAddr);
		BoneCPDataSource dsb = new BoneCPDataSource();
		BoneCPDataSource result = (BoneCPDataSource) dsb.getObjectInstance(mockRef, null, null, null);
		assertEquals("abcdefgh", result.getPassword());
		verify(mockRef, mockEnum, mockRefAddr);
	}


}
