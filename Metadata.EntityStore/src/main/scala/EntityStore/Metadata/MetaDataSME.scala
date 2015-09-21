package EntityStore.Metadata

import EntityAccess.JSONSerializer
import EntityInterface._
import mevolutrix.Interface.{SME_GetRequest, SME_PostRequest}
import ExpressionParser.EntityRequest
import SBEServiceAccess.ServiceConfig
import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.RoundRobinPool

class MetaDataSME extends Actor with ActorLogging {
  def receive = {
    case SME_PostRequest("createSet",params,postData) =>
      //println("Metadata SME createSet:"+params)
      sender() ! MetadataManager.createSet(EntityRequest.getFieldValue(params,"setName").asInstanceOf[String],
        JSONSerializer.unapply(postData,classOf[EntitySet]).asInstanceOf[EntitySet])
    case SME_PostRequest("updateSchema",params,postData) =>
      //println("Metadata SME updateSchema:"+params)
      MetadataManager.updateSchema((EntityRequest.getFieldValue(params,"id").asInstanceOf[String],
        EntityType.withName(EntityRequest.getFieldValue(params,"type").asInstanceOf[String])),
        JSONSerializer.unapply(postData,classOf[EntitySchema]).asInstanceOf[EntitySchema])
    case SME_PostRequest("updateSet",params,postData) =>
      //println("Metadata SME updateSet:"+params)
      sender() ! MetadataManager.updateSet(EntityRequest.getFieldValue(params,"setName").asInstanceOf[String],
        JSONSerializer.unapply(postData,classOf[EntitySet]).asInstanceOf[EntitySet])
    case SME_GetRequest("deploySets",params) =>
      val appSpace = EntityRequest.getFieldValue(params,"appSpace").asInstanceOf[String]
      //println("Deploy sets within appSpace:"+appSpace)
      sender() ! MetadataManager.deploySets(appSpace)
    case _ => sender() ! (-1,0L)
  }
}
object MetaDataSME {
  val storeClient = ServiceConfig.appSystem.actorOf(Props(new MetaDataSME()).
    withRouter(RoundRobinPool(nrOfInstances = 4)), "MetadataSME")
}
