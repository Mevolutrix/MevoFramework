package Metadata.Test
import org.scalatest.FunSuite

/**
 * Test set data coverage
 */
class TestSet extends FunSuite {
 /* test("Metadata operation test:") {
    implicit val system = SBEServiceAccess.ServiceConfig.appSystem
    val testProxy = TestProbe()
    val mdeSME = system.actorOf(Props[MetaDataSME])
    val alias = "MDE"
    val setName = AutoValueKey.autoKeySetInfo.setName
    val postData = JSONSerializer(AutoValueKey.autoKeySetInfo)
    val params = Map[String, String]("setName" -> "System.Configuration.AutoValueKey")
    testProxy.send(mdeSME, SME_PostRequest("createSet", params, postData))
    testProxy.expectMsg(true)
    system.shutdown()
  }*/
}
