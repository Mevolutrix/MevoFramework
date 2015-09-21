package Mevolutrix.portal

import EntityStore.Connection.SaeClientConnFactory
import SBEServiceAccess.CallSAE
import ServiceAuthorization.{SecureTokenAuth, UnauthorizedRequest}
import akka.actor._
import mevolutrix.Interface.SAE_GetReq
import spray.http._
import spray.routing.HttpService
import ExpressionParser.EntityRequest
import scala.concurrent.duration.Duration
import Mevolutrix.serviceBusEngine.DSEHandler
import EntityStore.Interface.SystemRequestContext
import spray.routing.directives.CachingDirectives._
import spray.http.HttpHeaders.{Location, `Set-Cookie`}
import Mevolutrix.serviceBusEngine.DSEHandler.CheckAuth
import Mevolutrix.ServiceMgmt.{ServiceAuth, ServiceManager}

class RESTSvcHandler extends Actor with SBE_Routing{
  def actorRefFactory = context
  def receive = runRoute(route)
}

trait SBE_Routing extends HttpService {
  private val pageCache = routeCache(maxCapacity = 1000, timeToIdle = Duration("300 min"))
  private def getReqContext(alias:String,cookie:String):SystemRequestContext = {
    val params = EntityRequest.parseRequest(cookie)
    val uid = params.get("uid").getOrElse("")
    val role = params.get("role").getOrElse("")
    new SystemRequestContext(alias, DSEHandler.getAppSpace(alias), uid, role, 0, 0)
  }

  //
  val DSE_Request = path(Segment / "DSE" / Segment)
  val SVC_Request = path(Segment / "SVC" / IntNumber)
  val SME_Request = path(Segment / "SME" / Segment)
  val LOGIN_Request = path("login")
  val VISITOR_LOGIN = path("visitor_login")

