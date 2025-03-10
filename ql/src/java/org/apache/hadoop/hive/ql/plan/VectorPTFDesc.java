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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.hive.common.type.DataTypePhysicalVariation;
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector.Type;
import org.apache.hadoop.hive.ql.exec.vector.expressions.ConstantVectorExpression;
import org.apache.hadoop.hive.ql.exec.vector.expressions.VectorExpression;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorBase;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorCount;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorBytesCountDistinct;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorCountStar;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalAvg;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalCountDistinct;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalFirstValue;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalLastValue;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalMax;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalMin;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDecimalSum;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDenseRank;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleAvg;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleCountDistinct;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleFirstValue;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleLastValue;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleMax;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleMin;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorDoubleSum;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLag;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLead;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongAvg;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongCountDistinct;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongFirstValue;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongLastValue;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongMax;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongMin;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorLongSum;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorRank;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorRowNumber;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDecimalAvg;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDecimalMax;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDecimalMin;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDecimalSum;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDoubleAvg;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDoubleMax;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDoubleMin;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingDoubleSum;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingLongAvg;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingLongMax;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingLongMin;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorStreamingLongSum;
import org.apache.hadoop.hive.ql.exec.vector.ptf.VectorPTFEvaluatorTimestampCountDistinct;
import org.apache.hadoop.hive.ql.parse.WindowingSpec.WindowType;
import org.apache.hadoop.hive.ql.plan.ptf.WindowFrameDef;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;

/**
 * VectorPTFDesc.
 *
 * Extra parameters beyond PTFDesc just for the VectorPTFOperator.
 *
 * We don't extend PTFDesc because the base OperatorDesc doesn't support
 * clone and adding it is a lot work for little gain.
 */
public class VectorPTFDesc extends AbstractVectorDesc  {

  private static final long serialVersionUID = 1L;

  public enum SupportedFunctionType {
    ROW_NUMBER,
    RANK,
    DENSE_RANK,
    MIN,
    MAX,
    SUM,
    AVG,
    FIRST_VALUE,
    LAST_VALUE,
    COUNT(true),
    LEAD,
    LAG;

    private final boolean supportDistinct;

    SupportedFunctionType() {
      supportDistinct = false;
    }

    SupportedFunctionType(boolean supportDistinct) {
      this.supportDistinct = supportDistinct;
    }

    public boolean isSupportDistinct() {
      return supportDistinct;
    }
  }

  public static HashMap<String, SupportedFunctionType> supportedFunctionsMap =
      new HashMap<String, SupportedFunctionType>();
  static {
    supportedFunctionsMap.put("row_number", SupportedFunctionType.ROW_NUMBER);
    supportedFunctionsMap.put("rank", SupportedFunctionType.RANK);
    supportedFunctionsMap.put("dense_rank", SupportedFunctionType.DENSE_RANK);
    supportedFunctionsMap.put("min", SupportedFunctionType.MIN);
    supportedFunctionsMap.put("max", SupportedFunctionType.MAX);
    supportedFunctionsMap.put("sum", SupportedFunctionType.SUM);
    supportedFunctionsMap.put("avg", SupportedFunctionType.AVG);
    supportedFunctionsMap.put("first_value", SupportedFunctionType.FIRST_VALUE);
    supportedFunctionsMap.put("last_value", SupportedFunctionType.LAST_VALUE);
    supportedFunctionsMap.put("count", SupportedFunctionType.COUNT);
    supportedFunctionsMap.put("lead", SupportedFunctionType.LEAD);
    supportedFunctionsMap.put("lag", SupportedFunctionType.LAG);
  }
  public static List<String> supportedFunctionNames = new ArrayList<String>();
  static {
    TreeSet<String> treeSet = new TreeSet<String>();
    treeSet.addAll(supportedFunctionsMap.keySet());
    supportedFunctionNames.addAll(treeSet);
  }

  private TypeInfo[] reducerBatchTypeInfos;
  private DataTypePhysicalVariation[] reducerBatchDataTypePhysicalVariations;

  private boolean isPartitionOrderBy;

  private String[] evaluatorFunctionNames;
  private boolean[] evaluatorsAreDistinct;
  private WindowFrameDef[] evaluatorWindowFrameDefs;
  private List<ExprNodeDesc>[] evaluatorInputExprNodeDescLists;

  private ExprNodeDesc[] orderExprNodeDescs;
  private ExprNodeDesc[] partitionExprNodeDescs;

  private String[] outputColumnNames;
  private TypeInfo[] outputTypeInfos;
  private DataTypePhysicalVariation[] outputDataTypePhysicalVariations;

  private VectorPTFInfo vectorPTFInfo;

  private int vectorizedPTFMaxMemoryBufferingBatchCount;

