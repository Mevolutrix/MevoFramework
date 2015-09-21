package Mevolutrix
import Mevolutrix.portal.{RESTSvcHandler, PortalServer}

object ServiceBusEngineServer extends App {
  implicit val system = SBEServiceAccess.ServiceConfig.appSystem
  PortalServer.start(classOf[RESTSvcHandler])
  println("Press \"q\" to quit:")
  var s: String = " "
  do {
    s = io.StdIn.readLine()
    s match {
      case "q" => PortalServer.stop; system.shutdown()
      case _ => println("Press \"q\" to quit:")
    }
  }while (s!="q")
}
