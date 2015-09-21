package EntityStore.QueryProcessor

import java.util.concurrent.ConcurrentHashMap

import EntityAccess._
import ExpressionParser._
import EntityInterface._
import EntityStore.Interface._
import spray.caching.SimpleLruCache
import scala.concurrent.Await
import scala.concurrent.duration._

object QueryCache {
  import scala.concurrent.ExecutionContext.Implicits.global
  private val conditionMap = new ConcurrentHashMap[String,ParseCondition]
  def getQueryCondition(predicate:String, setInfo:IEntitySet,values:Array[Object]=null):Condition = {
    val key = setInfo.setName+":"+predicate
    val ret = if (conditionMap.containsKey(key)) conditionMap.get(key)
              else {
                val cond = new ParseCondition(predicate,values,setInfo)
                conditionMap.put(key,cond)
                cond
              }
    ret.queryCondition
  }
}
class ParseCondition(predicate:String, values:Array[Object],schema:IEntitySet) {
  import KeyType._
  import QueryMethod._
  import LogicOperator._
  import CompareOperator._
  type Binary = Array[Byte]

  val exp = if (predicate==null || predicate.isEmpty()) null else new Parser(predicate,values)
  val queryCondition = getCondition(exp)

  private def getCondition(parser:Parser) = {
    if (parser==null)new Condition {
      isAbstractGroup = false
      operator = And
      subConditions = null
      queryOperations = null
    } else visit(parser.parse)

    if (curSearch!=null) curCondition = new Condition {
      isAbstractGroup = false
      operator = And
      subConditions = null
      queryOperations = Array(curSearch)
    }
    curCondition
  }
  private var curCondition:Condition = _
  private var curSearch:SearchOperation = _
  private var curIsNot:Boolean = false
  private def propertyKeyType(property:String):KeyType = {
    if (schema.isIndex(property)) Index
    else if (schema.primaryKey==property) PrimaryKey
    else Property
  }
  private def getParamIndex(paramExpression:Expression):Int = {
    if (paramExpression.isInstanceOf[ConstantExpression] && paramExpression.name.startsWith("@"))
      paramExpression.name.substring(1).toInt
    else -1
  }
  private def visit(exp:Expression):Expression = {
    exp match {
      case unaryExp:UnaryExpression => visitUnary(unaryExp)
      case binaryExp:BinaryExpression => visitBinary(binaryExp)
      case methodExp:MethodExpression => visitMethod(methodExp)
      case _ => exp
    }
  }
  private def visitUnary(exp:UnaryExpression):Expression = {
    val originalIsNot = curIsNot
    try {
      if (exp.getOperand=="not") {
        curIsNot = !originalIsNot
        visit(exp.getLeft)
      }
      else exp
    }
    finally {curIsNot = originalIsNot}
  }
  private def visitBinary(exp:BinaryExpression):Expression = {
    var searchOp:SearchOperation = null
    var group:Condition = null

    exp.getOperand match {
      case "and" => group = buildCondition(if (curIsNot) Or else And,exp.getExpList) 
      case "or" => group = buildCondition(if (curIsNot) And else Or,exp.getExpList)
      case "Equal" => searchOp = buildSearchOperation(if (curIsNot) NotEqual else Equal,exp.getExpList.iterator)
      case "NotEqual" => searchOp = buildSearchOperation(if (curIsNot) Equal else NotEqual,exp.getExpList.iterator)
      case "LessThan" => searchOp = buildSearchOperation(if (curIsNot) GreaterEqual else Less,exp.getExpList.iterator)
      case "LessThanEqual" => searchOp = buildSearchOperation(if (curIsNot) Greater else LessEqual,exp.getExpList.iterator)
      case "GreaterThan" => searchOp = buildSearchOperation(if (curIsNot) LessEqual else Greater,exp.getExpList.iterator)
      case "GreaterThanEqual" => searchOp = buildSearchOperation(if (curIsNot) Less else GreaterEqual,exp.getExpList.iterator)
      case _ => throw new Exception("The binary operator:"+exp.getOperand+" is not supported!")
    }
    curCondition = group
    curSearch = searchOp
    exp
  }
  private def visitMethod(exp:MethodExpression):Expression = {
    var searchOp:SearchOperation = null
    var group:Condition = null
    exp.name match {
      case "substringof" => searchOp = buildSearchOperation(if (curIsNot) GreaterEqual else Like,exp.getArgList.iterator,LikeMode.Contains)
      case "startswith" => searchOp =  buildSearchOperation(if (curIsNot) GreaterEqual else Like,exp.getArgList.iterator,LikeMode.BeginWith)
      case "endswith" => searchOp = buildSearchOperation(if (curIsNot) GreaterEqual else Like,exp.getArgList.iterator,LikeMode.EndWith)
      case "InSet" => searchOp =  buildSearchOperation(if (curIsNot) NotIn else In,exp.getArgList.iterator)
    }
    curCondition = group
    curSearch = searchOp
    exp
  }
  private def buildCondition(logicOp:LogicOperator,expList:List[Expression]):Condition = {
    val subGroups = new java.util.ArrayList[Condition]
    val searchOpList = new java.util.ArrayList[SearchOperation]
    expList.foreach((exp:Expression)=>{
      visit(exp)
      if (curSearch!=null) searchOpList.add(curSearch)
      if (curCondition!=null) subGroups.add(curCondition)
    })
    val subGroupSize = subGroups.size()
    val queryOpSize = searchOpList.size()
    new Condition {
      isAbstractGroup = subGroupSize>0
      operator = logicOp
      subConditions = if (subGroupSize>0) subGroups.toArray[Condition](new Array[Condition](subGroupSize)) else null
      queryOperations = if (queryOpSize>0) searchOpList.toArray[SearchOperation](new Array[SearchOperation](queryOpSize)) else null 
    }
  }
  import LikeMode._

