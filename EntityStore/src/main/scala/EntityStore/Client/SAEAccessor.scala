package EntityStore.Client

import EntityStore.Connection.SaeClientConnFactory
import EntityStore.Interface.ReqSendMode
import SBEServiceAccess.ServiceConfig
import akka.actor.{Actor, ActorLogging, Props}
import akka.routing.RoundRobinPool
import mevolutrix.Interface.{SAE_GetReq, SAE_PostReq}

object SAEAccessor {
  val accessorPoolSize = ServiceConfig.conf.getInt("mevo.sae.accessorPool.size")
  val storeClient = ServiceConfig.appSystem.actorOf(Props(new SAEAccessActor()).
    withRouter(RoundRobinPool(nrOfInstances = accessorPoolSize)),"SAEAccessor")

}
class SAEAccessActor extends Actor with ActorLogging {
  type Binary = Array[Byte]

  override def postRestart(thr: Throwable): Unit = {
    log.info("Actor recovery. stop reason:" + thr)
  }

  def receive = {
    case SAE_GetReq(alias,funcName,params,isSvc) =>
      log.info("SAE Get: alias:"+alias+" |funcName:"+funcName+" |params:"+params+"sender:"+sender())
      val accessor = SaeClientConnFactory(alias,ReqSendMode.forward,sender())
      if (isSvc) accessor.svcGet(funcName,params)
      else accessor.smeGet(funcName,params)
    case SAE_PostReq(alias,funcName,params,data,isSvc) =>
      log.info("SAE Post: alias:"+alias+" |funcName:"+funcName+" |params:"+params+" |data:"+data)
      val accessor = SaeClientConnFactory(alias,ReqSendMode.forward,sender())
      if (isSvc) accessor.svcPost(funcName,params,data)
      else accessor.smePost(funcName,params,data)
  }
}