  public VectorPTFDesc() {
    isPartitionOrderBy = false;

    evaluatorFunctionNames = null;
    evaluatorsAreDistinct = null;
    evaluatorInputExprNodeDescLists = null;

    orderExprNodeDescs = null;
    partitionExprNodeDescs = null;

    outputColumnNames = null;
    outputTypeInfos = null;

    vectorizedPTFMaxMemoryBufferingBatchCount = -1;

  }

  // We provide this public method to help EXPLAIN VECTORIZATION show the evaluator classes.
  public static VectorPTFEvaluatorBase getEvaluator(SupportedFunctionType functionType,
      boolean isDistinct, WindowFrameDef windowFrameDef, Type[] columnVectorTypes,
      VectorExpression[] inputVectorExpressions, int outputColumnNum) {

    final boolean isRowEndCurrent = (windowFrameDef.getWindowType() == WindowType.ROWS
        && windowFrameDef.getEnd().isCurrentRow());
    /*
     * we should only stream when the window start is unbounded and the end row is the current,
     * because that's the way how streaming evaluation works: calculate from the very-first row then
     * create result for the current row on the fly, so with other words: currently we cannot force
     * a boundary on a streaming evaluator
     */
    final boolean canStream = windowFrameDef.getStart().isUnbounded() && isRowEndCurrent;

    // most of the evaluators will use only first argument
    VectorExpression inputVectorExpression = inputVectorExpressions[0];
    Type columnVectorType = columnVectorTypes[0];

    VectorPTFEvaluatorBase evaluator;
    switch (functionType) {
    case ROW_NUMBER:
      evaluator =
          new VectorPTFEvaluatorRowNumber(windowFrameDef, inputVectorExpression, outputColumnNum);
      break;
    case RANK:
      evaluator = new VectorPTFEvaluatorRank(windowFrameDef, outputColumnNum);
      break;
    case DENSE_RANK:
      evaluator = new VectorPTFEvaluatorDenseRank(windowFrameDef, outputColumnNum);
      break;
    case MIN:
      switch (columnVectorType) {
      case LONG:
        evaluator = !canStream ?
            new VectorPTFEvaluatorLongMin(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingLongMin(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DOUBLE:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDoubleMin(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDoubleMin(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DECIMAL:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDecimalMin(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDecimalMin(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      default:
        throw new RuntimeException("Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case MAX:
      switch (columnVectorType) {
      case LONG:
        evaluator = !canStream ?
            new VectorPTFEvaluatorLongMax(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingLongMax(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DOUBLE:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDoubleMax(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDoubleMax(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DECIMAL:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDecimalMax(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDecimalMax(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      default:
        throw new RuntimeException("Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case SUM:
      switch (columnVectorType) {
      case LONG:
        evaluator = !canStream ?
            new VectorPTFEvaluatorLongSum(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingLongSum(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DOUBLE:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDoubleSum(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDoubleSum(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DECIMAL:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDecimalSum(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDecimalSum(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      default:
        throw new RuntimeException("Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case AVG:
      switch (columnVectorType) {
      case LONG:
        evaluator = !canStream ?
            new VectorPTFEvaluatorLongAvg(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingLongAvg(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DOUBLE:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDoubleAvg(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDoubleAvg(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DECIMAL:
        evaluator = !canStream ?
            new VectorPTFEvaluatorDecimalAvg(
                windowFrameDef, inputVectorExpression, outputColumnNum) :
            new VectorPTFEvaluatorStreamingDecimalAvg(
                windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      default:
        throw new RuntimeException("Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case FIRST_VALUE:
      switch (columnVectorType) {
      case LONG:
        evaluator = new VectorPTFEvaluatorLongFirstValue(windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DOUBLE:
        evaluator = new VectorPTFEvaluatorDoubleFirstValue(windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DECIMAL:
        evaluator = new VectorPTFEvaluatorDecimalFirstValue(windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      default:
        throw new RuntimeException("Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case LAST_VALUE:
      switch (columnVectorType) {
      case LONG:
        evaluator = new VectorPTFEvaluatorLongLastValue(windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DOUBLE:
        evaluator = new VectorPTFEvaluatorDoubleLastValue(windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      case DECIMAL:
        evaluator = new VectorPTFEvaluatorDecimalLastValue(windowFrameDef, inputVectorExpression, outputColumnNum);
        break;
      default:
        throw new RuntimeException("Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case COUNT:
      if (inputVectorExpression == null) {
        evaluator = new VectorPTFEvaluatorCountStar(windowFrameDef, inputVectorExpression, outputColumnNum);
      } else {
        if (isDistinct) {
          switch (columnVectorType) {
          case BYTES:
            evaluator = new VectorPTFEvaluatorBytesCountDistinct(windowFrameDef,
                inputVectorExpression, outputColumnNum);
            break;
          case DECIMAL_64: //Decimal64ColumnVector is a LongColumnVector
          case LONG:
            evaluator = new VectorPTFEvaluatorLongCountDistinct(windowFrameDef,
                inputVectorExpression, outputColumnNum);
            break;
          case DOUBLE:
            evaluator = new VectorPTFEvaluatorDoubleCountDistinct(windowFrameDef,
                inputVectorExpression, outputColumnNum);
            break;
          case DECIMAL:
            evaluator = new VectorPTFEvaluatorDecimalCountDistinct(windowFrameDef,
                inputVectorExpression, outputColumnNum);
            break;
          case TIMESTAMP:
            evaluator = new VectorPTFEvaluatorTimestampCountDistinct(windowFrameDef,
                inputVectorExpression, outputColumnNum);
            break;
          default:
            throw new RuntimeException(
                "Unexpected column type for ptf count distinct: " + columnVectorType);
          }
        } else {
          evaluator =
              new VectorPTFEvaluatorCount(windowFrameDef, inputVectorExpression, outputColumnNum);
        }
      }
      break;
    case LAG:
      // lag(column, constant, ...)
      int amt = inputVectorExpressions.length > 1
        ? (int) ((ConstantVectorExpression) inputVectorExpressions[1]).getLongValue() : 1;

      // lag(column, constant, constant/column)
      VectorExpression defaultValueExpression =
          inputVectorExpressions.length > 2 ? inputVectorExpressions[2] : null;
      switch (columnVectorType) {
      case LONG:
      case DOUBLE:
      case DECIMAL:
        evaluator = new VectorPTFEvaluatorLag(windowFrameDef, inputVectorExpression,
            outputColumnNum, columnVectorType, amt, defaultValueExpression);
        break;
      default:
        throw new RuntimeException(
            "Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    case LEAD:
      // lead(column, constant, ...)
      amt = inputVectorExpressions.length > 1
        ? (int) ((ConstantVectorExpression) inputVectorExpressions[1]).getLongValue() : 1;

      // lead(column, constant, constant/column)
      defaultValueExpression = inputVectorExpressions.length > 2 ? inputVectorExpressions[2] : null;
      switch (columnVectorType) {
      case LONG:
      case DOUBLE:
      case DECIMAL:
        evaluator = new VectorPTFEvaluatorLead(windowFrameDef, inputVectorExpression,
            outputColumnNum, columnVectorType, amt, defaultValueExpression);
        break;
      default:
        throw new RuntimeException(
            "Unexpected column vector type " + columnVectorType + " for " + functionType);
      }
      break;
    default:
      throw new RuntimeException("Unexpected function type " + functionType);
    }
    return evaluator;
  }

  public static VectorPTFEvaluatorBase[] getEvaluators(VectorPTFDesc vectorPTFDesc, VectorPTFInfo vectorPTFInfo) {
    String[] evaluatorFunctionNames = vectorPTFDesc.getEvaluatorFunctionNames();
    boolean[] evaluatorsAreDistinct = vectorPTFDesc.getEvaluatorsAreDistinct();
    int evaluatorCount = evaluatorFunctionNames.length;
    WindowFrameDef[] evaluatorWindowFrameDefs = vectorPTFDesc.getEvaluatorWindowFrameDefs();
    VectorExpression[][] evaluatorInputExpressions = vectorPTFInfo.getEvaluatorInputExpressions();
    Type[][] evaluatorInputColumnVectorTypes = vectorPTFInfo.getEvaluatorInputColumnVectorTypes();

    int[] outputColumnMap = vectorPTFInfo.getOutputColumnMap();

    VectorPTFEvaluatorBase[] evaluators = new VectorPTFEvaluatorBase[evaluatorCount];
    for (int i = 0; i < evaluatorCount; i++) {
      String functionName = evaluatorFunctionNames[i];
      boolean isDistinct = evaluatorsAreDistinct[i];
      WindowFrameDef windowFrameDef = evaluatorWindowFrameDefs[i];
      SupportedFunctionType functionType = VectorPTFDesc.supportedFunctionsMap.get(functionName);
      VectorExpression[] inputVectorExpressions = evaluatorInputExpressions[i];
      final Type[] columnVectorTypes = evaluatorInputColumnVectorTypes[i];

      // The output* arrays start at index 0 for output evaluator aggregations.
      final int outputColumnNum = outputColumnMap[i];

      VectorPTFEvaluatorBase evaluator =
          VectorPTFDesc.getEvaluator(functionType, isDistinct,
              windowFrameDef, columnVectorTypes, inputVectorExpressions, outputColumnNum);

      evaluators[i] = evaluator;
    }
    return evaluators;
  }

  public static int[] getStreamingEvaluatorNums(VectorPTFEvaluatorBase[] evaluators) {
    final int evaluatorCount = evaluators.length;
    ArrayList<Integer> streamingEvaluatorNums = new ArrayList<Integer>();
    for (int i = 0; i < evaluatorCount; i++) {
      final VectorPTFEvaluatorBase evaluator = evaluators[i];
      if (evaluator.streamsResult()) {
        streamingEvaluatorNums.add(i);
      }
    }
    return ArrayUtils.toPrimitive(streamingEvaluatorNums.toArray(new Integer[0]));
  }

  public TypeInfo[] getReducerBatchTypeInfos() {
    return reducerBatchTypeInfos;
  }

  public void setReducerBatchTypeInfos(TypeInfo[] reducerBatchTypeInfos,
      DataTypePhysicalVariation[] reducerBatchDataTypePhysicalVariations) {
    this.reducerBatchTypeInfos = reducerBatchTypeInfos;
    this.reducerBatchDataTypePhysicalVariations = reducerBatchDataTypePhysicalVariations;
  }

  public boolean getIsPartitionOrderBy() {
    return isPartitionOrderBy;
  }

  public void setIsPartitionOrderBy(boolean isPartitionOrderBy) {
    this.isPartitionOrderBy = isPartitionOrderBy;
  }

  public String[] getEvaluatorFunctionNames() {
    return evaluatorFunctionNames;
  }

  public void setEvaluatorFunctionNames(String[] evaluatorFunctionNames) {
    this.evaluatorFunctionNames = evaluatorFunctionNames;
  }

  public void setEvaluatorsAreDistinct(boolean[] evaluatorsAreDistinct) {
    this.evaluatorsAreDistinct = evaluatorsAreDistinct;
  }

  public boolean[] getEvaluatorsAreDistinct() {
    return evaluatorsAreDistinct;
  }

  public WindowFrameDef[] getEvaluatorWindowFrameDefs() {
    return evaluatorWindowFrameDefs;
  }

  public void setEvaluatorWindowFrameDefs(WindowFrameDef[] evaluatorWindowFrameDefs) {
    this.evaluatorWindowFrameDefs = evaluatorWindowFrameDefs;
  }

  public List<ExprNodeDesc>[] getEvaluatorInputExprNodeDescLists() {
    return evaluatorInputExprNodeDescLists;
  }

  public void setEvaluatorInputExprNodeDescLists(List<ExprNodeDesc>[] evaluatorInputExprNodeDescLists) {
    this.evaluatorInputExprNodeDescLists = evaluatorInputExprNodeDescLists;
  }

  public ExprNodeDesc[] getOrderExprNodeDescs() {
    return orderExprNodeDescs;
  }

  public void setOrderExprNodeDescs(ExprNodeDesc[] orderExprNodeDescs) {
    this.orderExprNodeDescs = orderExprNodeDescs;
  }

  public ExprNodeDesc[] getPartitionExprNodeDescs() {
    return partitionExprNodeDescs;
  }

  public void setPartitionExprNodeDescs(ExprNodeDesc[] partitionExprNodeDescs) {
    this.partitionExprNodeDescs = partitionExprNodeDescs;
  }

  public String[] getOutputColumnNames() {
    return outputColumnNames;
  }

  public void setOutputColumnNames(String[] outputColumnNames) {
    this.outputColumnNames = outputColumnNames;
  }

  public TypeInfo[] getOutputTypeInfos() {
    return outputTypeInfos;
  }

  public DataTypePhysicalVariation[] getOutputDataTypePhysicalVariations() {
    return outputDataTypePhysicalVariations;
  }

  public void setOutputTypeInfos(TypeInfo[] outputTypeInfos,
      DataTypePhysicalVariation[] outputDataTypePhysicalVariations) {
    this.outputTypeInfos = outputTypeInfos;
    this.outputDataTypePhysicalVariations = outputDataTypePhysicalVariations;
  }

  public void setVectorPTFInfo(VectorPTFInfo vectorPTFInfo) {
    this.vectorPTFInfo = vectorPTFInfo;
  }

  public VectorPTFInfo getVectorPTFInfo() {
    return vectorPTFInfo;
  }

  public void setVectorizedPTFMaxMemoryBufferingBatchCount(
      int vectorizedPTFMaxMemoryBufferingBatchCount) {
    this.vectorizedPTFMaxMemoryBufferingBatchCount = vectorizedPTFMaxMemoryBufferingBatchCount;
  }

  public int getVectorizedPTFMaxMemoryBufferingBatchCount() {
    return vectorizedPTFMaxMemoryBufferingBatchCount;
  }
}