  private def buildSearchOperation(comparer:CompareOperator,expList:Iterator[Expression],opMode:LikeMode=NA):SearchOperation = {
    var memberExp:MemberExpression = null
    var constExp:ConstantExpression = null

    expList.foreach((exp:Expression)=>{
      if (exp.isInstanceOf[MemberExpression])memberExp = exp.asInstanceOf[MemberExpression]
      if (exp.isInstanceOf[ConstantExpression])constExp = exp.asInstanceOf[ConstantExpression]
    })
    val compareName = memberExp.name
    new SearchOperation {
      compareOperator = if (constExp.name=="null") {
                if (comparer==Equal||comparer==NotEqual) {
                  if (comparer==Equal) IsNull else IsNotNull
                }else throw new IllegalArgumentException("null value can only be used in == or != comparer.")
                } else comparer
      keyType = propertyKeyType(compareName)
      searchPropertyName = keyType match { 
        case PrimaryKey => null
        case Index => compareName
        case Property => schema.asInstanceOf[IEntitySchema].getID(compareName).toString
      } 
      paramertIndex = if (constExp.value.isInstanceOf[QueryStatement]) -1
                else if (constExp.name.startsWith("@")) constExp.name.substring(1).toInt else -1
      compareValue = getSearchValues(constExp,comparer,opMode)
      subQuery = if (constExp.value.isInstanceOf[QueryStatement]) 
               new SubQuery {
               queryOperation = constExp.value.asInstanceOf[QueryStatement].query
               argumentList = constExp.value.asInstanceOf[QueryStatement].argList
               cacheId = constExp.value.asInstanceOf[QueryStatement].cacheID
               }
             else null
    }
  }
  private def getSearchValues(valueExpression:ConstantExpression,comparer:CompareOperator,opMode:LikeMode):Array[SearchValue] = {
    if (valueExpression.value.isInstanceOf[QueryStatement] || valueExpression.name.startsWith("@")) null
    else if (comparer == In || comparer == NotIn) {
      val values = valueExpression.value.asInstanceOf[Array[Binary]]
      val ret = new Array[SearchValue](values.length)
      for (i<-0 until values.length) ret(i) = new SearchValue {value = values(i)}
      ret
    }
    else Array(new SearchValue {
                 operationMode = opMode
                 value = GeneralEntityToCSON.getRawValue(valueExpression.value, true)})
  }
}