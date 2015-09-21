package mevolutrix.Interface
import akka.actor.ActorRef
import akka.pattern._
import akka.util._
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class SME_PostRequest(funcName:String,params:Map[String,String],postData:String)
case class SME_GetRequest(funcName:String,params:Map[String,String])
/**
 * abstract interface for SME service actor
 */
trait SMEService {
  implicit val timeout:Timeout = Timeout(15.seconds)
  val serviceActor:ActorRef = null
  def post(funcName:String)(params:Map[String,String],data:String) = {
    val future = serviceActor.ask(SME_PostRequest(funcName, params, data))
    Await.result(future, Duration("30s"))
  }
  def get(funcName:String)(params:Map[String,String]) = {
    val future = serviceActor.ask(SME_GetRequest(funcName, params))
    Await.result(future, Duration("30s"))
  }
}
