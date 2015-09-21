package mevolutrix
import SBEServiceAccess._
import EntityStore.Communication._
import MySQLStore.operationExecutor._
import mevolutrix.serviceMediator.{SAEReqTaskProcessor, SAEServer}

object ServiceAggregationServer extends App {
  def withExecuteTime(f:() => Unit,prompt:String) = {
    val beforeTime = System.nanoTime()
    f()
    val executeTime = (System.nanoTime() - beforeTime)/1000
    println(prompt+executeTime+"us")
  }

  EntityStoreServer.start(HsTaskProcessor)
  SAEServer.start(SAEReqTaskProcessor)
  printf("Press \"q\" to quit:")
  while ("q" != System.console().readLine()){println("Press \"q\" to quit:")}
  ServiceConfig.appSystem.shutdown()
}
