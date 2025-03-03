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
package org.apache.iotdb.itbase.env;

import org.apache.iotdb.confignode.rpc.thrift.IConfigNodeRPCService;
import org.apache.iotdb.it.env.ConfigNodeWrapper;
import org.apache.iotdb.it.env.DataNodeWrapper;
import org.apache.iotdb.jdbc.Constant;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface BaseEnv {

  void initBeforeClass() throws InterruptedException;

  void cleanAfterClass();

  void initBeforeTest() throws InterruptedException;

  void cleanAfterTest();

  Connection getConnection() throws SQLException;

  Connection getConnection(Constant.Version version) throws SQLException;

  void setTestMethodName(String testCaseName);

  void dumpTestJVMSnapshot();

  List<ConfigNodeWrapper> getConfigNodeWrapperList();

  void setConfigNodeWrapperList(List<ConfigNodeWrapper> configNodeWrapperList);

  List<DataNodeWrapper> getDataNodeWrapperList();

  void setDataNodeWrapperList(List<DataNodeWrapper> dataNodeWrapperList);

  IConfigNodeRPCService.Iface getConfigNodeConnection() throws IOException;
}