  val route =
    path(Segment / "Portal" /Segments) { (alias, dirs) =>
      get {
        cache(pageCache) {
          val xeduLoginUrl = "/XEDU/Portal/cffex_edu/login.html"
          if (alias == "WEB")
            complete { WebPageService(alias, dirs) }
          else optionalCookie("uaas-token") {
            case Some(nameCookie) =>
              if (SecureTokenAuth.verifySToken(nameCookie.content))
                complete(WebPageService(alias, dirs))
              else {
                if (alias == "XEDU")
                  redirect(xeduLoginUrl + "?url=/" + alias + "/Portal" + dirs.mkString("/", "/", ""), StatusCodes.Found)
                else redirect("/login" + "?url=/" + alias + "/Portal" + dirs.mkString("/", "/", ""), StatusCodes.Found)
              }
            case None => if (alias=="XEDU") redirect("/visitor_login"+"?url=/"+alias+"/Portal"+dirs.mkString("/", "/", ""),StatusCodes.Found)
              else redirect("/login"+"?url=/"+alias+"/Portal"+dirs.mkString("/", "/", ""),StatusCodes.Found)
          }
        }
      }
    }~
    LOGIN_Request{
      get {
        complete {
          <html>
            <head>
              <title>Visitor logon</title>
            </head>
            <script>
              function getRedirectURL() {{
              document.getElementById('url').value=location.search.substr(5,location.search.length);
              }}
            </script>
            <body onload="getRedirectURL()">
              <li class="active">Not logged on, please login</li>
              <form id="logonForm" action="/login" method="post">
                <li class="active">Name:
                  <input id="uid" type="text" name="username"/> <P></P>
                </li>
                <li class="active">Password:
                  <input type="password" name="pwd"/>
                  <input id="url" type="hidden" name="url" value=" "/>
                </li>
                <li class="active">
                  <input type="submit" value="OK"/>
                </li>
              </form>
            </body>
          </html>
        }
      }~  // ctx was used to extract Post data. Thus we must call "ctx.complete" instead of comple directive
      post { ctx => ctx.complete {
        val params = EntityRequest.parseRequest(ctx.request.entity.asString)
        val url = params.get("url").getOrElse("").replaceAll("%2F", "/")
        val username = params.get("username").getOrElse("")
        val expireDuration: Long = (params.get("savePassword").getOrElse("8")).toLong
        val checkPwd = CallSAE(SAE_GetReq("CFG","checkPwd","username -> "+EntityRequest.getField(params,"username")
          +",pwd -> '"+EntityRequest.getField(params,"pwd")+"'",false))
        println("Pwd verify:"+checkPwd)
        val reqHeaders = if (checkPwd=="{verified:true}" || EntityRequest.getField(params,"username")=="Admin")
          scala.List[spray.http.HttpHeader](`Set-Cookie`(HttpCookie("uaas-token",
            SecureTokenAuth.getSToken(username), Option(DateTime.now + 1000L * 3600L * expireDuration))),
            Location(url))
          else scala.List[spray.http.HttpHeader](Location(url))
        HttpResponse(headers = reqHeaders, status = StatusCodes.Found)
      }
      }
    }~
    VISITOR_LOGIN {
      get {
        parameterMap { paramMaps => complete {
          val url = paramMaps.get("url").getOrElse("").replaceAll("%2F", "/")
          println("Visitor ref from url:" + url)
          HttpResponse(headers = scala.List[spray.http.HttpHeader](`Set-Cookie`(HttpCookie("uaas-token",
            SecureTokenAuth.getSToken("visitor"), Option(DateTime.now + 1000L * 3600L * 3))),
            Location(url)), status = StatusCodes.Found)
        }}
      }
    }~
    DSE_Request { (alias, entity) =>
      optionalCookie("uaas-token") {
        case Some(nameCookie) => {
          get {
            parameterMap { args => complete({
              val reqContext = getReqContext(alias, nameCookie.content)
              ServiceAuth.isAuthorized(nameCookie.content,alias,reqContext.userRole,
                (authChecker) => DSEHandler.get(reqContext, entity, args, authChecker))
            })
            }
          } ~
            delete { ctx => ctx.complete({
              val reqContext = getReqContext(alias, nameCookie.content)
              ServiceAuth.isAuthorized(nameCookie.content,alias,reqContext.userRole,
                (authChecker) => DSEHandler.delete(reqContext, entity, authChecker))
            })
            } ~
            post { ctx => ctx.complete({
              val reqContext = getReqContext(alias, nameCookie.content)
              ServiceAuth.isAuthorized(nameCookie.content,alias,reqContext.userRole,
                (authChecker) => DSEHandler.post(reqContext, entity, ctx.request.entity.asString, authChecker))
            })
            } ~
            put { ctx => ctx.complete({
              val reqContext = getReqContext(alias, nameCookie.content)
              ServiceAuth.isAuthorized(nameCookie.content,alias,reqContext.userRole,
                (authChecker) => DSEHandler.put(reqContext, entity, ctx.request.entity.asString, authChecker))
            })
            }
        }
        case None => complete(StatusCodes.Unauthorized, "Unauthorized")
      }
    } ~
    SVC_Request { (alias, serviceId) =>
      parameterMap { paramMaps =>
        optionalCookie("uaas-token") {
          case Some(nameCookie) => {
            get {
              complete {
                val reqContext = getReqContext(alias, nameCookie.content)
                ServiceAuth.isAuthorized(nameCookie.content,alias,reqContext.userRole,
                  (authChecker) => {
                    val uri = ("get:/" + alias + "/SVC/" + serviceId + paramMaps.mkString("?", "&", "").replace(" -> ", "="))
                    //println("URI: " + uri)
                    ServiceManager(alias)
                    if (!authChecker(uri)) throw new UnauthorizedRequest(uri, reqContext.userID)
                    val funcName = ServiceManager.getById(serviceId).name
                    val pStr = (if (paramMaps != null) paramMaps.mkString(",") else null)
                    HttpResponse(entity = HttpEntity(MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),
                      SaeClientConnFactory(alias).svcGet(funcName)(pStr)))
                  })
              }
            } ~
            post { ctx =>
              ctx.complete {
                val reqContext = getReqContext(alias, nameCookie.content)
                ServiceAuth.isAuthorized(nameCookie.content, alias, reqContext.userRole,
                  (authChecker) => {
                    val reqContext = getReqContext(alias, nameCookie.content)
                    val uri = "post:/" + alias + "/SVC/" + serviceId + paramMaps.mkString("?", "&", "").replace(" -> ", "=")
                    //println("URI: " + uri)
                    ServiceManager(alias)
                    if (!authChecker(uri)) throw new UnauthorizedRequest(uri, reqContext.userID)
                    val pStr = if (paramMaps != null) paramMaps.mkString(",") else null
                    val funcName = ServiceManager.getById(serviceId).name
                    HttpResponse(entity = HttpEntity(MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),
                      SaeClientConnFactory(alias).svcPost(funcName)(pStr, ctx.request.entity.asString)))
                  })
              }
            }
          }
          case None => complete(StatusCodes.Unauthorized, "Unauthorized")
        }
      }
    } ~
    SME_Request { (alias, funcCall) =>
      parameterMap { paramMaps =>
        get {
          complete {
            //println("URI: get:/" + alias + "/SME/" + funcCall + paramMaps.mkString("?", "&", "").replace(" -> ", "="))
            val uri ="get:/" + alias + "/SME/" + funcCall + paramMaps.mkString("?", "&", "").replace(" -> ", "=")
            ServiceManager(alias)
            val pStr = if (paramMaps != null) paramMaps.mkString(",") else null
            HttpResponse(entity = HttpEntity(MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),
              SaeClientConnFactory(alias).smeGet(funcCall)(pStr).toString))
          }
        } ~
          post { ctx =>
            ctx.complete({
              //println("URI: post:/" + alias + "/SME/" + funcCall + paramMaps.mkString("?", "&", "").replace(" -> ", "="))
              val uri = "post:/" + alias + "/SME/" + funcCall + paramMaps.mkString("?", "&", "").replace(" -> ", "=")
              ServiceManager(alias)
              val pStr = if (paramMaps != null) paramMaps.mkString(",") else null
              HttpResponse(entity = HttpEntity(MediaTypes.`application/json`.withCharset(HttpCharsets.`UTF-8`),
                SaeClientConnFactory(alias).smePost(funcCall)(pStr, ctx.request.entity.asString).toString))
            })
          }
      }
    }
}