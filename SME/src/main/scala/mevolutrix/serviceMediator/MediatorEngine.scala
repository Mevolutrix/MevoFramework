package mevolutrix.serviceMediator
import java.util.concurrent.{TimeUnit, ConcurrentHashMap}
import SBEServiceAccess.ServiceConfig
import mevolutrix.Interface.SMEService
import akka.util.Timeout
import mevolutrix.engineConfig.SMEConfig

import scala.concurrent.Await

object MediatorEngine {
  val REQUEST_FINISH_TIMEOUT = 30000
  private val serviceCache = new ConcurrentHashMap[String,SMEService]()
  def apply(svcObjName:String):SMEService = {
    val ret = serviceCache.get(svcObjName)
    if (ret==null) {
      val smeConfig = SMEConfig(svcObjName)
      if (smeConfig!=null) {
        val smeSvc = new SMEService {
          override val serviceActor = getSvcActor(smeConfig.actorPath)
        }
        serviceCache.put(svcObjName, smeSvc)
        smeSvc
      } else new SMEService {
          override def get(funcName:String)(params:Map[String,String]) =
          "You called SME service not exist!"
        }
    }else ret
  }
  def getSvcActor(actorPath:String) = {
    implicit val timeout = Timeout(REQUEST_FINISH_TIMEOUT, TimeUnit.SECONDS)
    Await.result(ServiceConfig.appSystem.actorSelection(actorPath).
      resolveOne, timeout.duration)
  }
}
