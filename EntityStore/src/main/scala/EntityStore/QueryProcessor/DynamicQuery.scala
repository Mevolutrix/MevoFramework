package EntityStore.QueryProcessor
import CSON.Types._
import akka.actor._
import EntityAccess._
import EntityInterface._
import akka.util.Timeout
import ExpressionParser._
import EntityStore.Client._
import EntityStore.Interface._
import scala.concurrent.Await
import scala.concurrent.duration._
//import EntityStore.client.StoreAccessor._

class DynamicQuery(queryContext:IRequestContext,setInfo:IEntitySet, queryTarget:String = null) {
  private var statement : QueryStatement = _
  private var returnSchema : Option[IEntitySchema] = None
  
  private def buildQueryStatement(predicate:String,values:Array[Object]): QueryStatement = {
    val ret =new QueryStatement {
      queryMethod = QueryMethod.Query
      tenantID = queryContext.tenantID
      query = new QueryOperationSet {
      queryEntitySet = if (queryTarget!=null) queryTarget else setInfo.setName
      queryCondition = QueryCache.getQueryCondition(predicate, setInfo, values)
      }
    }
    if (values!=null && values.length>0) replaceParams(ret,values)
    ret
  }
  import AggregationFunction._
  private def buildProj(schema:IEntitySchema,key:String,aggVerb:AggregationFunction,isGroupKey:Boolean):Projection = {
    new Projection {
      aggregation = aggVerb
      isForGroup = isGroupKey
      alias = key
      projectionProperty = if (aggVerb!=Count) schema.getID(key).toString else "0"
    }
  }
  private def createProjection(schema:IEntitySchema,columns:Array[String]):Array[Projection] = {
    if (columns.isEmpty) null
    else {
      val ret = new Array[Projection](columns.length)
      val retSchema = new DynamicSchema("select")
      for (i<-0 until columns.length) {
        val property = columns(i)
        ret(i)=buildProj(schema,property,NA,false)
        retSchema.add(property, schema.getElementType(property))
      }// Build schema for the temp query projection result list
      returnSchema = Some(retSchema)
      ret
    }
  }
  private def getAggregationElelment(aggStatement:String):Tuple2[String,AggregationFunction] = {
    val indexOfKey = aggStatement.indexOf("(")
    val aggVerb = AggregationFunction.withName(aggStatement.substring(0, indexOfKey).trim())
    val key = aggStatement.substring(indexOfKey+1,aggStatement.indexOf(")")).trim
    (key,aggVerb)
  }
  private def createGroupBy(schema:IEntitySchema,groupKey:Array[String],groupValues:Array[String]):Array[Projection] = {
    val len = (if (groupKey==null || groupKey.isEmpty) 0
               else groupKey.length)
             +(if (groupKey==null || groupValues.isEmpty) 0
               else groupValues.length)
    val ret = new Array[Projection](len)
    val retSchema = new DynamicSchema("groupBy")
    
    if (!(groupKey==null) && !groupKey.isEmpty) for (i<-0 until groupKey.length) {
      val groupKeyName = groupKey(i)
      buildProj(schema,groupKeyName,NA,true)
      retSchema.add(groupKeyName, schema.getElementType(groupKeyName))
    }
    if (!(groupValues==null) && !groupValues.isEmpty) for (n<-0 until groupValues.length) {
      val aggElements = getAggregationElelment(groupValues(n))
      val aggPropertyName = if (aggElements._2==Count) "Count" else aggElements._1
      buildProj(schema,aggPropertyName,aggElements._2,true)
      retSchema.add(aggPropertyName, if (aggElements._2==Count) CSONTypesArray.IntElementType else schema.getElementType(aggPropertyName))
    }
    returnSchema = Some(retSchema)
    ret
  }
  
  def replaceParams(query:QueryStatement,argList:Array[Object]) =
    try {
      query.argList = EntityAccessor.getRawValueList(argList)
    } catch {case e:Exception => throw new Exception("Replace Params error.Args:"+argList.length+"-"+argList.mkString("|"))}
  
  def getStatement = statement
  def where(filter:String,argList:Array[Object]):DynamicQuery = { 
    statement = buildQueryStatement(filter,argList)
    this
  }
  def select(selectColumns:String):DynamicQuery = {
    if (selectColumns!=null) {
      if (statement == null) where("", null)
      getStatement.query.queryProjections = createProjection(setInfo.asInstanceOf[IEntitySchema], selectColumns.split(","))
    }
    this
  }
  def groupBy(groupByColumns:String,groupByValues:String):DynamicQuery = {
    if (!(groupByColumns==null) || !(groupByValues==null)) {
      if (statement == null) where("", null)
      val gByCols = if (groupByColumns != null) groupByColumns.split(",") else null
      val gByVals = if (groupByValues != null) groupByValues.split(",") else null
      getStatement.query.queryProjections = createGroupBy(setInfo.asInstanceOf[IEntitySchema], gByCols, gByVals)
    }
    this
  }
  import ReqSendMode._
  def execute(requestMode:ReqSendMode=syncMode,sender:ActorRef=null) = {
    val sa = DSEClientAccessor.getSession(queryContext.appSpaceID,queryContext.requestID,
             returnSchema.getOrElse(setInfo.asInstanceOf[IEntitySchema]),requestMode,sender)
    sa.query(getStatement,OutputMode.ToClient)
    returnSchema = None
    if (requestMode == syncMode) {
      val ret = sa.waitForResult
      ret
    }
    else null
  }
}
