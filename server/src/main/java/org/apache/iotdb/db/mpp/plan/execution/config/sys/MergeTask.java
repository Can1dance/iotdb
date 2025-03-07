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

package org.apache.iotdb.db.mpp.plan.execution.config.sys;

import org.apache.iotdb.confignode.rpc.thrift.TMergeReq;
import org.apache.iotdb.db.conf.IoTDBConfig;
import org.apache.iotdb.db.conf.IoTDBDescriptor;
import org.apache.iotdb.db.mpp.plan.execution.config.ConfigTaskResult;
import org.apache.iotdb.db.mpp.plan.execution.config.IConfigTask;
import org.apache.iotdb.db.mpp.plan.execution.config.executor.IConfigTaskExecutor;
import org.apache.iotdb.db.mpp.plan.statement.sys.MergeStatement;

import com.google.common.util.concurrent.ListenableFuture;

public class MergeTask implements IConfigTask {

  private MergeStatement mergeStatement;

  public MergeTask(MergeStatement mergeStatement) {
    this.mergeStatement = mergeStatement;
  }

  @Override
  public ListenableFuture<ConfigTaskResult> execute(IConfigTaskExecutor configTaskExecutor)
      throws InterruptedException {
    TMergeReq mergeReq = new TMergeReq();
    IoTDBConfig config = IoTDBDescriptor.getInstance().getConfig();
    if (mergeStatement.isCluster()) {
      mergeReq.setDataNodeId(-1);
    } else {
      mergeReq.setDataNodeId(config.getDataNodeId());
    }
    return configTaskExecutor.merge(mergeReq);
  }
}
