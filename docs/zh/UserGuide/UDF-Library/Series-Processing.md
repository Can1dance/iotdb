<!--

​    Licensed to the Apache Software Foundation (ASF) under one
​    or more contributor license agreements.  See the NOTICE file
​    distributed with this work for additional information
​    regarding copyright ownership.  The ASF licenses this file
​    to you under the Apache License, Version 2.0 (the
​    "License"); you may not use this file except in compliance
​    with the License.  You may obtain a copy of the License at
​    
​        http://www.apache.org/licenses/LICENSE-2.0
​    
​    Unless required by applicable law or agreed to in writing,
​    software distributed under the License is distributed on an
​    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
​    KIND, either express or implied.  See the License for the
​    specific language governing permissions and limitations
​    under the License.

-->

# 序列处理

## DEDUP

### 函数简介

本函数用于去除输入序列中的连续相同值。如输入序列`1，1，2，2，3`输出序列为`1，2，3`。

**函数名：** DEDUP

**输入序列：** 仅支持输入1个序列。

**参数：** 无

### 使用示例

原始数据：

```
+-----------------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
|                         Time|root.testDeDup.d1.s1|root.testDeDup.d1.s2|root.testDeDup.d1.s3|root.testDeDup.d1.s4|root.testDeDup.d1.s5|root.testDeDup.d1.s6|
+-----------------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
|1970-01-01T08:00:00.001+08:00|                true|                   1|                   1|                 1.0|                 1.0|              1test1|
|1970-01-01T08:00:00.002+08:00|                true|                   2|                   2|                 2.0|                 1.0|              2test2|
|1970-01-01T08:00:00.003+08:00|               false|                   1|                   2|                 1.0|                 1.0|              2test2|
|1970-01-01T08:00:00.004+08:00|                true|                   1|                   3|                 1.0|                 1.0|              1test1|
|1970-01-01T08:00:00.005+08:00|                true|                   1|                   3|                 1.0|                 1.0|              1test1|
+-----------------------------+--------------------+--------------------+--------------------+--------------------+--------------------+--------------------+
```

用于查询的SQL语句：

```sql
select deDup(s1), deDup(s2), deDup(s3), deDup(s4), deDup(s5), deDup(s6) from root.testDeDup.d1
```

输出序列：

```
+-----------------------------+---------------------------+---------------------------+---------------------------+---------------------------+---------------------------+---------------------------+
|                         Time|deDup(root.testDeDup.d1.s1)|deDup(root.testDeDup.d1.s2)|deDup(root.testDeDup.d1.s3)|deDup(root.testDeDup.d1.s4)|deDup(root.testDeDup.d1.s5)|deDup(root.testDeDup.d1.s6)|
+-----------------------------+---------------------------+---------------------------+---------------------------+---------------------------+---------------------------+---------------------------+
|1970-01-01T08:00:00.001+08:00|                       true|                          1|                          1|                        1.0|                        1.0|                     1test1|
|1970-01-01T08:00:00.002+08:00|                       null|                          2|                          2|                        2.0|                       null|                     2test2|
|1970-01-01T08:00:00.003+08:00|                      false|                          1|                       null|                        1.0|                       null|                       null|
|1970-01-01T08:00:00.004+08:00|                       true|                       null|                          3|                       null|                       null|                     1test1|
+-----------------------------+---------------------------+---------------------------+---------------------------+---------------------------+---------------------------+---------------------------+
```