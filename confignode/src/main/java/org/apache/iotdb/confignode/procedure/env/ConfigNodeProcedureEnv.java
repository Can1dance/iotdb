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

package org.apache.iotdb.confignode.procedure.env;

import org.apache.iotdb.common.rpc.thrift.TConfigNodeLocation;
import org.apache.iotdb.common.rpc.thrift.TDataNodeConfiguration;
import org.apache.iotdb.common.rpc.thrift.TSStatus;
import org.apache.iotdb.confignode.client.ConfigNodeRequestType;
import org.apache.iotdb.confignode.client.DataNodeRequestType;
import org.apache.iotdb.confignode.client.async.datanode.AsyncDataNodeClientPool;
import org.apache.iotdb.confignode.client.sync.confignode.SyncConfigNodeClientPool;
import org.apache.iotdb.confignode.client.sync.datanode.SyncDataNodeClientPool;
import org.apache.iotdb.confignode.consensus.request.write.DeleteStorageGroupPlan;
import org.apache.iotdb.confignode.consensus.request.write.PreDeleteStorageGroupPlan;
import org.apache.iotdb.confignode.exception.AddConsensusGroupException;
import org.apache.iotdb.confignode.exception.AddPeerException;
import org.apache.iotdb.confignode.manager.ConfigManager;
import org.apache.iotdb.confignode.procedure.exception.ProcedureException;
import org.apache.iotdb.confignode.procedure.scheduler.LockQueue;
import org.apache.iotdb.confignode.procedure.scheduler.ProcedureScheduler;
import org.apache.iotdb.confignode.rpc.thrift.TAddConsensusGroupReq;
import org.apache.iotdb.mpp.rpc.thrift.TInvalidateCacheReq;
import org.apache.iotdb.rpc.TSStatusCode;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ConfigNodeProcedureEnv {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigNodeProcedureEnv.class);

  /** add or remove node lock */
  private final LockQueue nodeLock = new LockQueue();

  private final ReentrantLock schedulerLock = new ReentrantLock();

  private final ConfigManager configManager;

  private final ProcedureScheduler scheduler;

  private final DataNodeRemoveHandler dataNodeRemoveHandler;

  private static boolean skipForTest = false;

  private static boolean invalidCacheResult = true;

  public static void setSkipForTest(boolean skipForTest) {
    ConfigNodeProcedureEnv.skipForTest = skipForTest;
  }

  public static void setInvalidCacheResult(boolean result) {
    ConfigNodeProcedureEnv.invalidCacheResult = result;
  }

  public ConfigNodeProcedureEnv(ConfigManager configManager, ProcedureScheduler scheduler) {
    this.configManager = configManager;
    this.scheduler = scheduler;
    this.dataNodeRemoveHandler = new DataNodeRemoveHandler(configManager);
  }

  public ConfigManager getConfigManager() {
    return configManager;
  }

  /**
   * Delete ConfigNode cache, includes ClusterSchemaInfo and PartitionInfo
   *
   * @param name storage group name
   * @return tsStatus
   */
  public TSStatus deleteConfig(String name) {
    DeleteStorageGroupPlan deleteStorageGroupPlan = new DeleteStorageGroupPlan(name);
    return configManager.getClusterSchemaManager().deleteStorageGroup(deleteStorageGroupPlan);
  }

  /**
   * Pre delete a storage group
   *
   * @param preDeleteType execute/rollback
   * @param deleteSgName storage group name
   */
  public void preDelete(
      PreDeleteStorageGroupPlan.PreDeleteType preDeleteType, String deleteSgName) {
    configManager.getPartitionManager().preDeleteStorageGroup(deleteSgName, preDeleteType);
  }

  /**
   * @param storageGroupName Storage group name
   * @return ALL SUCCESS OR NOT
   * @throws IOException IOE
   * @throws TException Thrift IOE
   */
  public boolean invalidateCache(String storageGroupName) throws IOException, TException {
    // TODO: Remove it after IT is supported
    if (skipForTest) {
      return invalidCacheResult;
    }
    List<TDataNodeConfiguration> allDataNodes =
        configManager.getNodeManager().getRegisteredDataNodes(-1);
    TInvalidateCacheReq invalidateCacheReq = new TInvalidateCacheReq();
    invalidateCacheReq.setStorageGroup(true);
    invalidateCacheReq.setFullPath(storageGroupName);
    for (TDataNodeConfiguration dataNodeConfiguration : allDataNodes) {
      final TSStatus invalidateSchemaStatus =
          SyncDataNodeClientPool.getInstance()
              .sendSyncRequestToDataNodeWithRetry(
                  dataNodeConfiguration.getLocation().getInternalEndPoint(),
                  invalidateCacheReq,
                  DataNodeRequestType.INVALIDATE_SCHEMA_CACHE);
      final TSStatus invalidatePartitionStatus =
          SyncDataNodeClientPool.getInstance()
              .sendSyncRequestToDataNodeWithRetry(
                  dataNodeConfiguration.getLocation().getInternalEndPoint(),
                  invalidateCacheReq,
                  DataNodeRequestType.INVALIDATE_PARTITION_CACHE);
      if (!verifySucceed(invalidatePartitionStatus, invalidateSchemaStatus)) {
        LOG.error(
            "Invalidate cache failed, invalidate partition cache status is {}， invalidate schema cache status is {}",
            invalidatePartitionStatus,
            invalidateSchemaStatus);
        return false;
      }
    }
    return true;
  }

  public boolean verifySucceed(TSStatus... status) {
    return Arrays.stream(status)
        .allMatch(tsStatus -> tsStatus.getCode() == TSStatusCode.SUCCESS_STATUS.getStatusCode());
  }

  /**
   * Let the remotely new ConfigNode build the ConsensusGroup
   *
   * @param tConfigNodeLocation New ConfigNode's location
   */
  public void addConsensusGroup(TConfigNodeLocation tConfigNodeLocation)
      throws AddConsensusGroupException {
    List<TConfigNodeLocation> configNodeLocations =
        new ArrayList<>(configManager.getNodeManager().getRegisteredConfigNodes());
    configNodeLocations.add(tConfigNodeLocation);
    TSStatus status =
        (TSStatus)
            SyncConfigNodeClientPool.getInstance()
                .sendSyncRequestToConfigNodeWithRetry(
                    tConfigNodeLocation.getInternalEndPoint(),
                    new TAddConsensusGroupReq(configNodeLocations),
                    ConfigNodeRequestType.ADD_CONSENSUS_GROUP);
    if (status.getCode() != TSStatusCode.SUCCESS_STATUS.getStatusCode()) {
      throw new AddConsensusGroupException(tConfigNodeLocation);
    }
  }

  /**
   * Leader will add the new ConfigNode Peer into PartitionRegion
   *
   * @param configNodeLocation The new ConfigNode
   * @throws AddPeerException When addPeer doesn't success
   */
  public void addConfigNodePeer(TConfigNodeLocation configNodeLocation) throws AddPeerException {
    configManager.getConsensusManager().addConfigNodePeer(configNodeLocation);
  }

  /**
   * Remove peer in Leader node
   *
   * @param tConfigNodeLocation node is removed
   * @throws ProcedureException if failed status
   */
  public void removeConfigNodePeer(TConfigNodeLocation tConfigNodeLocation)
      throws ProcedureException {
    TSStatus tsStatus = configManager.getNodeManager().removeConfigNodePeer(tConfigNodeLocation);
    if (tsStatus.getCode() != TSStatusCode.SUCCESS_STATUS.getStatusCode()) {
      throw new ProcedureException(tsStatus.getMessage());
    }
  }

  /**
   * Remove Consensus Group in removed node
   *
   * @param tConfigNodeLocation config node location
   * @throws ProcedureException if failed status
   */
  public void removeConsensusGroup(TConfigNodeLocation tConfigNodeLocation)
      throws ProcedureException {
    TSStatus tsStatus =
        (TSStatus)
            SyncConfigNodeClientPool.getInstance()
                .sendSyncRequestToConfigNodeWithRetry(
                    tConfigNodeLocation.getInternalEndPoint(),
                    tConfigNodeLocation,
                    ConfigNodeRequestType.REMOVE_CONSENSUS_GROUP);
    if (tsStatus.getCode() != TSStatusCode.SUCCESS_STATUS.getStatusCode()) {
      throw new ProcedureException(tsStatus.getMessage());
    }
  }

  /**
   * Stop Config Node
   *
   * @param tConfigNodeLocation config node location
   * @throws ProcedureException if failed status
   */
  public void stopConfigNode(TConfigNodeLocation tConfigNodeLocation) throws ProcedureException {
    TSStatus tsStatus =
        (TSStatus)
            SyncConfigNodeClientPool.getInstance()
                .sendSyncRequestToConfigNodeWithRetry(
                    tConfigNodeLocation.getInternalEndPoint(),
                    tConfigNodeLocation,
                    ConfigNodeRequestType.STOP_CONFIG_NODE);
    if (tsStatus.getCode() != TSStatusCode.SUCCESS_STATUS.getStatusCode()) {
      throw new ProcedureException(tsStatus.getMessage());
    }
  }

  /**
   * Leader will record the new ConfigNode's information
   *
   * @param configNodeLocation The new ConfigNode
   */
  public void applyConfigNode(TConfigNodeLocation configNodeLocation) {
    configManager.getNodeManager().applyConfigNode(configNodeLocation);
  }

  /**
   * Leader will notify the new ConfigNode that registration success
   *
   * @param configNodeLocation The new ConfigNode
   */
  public void notifyRegisterSuccess(TConfigNodeLocation configNodeLocation) {
    SyncConfigNodeClientPool.getInstance()
        .sendSyncRequestToConfigNodeWithRetry(
            configNodeLocation.getInternalEndPoint(),
            null,
            ConfigNodeRequestType.NOTIFY_REGISTER_SUCCESS);
  }

  /** notify all DataNodes when the capacity of the ConfigNodeGroup is expanded or reduced */
  public void broadCastTheLatestConfigNodeGroup() {
    AsyncDataNodeClientPool.getInstance()
        .broadCastTheLatestConfigNodeGroup(
            configManager.getNodeManager().getRegisteredDataNodeLocations(-1),
            configManager.getNodeManager().getRegisteredConfigNodes());
  }

  public LockQueue getNodeLock() {
    return nodeLock;
  }

  public ProcedureScheduler getScheduler() {
    return scheduler;
  }

  public LockQueue getRegionMigrateLock() {
    return dataNodeRemoveHandler.getRegionMigrateLock();
  }

  public ReentrantLock getSchedulerLock() {
    return schedulerLock;
  }

  public DataNodeRemoveHandler getDataNodeRemoveHandler() {
    return dataNodeRemoveHandler;
  }
}
