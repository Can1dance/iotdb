/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.iotdb.db.it.env;

import org.apache.iotdb.confignode.rpc.thrift.IConfigNodeRPCService;
import org.apache.iotdb.db.utils.EnvironmentUtils;
import org.apache.iotdb.it.env.ConfigNodeWrapper;
import org.apache.iotdb.it.env.DataNodeWrapper;
import org.apache.iotdb.itbase.env.BaseEnv;
import org.apache.iotdb.jdbc.Config;
import org.apache.iotdb.jdbc.Constant;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.apache.iotdb.jdbc.Config.VERSION;
import static org.junit.Assert.fail;

/** This class is used by EnvFactory with using reflection. */
public class StandaloneEnv implements BaseEnv {

  @Override
  public void initBeforeClass() {
    EnvironmentUtils.envSetUp();
  }

  @Override
  public void cleanAfterClass() {
    cleanAfterTest();
  }

  @Override
  public void initBeforeTest() {

    EnvironmentUtils.envSetUp();
  }

  @Override
  public void cleanAfterTest() {
    try {
      EnvironmentUtils.cleanEnv();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    try {
      Class.forName(Config.JDBC_DRIVER_NAME);
      return DriverManager.getConnection(
          Config.IOTDB_URL_PREFIX + "127.0.0.1:6667/", "root", "root");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      fail();
    }
    return null;
  }

  @Override
  public Connection getConnection(Constant.Version version) throws SQLException {
    try {
      Class.forName(Config.JDBC_DRIVER_NAME);
      return DriverManager.getConnection(
          Config.IOTDB_URL_PREFIX + "127.0.0.1:6667" + "?" + VERSION + "=" + version.toString(),
          "root",
          "root");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      fail();
    }
    return null;
  }

  public void setTestMethodName(String testCaseName) {
    // Do nothing
  }

  public void dumpTestJVMSnapshot() {
    // Do nothing
  }

  @Override
  public List<ConfigNodeWrapper> getConfigNodeWrapperList() {
    return null;
  }

  @Override
  public void setConfigNodeWrapperList(List<ConfigNodeWrapper> configNodeWrapperList) {
    // Do nothing
  }

  @Override
  public List<DataNodeWrapper> getDataNodeWrapperList() {
    return null;
  }

  @Override
  public void setDataNodeWrapperList(List<DataNodeWrapper> dataNodeWrapperList) {
    // Do nothing
  }

  @Override
  public IConfigNodeRPCService.Iface getConfigNodeConnection() throws IOException {
    return null;
  }
}
