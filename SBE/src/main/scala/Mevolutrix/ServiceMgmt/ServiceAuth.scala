package Mevolutrix.ServiceMgmt
import java.util.concurrent.ConcurrentHashMap
import Mevolutrix.serviceBusEngine.DSEHandler._
import ServiceAuthorization.{UnauthorizedRequest, SecureTokenAuth}
import ExpressionParser._
import spray.http._

/**
 * Check and verify Service authorization
 */
object ServiceAuth {
  private val secureTokenCache = new ConcurrentHashMap[String,Int]()
  def checkAccessAuth(alias:String,uri:String,role:String):Boolean = {
    def checkRole(roleList:String,userRoles:Array[String]):Boolean = {
      var found = false
      userRoles.foreach(role=>{if (roleList.contains(role)) found=true})
      found
    }
    //val roleList = ServiceManager(alias).get(uri)
    //(roleList!=null) && checkRole(roleList,role.split("|"))
    true
  }
  def isAuthorized(token:String,alias:String,role:String,authorizedOp:(CheckAuth)=>HttpResponse):HttpResponse =
    try {
      if (!secureTokenCache.contains(token)) {
        if (SecureTokenAuth.verifySToken(token)) secureTokenCache.put(token, 1)
        else throw new UnauthorizedRequest("", "")
      }
      authorizedOp(ServiceAuth.checkAccessAuth(alias, _, role))
    }
    catch {
      case e:IllegalArgumentException =>
        HttpResponse(status = StatusCodes.Conflict,entity = HttpEntity(MediaTypes.`application/json`.
          withCharset(HttpCharsets.`UTF-8`), """{"Result":"""" + e + """"}"""))
      case e: UnauthorizedRequest =>
        HttpResponse(entity = HttpEntity(MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),
          "{Result:\"Unauthorized\"}"), status = StatusCodes.Unauthorized)
      case e:ParseException =>
        HttpResponse(status = StatusCodes.BadRequest,entity = HttpEntity(MediaTypes.`application/json`.
          withCharset(HttpCharsets.`UTF-8`),"""{"Result":"""" + e + """"}"""))
      case e: Exception => throw e
    }

}
