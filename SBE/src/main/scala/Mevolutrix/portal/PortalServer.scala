package Mevolutrix.portal
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import akka.routing.RoundRobinPool
import java.util.concurrent.TimeUnit
import SBEServiceAccess.ServiceConfig
import akka.actor.{ActorLogging, Props, Actor, ActorRef}

object PortalServer {
  private var httpListener : ActorRef = null
  private var service : ActorRef = null
  private var starter : ActorRef = null
  case class StartHttpServer(serviceInterface:String,servicePort:Int)
  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  class HttpServiceStarter extends Actor with ActorLogging {
    private var parentActor:ActorRef = null
    def receive = {
      case StartHttpServer(serviceInterface,servicePort) =>
        parentActor = sender()
        IO(Http)(ServiceConfig.appSystem) ! Http.Bind(service,
          interface = serviceInterface, port = servicePort)
      case bound:akka.io.Tcp.Bound =>
        httpListener = sender()
        parentActor ! sender()
      case "stop" =>
        parentActor = sender()
        httpListener ! Http.Unbind
      case Http.Unbound => parentActor ! Http.Unbound
    }
  }
  def start(restHandler:Class[_],serviceInterface:String="0.0.0.0",servicePort:Int=8080) = {
    service = ServiceConfig.appSystem.actorOf(Props(restHandler).
      withRouter(RoundRobinPool(nrOfInstances = 16)))
    starter = ServiceConfig.appSystem.actorOf(Props(new HttpServiceStarter()))
    val future = starter ? StartHttpServer(serviceInterface,servicePort)
    httpListener = Await.result(future,timeout.duration).asInstanceOf[ActorRef]
  }
  def stop = {
    val future = starter ? "stop"
      Await.result(future, timeout.duration)
      ServiceConfig.appSystem.stop(starter)
      ServiceConfig.appSystem.stop(service)
  }
}
