package Mevolutrix.serviceBusEngine
import java.util
import ServiceAuthorization.UnauthorizedRequest
import spray.http._
import CSON.CSONDocument
import EntityStore.Interface._
import HandlerSocket.Protocol._
import EntityInterface.IEntitySet
import ExpressionParser.EntityRequest
import EntityStore.Metadata.SBEMetadata
import EntityAccess.GeneralEntityToCSON
import java.util.concurrent.ConcurrentHashMap
import EntityStore.Client.{EntityAccessor, EntityPreProcessor}

object DSEHandler {
  val appSpaceMap = new ConcurrentHashMap[String,String]()
  //get AppSpace Name with AppSpace alias
  def getAppSpace(alias:String): String = {
    Option(appSpaceMap.get(alias)).getOrElse(
      Option(appSpaceMap.putIfAbsent(alias,SBEMetadata.getAppSpace(alias).appSpaceName))
        .getOrElse(appSpaceMap.get(alias)))
  }
  private def extractURI(alias:String,method:String,setName:String,pKey:Object,filter:String,select:String) = {
    def lastName(name:String)= name.substring(name.lastIndexOf(".")+1,name.length)

    val sb = new StringBuilder()
    sb.append(method).append(":/").append(alias).append("/DSE/").append(lastName(setName))
    if (pKey!=null) sb.append("(@0)")
    else {
      if (filter!=null) sb.append("?$filter='").append(filter.trim).append("'")
      if (select!=null) {
        if (filter!=null) sb.append("&")
        else sb.append("?")
        sb.append("$select='").append(select).append("'")
      }
    }
    sb.toString()
  }
  type CheckAuth = (String) => Boolean
  private def produceResult(retMsg:String):HttpResponse =
    if (retMsg.contains("""{"Result":"""))
      HttpResponse(status = StatusCodes.Conflict,entity = HttpEntity(
        MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),retMsg))
    else HttpResponse(entity = HttpEntity(MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),retMsg))

  def get(reqContext:SystemRequestContext,entity:String,args:Map[String,String],authChecker:CheckAuth):HttpResponse = {
    val filter: String = EntityRequest.getField(args, "$filter")
    val selects: String = EntityRequest.getField(args, "$select")
    val (entityName, pKey) = EntityRequest.getKey(entity)
    val appSpace = reqContext.appSpaceID
    val schema = SBEMetadata.getSchema(appSpace + "." + entityName)
    val setName = schema.asInstanceOf[IEntitySet].setName
    val (queryFilter, params) = EntityRequest.parseFilter(filter)
    val uri = extractURI(reqContext.alias, "get", setName, pKey, queryFilter, selects)
    //println("URI:" + uri)
    if (!authChecker(uri)) throw new UnauthorizedRequest(uri,reqContext.userID)
    produceResult(if (pKey != null) {
      // Load by key
      val ret = ResultFormat.getJsonResult(EntityAccessor.loadByKey(pKey,
        reqContext, setName, schema).asInstanceOf[util.ArrayList[CSONDocument]])
        if (ret.size > 0) ret(0) else "[{ }]"
    } else {
      EntityAccessor.query(new SystemQueryRequest(queryFilter, selects, params), reqContext, setName, schema) match {
        case ret: java.util.List[CSONDocument] => ResultFormat.getJsonResult(ret).mkString("[", ",", "]")
        case a: (Int, Long) => """{"Result":"""" + MySQLErrorCodes(a._2) + """"}"""
      }
    })
  }
  def post(reqContext:SystemRequestContext,entity:String,data:String,authChecker:CheckAuth):HttpResponse = {
    // Post: /appSpace/DSE/EntityName(Id)   (Update the entity)
    val (entityName, pKey) = EntityRequest.getKey(entity)
    val appSpace = reqContext.appSpaceID
    val schema = SBEMetadata.getSchema(appSpace + "." + entityName)
    val setName = schema.asInstanceOf[IEntitySet].setName
    val uri = extractURI(reqContext.appSpaceID,"post",setName,pKey,null,null)
    //println("URI:" + uri)
    if (!authChecker(uri)) throw new UnauthorizedRequest(uri,reqContext.userID)
    produceResult(if (pKey != null) {
      val set = SBEMetadata.getEntitySet(setName)
      val binary = EntityPreProcessor(data, schema, set)
      EntityAccessor.update(reqContext, setName, pKey, binary, schema) match {
        case ret: java.util.List[CSONDocument] =>
          if (ret.size() > 0) ResultFormat.getJsonResult(ret)(0)
          else data
        case a: (Int, Long) => """{"Result":"""" + MySQLErrorCodes(a._2) + """"}"""
      }
    }
    else """{"Result":"Not a valid update by key request."}""")
  }
  def put(reqContext:SystemRequestContext,entity:String,data:String,authChecker:CheckAuth):HttpResponse = {
    val appSpace = reqContext.appSpaceID
    val (entityName, pKey) = EntityRequest.getKey(entity)
    val schema = SBEMetadata.getSchema(appSpace + "." + entityName)
    val setName = schema.asInstanceOf[IEntitySet].setName
    val uri = extractURI(reqContext.appSpaceID,"put",setName,pKey,null,null)
    //println("URI:" + uri)
    if (!authChecker(uri)) throw new UnauthorizedRequest(uri,reqContext.userID)
    produceResult({
      val set = SBEMetadata.getEntitySet(setName)
      val newEntity = EntityPreProcessor.preCreate(data, schema, set)
      val binary = newEntity.getBytes()
      // return the pKey in raw value format directly(with typeCode)
      val pKeyRawValue = if (set.getAutomaticProperty != null)
        newEntity.getRawValue(set.primaryKey)
      else GeneralEntityToCSON.getRawValue(pKey, true)
      EntityAccessor.insert(binary, reqContext, setName, schema, pKeyRawValue) match {
        case ret: java.util.List[CSONDocument] =>
          if (ret.size() > 0) ResultFormat.getJsonResult(ret)(0)
          else EntityAccess.CSON2JSON(newEntity.toCSONElement)
        case a: (Int, Long) => """{"Result":"""" + MySQLErrorCodes(a._2) + """"}"""
      }
    })
  }
  def delete(reqContext:SystemRequestContext,entity:String,authChecker:CheckAuth):HttpResponse = {
    val appSpace = reqContext.appSpaceID
    val (entityName, pKey) = EntityRequest.getKey(entity)
    val schema = SBEMetadata.getSchema(appSpace + "." + entityName)
    val setName = schema.asInstanceOf[IEntitySet].setName
    val uri = extractURI(reqContext.appSpaceID,"delete",setName,pKey,null,null)
    //println("URI:" + uri)
    if (!authChecker(uri)) throw new UnauthorizedRequest(uri,reqContext.userID)
    produceResult(if (pKey != null) {
        //CallDSE(DelEntity(pKey, appSpace, schema, setName)) match {
        EntityAccessor.delete(reqContext,setName,schema,Array[Object](pKey)) match {
          case ret:java.util.List[CSONDocument] => """{"Delete":true}"""
          case _ => """{"Delete":false}"""
        }
      } else """{"Result":"Not a valid delete key request."}""")
  }
}
