/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.iotdb.db.mpp.plan.expression.ternary;

import org.apache.iotdb.db.mpp.plan.analyze.TypeProvider;
import org.apache.iotdb.db.mpp.plan.expression.Expression;
import org.apache.iotdb.db.mpp.plan.expression.ExpressionType;
import org.apache.iotdb.db.mpp.transformation.api.LayerPointReader;
import org.apache.iotdb.db.mpp.transformation.dag.transformer.ternary.BetweenTransformer;
import org.apache.iotdb.db.mpp.transformation.dag.transformer.ternary.TernaryTransformer;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.utils.ReadWriteIOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BetweenExpression extends TernaryExpression {
  private final boolean isNotBetween;

  public boolean isNotBetween() {
    return isNotBetween;
  }

  public BetweenExpression(
      Expression firstExpression,
      Expression secondExpression,
      Expression thirdExpression,
      boolean isNotBetween) {
    super(firstExpression, secondExpression, thirdExpression);
    this.isNotBetween = isNotBetween;
  }

  public BetweenExpression(
      Expression firstExpression, Expression secondExpression, Expression thirdExpression) {
    super(firstExpression, secondExpression, thirdExpression);
    this.isNotBetween = false;
  }

  public BetweenExpression(ByteBuffer byteBuffer) {
    super(byteBuffer);
    this.isNotBetween = ReadWriteIOUtils.readBool(byteBuffer);
  }

  @Override
  protected TernaryTransformer constructTransformer(
      LayerPointReader firstParentLayerPointReader,
      LayerPointReader secondParentLayerPointReader,
      LayerPointReader thirdParentLayerPointReader) {
    return new BetweenTransformer(
        firstParentLayerPointReader,
        secondParentLayerPointReader,
        thirdParentLayerPointReader,
        isNotBetween);
  }

  @Override
  protected String operator() {
    return "between";
  }

  @Override
  public TSDataType inferTypes(TypeProvider typeProvider) {
    final String expressionString = toString();
    if (!typeProvider.containsTypeInfoOf(expressionString)) {
      firstExpression.inferTypes(typeProvider);
      secondExpression.inferTypes(typeProvider);
      thirdExpression.inferTypes(typeProvider);
      typeProvider.setType(expressionString, TSDataType.BOOLEAN);
    }
    return TSDataType.BOOLEAN;
  }

  @Override
  protected String getExpressionStringInternal() {
    return firstExpression + " BETWEEN " + secondExpression + " AND " + thirdExpression;
  }

  @Override
  public ExpressionType getExpressionType() {
    return ExpressionType.BETWEEN;
  }

  protected void serialize(ByteBuffer byteBuffer) {
    super.serialize(byteBuffer);
    ReadWriteIOUtils.write(isNotBetween, byteBuffer);
  }

  @Override
  protected void serialize(DataOutputStream stream) throws IOException {
    super.serialize(stream);
    ReadWriteIOUtils.write(isNotBetween, stream);
  }

  public Expression getExpression() {
    return this;
  }
}
