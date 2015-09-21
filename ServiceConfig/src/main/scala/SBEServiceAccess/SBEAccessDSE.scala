package SBEServiceAccess
import java.util
import java.util.concurrent.TimeUnit
import CSON.CSONDocument
import EntityAccess.{GeneralEntitySchema, GeneralEntityToCSON}
import EntityStore.Interface.{ResultFormat, StoreOperation}
import akka.actor._
import akka.pattern._
import akka.util.Timeout
import org.slf4j.LoggerFactory
import scala.concurrent.Await
import com.typesafe.config.ConfigFactory

object ServiceConfig {
  val nameOfActorSystem = "mevolutrix"
  val nameOfEGAccessor = "/user/DSEAccessor" //"akka://"+nameOfActorSystem+
  val nameOfSAEAccessor = "/user/SAEAccessor"
  val appSystem = ActorSystem(nameOfActorSystem)
  val conf = ConfigFactory.load()
  val sbePortalRoot = conf.getString("sbe.webroot")
  val initEAccessor = Class.forName("EntityStore.Client.EntityAccessor$")
  val initSAEAccessor = Class.forName("EntityStore.Client.SAEAccessor$")
  //val initMDE = Class.forName("EntityStore.Metadata.MetaDataSME$")
}

object CallDSE {
//  val REQUEST_FINISH_TIMEOUT = 30000
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  lazy val storeAccessor = Await.result(ServiceConfig.appSystem.
    actorSelection(ServiceConfig.nameOfEGAccessor).resolveOne,timeout.duration)
  def apply(op:StoreOperation,asyncCall:Boolean=false):AnyRef = {
    if (asyncCall) {
      storeAccessor ! op
      null
    }
    else {
      val future = storeAccessor.ask(op)//(REQUEST_FINISH_TIMEOUT)
      Await.result(future, timeout.duration).asInstanceOf[AnyRef]
    }
  }
  def apply(op:StoreOperation,typeInfo:Class[_]):AnyRef = {
    val future = storeAccessor.ask(op)//(REQUEST_FINISH_TIMEOUT)
    val serializer = GeneralEntityToCSON(typeInfo)
    val schema = GeneralEntitySchema(typeInfo)
    val retList = ResultFormat.getCSONResult(Await.result(future, timeout.duration).
      asInstanceOf[util.ArrayList[CSONDocument]],schema)
    if (retList.size>0) serializer.getObject(retList(0))
    else null
  }
}
object CallSAE {
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  val log = LoggerFactory.getLogger("SAE_Accessor")
  lazy val saeAccessor = Await.result(ServiceConfig.appSystem.
    actorSelection(ServiceConfig.nameOfSAEAccessor).resolveOne,timeout.duration)
  def apply(op:Object,asyncCall:Boolean=false):AnyRef = {
    if (asyncCall) {
      saeAccessor ! op
      null
    }
    else {
      val future = saeAccessor.ask(op)//(REQUEST_FINISH_TIMEOUT)
      try {
        Await.result(future, timeout.duration).asInstanceOf[AnyRef] match {
          case retList:util.ArrayList[CSONDocument] =>
            val jsonList = ResultFormat.getJsonResult(retList)
            if (jsonList.length>0) jsonList(0)
            else null
          case ret@_ => ret
        }
      } catch {
        case e:java.util.concurrent.TimeoutException =>
          log.error("SAE req:"+op+" timeout.")
          null
        case e:Exception => throw e
      }
    }
  }
  def apply(op:Object,typeInfo:Class[_]):AnyRef = {
    val future = saeAccessor.ask(op)//(REQUEST_FINISH_TIMEOUT)
    val serializer = GeneralEntityToCSON(typeInfo)
    val schema = GeneralEntitySchema(typeInfo)
    try {
      val retList = ResultFormat.getCSONResult(Await.result(future, timeout.duration).
        asInstanceOf[util.ArrayList[CSONDocument]], schema)
      val ret = if (retList.size > 0) serializer.getObject(retList(0))
      else null
      ret
    } catch {
      case e:java.util.concurrent.TimeoutException => log.error("SAE req:"+op+" timeout.")
        null
      case e:Exception => throw e
    }
  }
}