package mevolutrix.Interface

import java.util
import CSON.CSONDocument
import EntityAccess.{JSONSerializer, GeneralEntityToCSON}
import EntityStore.Interface.{ResultFormat, IServiceAccessor}
import HandlerSocket.Protocol.MySQLErrorCodes
import akka.util._
import scala.concurrent.duration._

case class SAE_GetReq(appSpace:String,funcName:String,params:String,isSvc:Boolean)
case class SAE_PostReq(appSpace:String,funcName:String,params:String,postData:String,isSvc:Boolean)
/**
 * abstract interface for SME service actor
 */
trait SAEServiceAccessor {
  implicit val timeout:Timeout = Timeout(15.seconds)
  val appSpace:String = null
  val sa:IServiceAccessor = null
  def smePost(funcName:String)(params:String,data:String) = {
    sa.smePostRequest(appSpace,funcName,params,data)
    ResultFormat.getResult(sa.waitForResult)
  }
  // Forward mode used in SAE access actor
  def smePost(funcName:String, params:String,data:String) = {
    sa.smePostRequest(appSpace,funcName,params,data)
    // No need to wait as return result will be sent to the accessor actor
  }
  def smeGet(funcName:String)(params:String) = {
    sa.smeGetRequest(appSpace,funcName,params)
    ResultFormat.getResult(sa.waitForResult)
  }
  // Forward mode used in SAE access actor
  def smeGet(funcName:String,params:String) = {
    sa.smeGetRequest(appSpace,funcName,params)
    // No need to wait as return result will be sent to the accessor actor
  }
  def svcPost(funcName:String)(params:String,data:String) = {
    sa.svcPostRequest(appSpace,funcName,params,data)
    ResultFormat.getResult(sa.waitForResult)
  }
  // Forward mode used in SAE access actor
  def svcPost(funcName:String,params:String,data:String) = {
    sa.svcPostRequest(appSpace,funcName,params,data)
    // No need to wait as return result will be sent to the accessor actor
  }
  def svcGet(funcName:String)(params:String) = {
    sa.svcGetRequest(appSpace,funcName,params)
    ResultFormat.getResult(sa.waitForResult)
  }
  // Forward mode used in SAE access actor
  def svcGet(funcName:String,params:String) = {
    sa.svcGetRequest(appSpace, funcName, params)
    // No need to wait as return result will be sent to the accessor actor
  }
}

class SAEReqMsg {
  var isSMECall:Boolean = true
  var isPostCall:Boolean = false
  var appSpace:String=_
  var funcName:String=_
  var params:String=_
  var data:String=_
}

trait ISmeHandler {
  def getRequest(funcName:String,params:Map[String,String]):String
  def postRequest(funcName:String,params:Map[String,String],jsonBody:String):String
}