package EntityStore.Interface

object LogicOperator extends Enumeration {
  type LogicOperator = Value
  val And = Value(0)
  val Or = Value(1)
}
object CompareOperator extends Enumeration {
  type CompareOperator = Value
  val Equal = Value(0)
  val Greater = Value(1)
  val Less = Value(2)
  val GreaterEqual = Value(3)
  val LessEqual = Value(4)
  val Like = Value(5)
  val In = Value(6)
  val BetweenAnd = Value(7)
  val NotEqual = Value(8)
  val NotIn = Value(9)
  val IsNull = Value(10)
  val IsNotNull = Value(11)
}
object LikeCompareOperatorOption extends Enumeration {
  type LikeCompareOperatorOption = Value
  val Left = Value(0)
  val LeftEqual = Value(1)
  val Right = Value(2)
  val RightEqual = Value(3)
  val Include = Value(4)
  val IncludeEqual = Value(5)
}
object AggregationFunction extends Enumeration {
  type AggregationFunction = Value
  val NA = Value(0)
  val Sum = Value(1)
  val Count = Value(2)
  val Min = Value(3)
  val Max = Value(4)
}
class QueryOperationSet {
  var queryEntitySet:String = null    // The name of entitySet to be queried
  var queryProjections:Array[Projection] = null   // select GroupBy expression sub-statements
  var queryCondition:Condition = null
}
class QueryStatement extends OperationStatement {
  var query : QueryOperationSet = null
  var tenantID : Int = 0
  var processingScript : String = null
}
class Condition {
  import LogicOperator._
  var isAbstractGroup:Boolean = false   // this flag indicates that this condition is just And/Or and details in SubConditions
  var operator:LogicOperator = And
  var joinOnKey:Boolean = false          // Todo: implement the primary key search
  var queryOperations:Array[SearchOperation] = null
  var subConditions:Array[Condition] = null
}

object KeyType extends Enumeration {
  type KeyType = Value
  val Index = Value(0)
  val PrimaryKey = Value(1)
  val Property = Value(2)
}
class SearchOperation {
  import KeyType._
  import CompareOperator._
  var compareOperator:CompareOperator = Equal
  var searchPropertyName:String = null //keyType=0:Index name(May be child property index);  keyType=1,nullelement(Search with the primary key);  type=2 property name value
  /**
   * Index = 0, PrimaryKey = 1, Property = 2
   */
  var keyType:KeyType = Property
  var paramertIndex:Int = -1
  var compareValue:Array[SearchValue]=null
  var subQuery:SubQuery = null
}

object LikeMode extends Enumeration {
  type LikeMode = Value
  val Contains = Value(0) 
  val BeginWith = Value(1) 
  val EndWith = Value(2) 
  val NA = Value(3) 
}

class SearchValue {
  import LikeMode._
  
  var value:Array[Byte] = null
  var operationMode:LikeMode = NA     
}
class Projection {
  import AggregationFunction._
  var aggregation:AggregationFunction = NA
  var alias:String = null   
  var isForGroup:Boolean = false
  var projectionProperty:String = null
}

class SubQuery {
  type Binary = Array[Byte]
  // ?????????????????????????????Join??????????????Like??>, <??????????Inset?????????????Server???????
  //var OperationMode:Int = 0 
  var cacheId:java.util.UUID = null // 128 bit UUID 
  var argumentList:Array[Binary]=null // ???????
  var queryOperation:QueryOperationSet = null
  var tenantID:Int = 0
}
