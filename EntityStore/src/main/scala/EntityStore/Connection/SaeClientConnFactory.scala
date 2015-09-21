package EntityStore.Connection
import EntityStore.Interface.ReqSendMode
import EntityStore.Interface.ReqSendMode.ReqSendMode
import EntityStore.Metadata.SBEMetadata
import akka.actor.ActorRef
import mevolutrix.Interface.SAEServiceAccessor

object SaeClientConnFactory {
  def apply(alias:String,reqMode:ReqSendMode = ReqSendMode.syncMode,requester:ActorRef=null):SAEServiceAccessor = {
    class SAEServiceConn(alias:String,reqMode:ReqSendMode,requester:ActorRef) extends SAEServiceAccessor{
      override val appSpace:String = SBEMetadata.getAppSpace(alias).appSpaceName
      override val sa = SAEInstances(appSpace).session(0,requestMode = reqMode,sender = requester)
    }
    new SAEServiceConn(alias,reqMode,requester)
  }
}
