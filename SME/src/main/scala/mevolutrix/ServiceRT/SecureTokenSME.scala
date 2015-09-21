package mevolutrix.ServiceRT
import java.util
import Encoding.B64GZIP
import CSON.CSONDocument
import ExpressionParser.EntityRequest
import mevolutrix.Interface.ISmeHandler
import EntityStore.Client.EntityAccessor
import EntityStore.Interface._
import EntityStore.Metadata.MetadataManager
import ServiceAuthorization.SecureTokenAuth

/**
 * SecureToken Verification SME Service handler
 */
class SecureTokenSME extends ISmeHandler {
  val userInfoSchemaName = SVCConfig.systemAppSpace+".UserInfo"
  val userSchemaInfo = MetadataManager.getSchema(userInfoSchemaName)
  val vcodeInfoSchemaName = SVCConfig.systemAppSpace+".VerificationCode"
  val vcodeSchemaInfo = MetadataManager.getSchema(vcodeInfoSchemaName)
  private def getvCode(uid:Object):String = EntityAccessor.loadByKey(uid,SVCConfig.cfgReqContext,
    vcodeInfoSchemaName,vcodeSchemaInfo) match {
    case retList: util.List[CSONDocument] =>
      val csonList = ResultFormat.getCSONResult(retList, userSchemaInfo)
      if (csonList.length>0) csonList(0).getValue("vcode").asInstanceOf[String]
      else null
    case _ => null
  }
  private def getUserInfo(uid:Object) = EntityAccessor.loadByKey(uid,SVCConfig.cfgReqContext,
    userInfoSchemaName,userSchemaInfo) match {
    case retList: util.List[CSONDocument] =>
      val csonList = ResultFormat.getCSONResult(retList, userSchemaInfo)
      if (csonList.length>0) csonList(0)
      else null
    case _ => null
  }

  def getRequest(funcName:String,params:Map[String,String]):String = {
    funcName match {
      case "verifySToken" =>
        val token = new String(B64GZIP.b64Decoding(EntityRequest.getFieldValue(params,"token")
          .asInstanceOf[String]),"ISO-8859-1")
        "{verified:"+SecureTokenAuth.verifySToken(token).toString +"}"
      case "checkPwd" =>
        val uid = EntityRequest.getFieldValue(params,"username")
        val pwd = EntityRequest.getFieldValue(params,"pwd").asInstanceOf[String]
        val userInfo = getUserInfo(uid)
        val verified = if (userInfo!=null) {
          val retryTimes = userInfo.getValue("tryTimes").asInstanceOf[Integer]
          val isBlock = userInfo.getValue("is_block").asInstanceOf[Boolean]
          if (isBlock) false // User not activated
          else if (retryTimes<3)
            SecureTokenAuth.verifyPwd(pwd, userInfo.getValue("pwd").asInstanceOf[String])
          else {/* Do verification code check */
            val vCode = params.get("vcode").getOrElse("").replace("'","")
            val ret = if (vCode=="") false else (getvCode(uid)==vCode)
            if (ret) {
              userInfo.setValue("tryTimes",Integer.valueOf(0))
              val bin = userInfo.getBytes()
              EntityAccessor.update(SVCConfig.cfgReqContext,userInfoSchemaName,uid,bin,userSchemaInfo)
              SecureTokenAuth.verifyPwd(pwd, userInfo.getValue("pwd").asInstanceOf[String])
            } else false
          }
        }
        else false

        "{verified:"+(if (userInfo!=null && !verified) {
          val retryTimes = userInfo.getValue("tryTimes").asInstanceOf[Integer]
          val isBlock = userInfo.getValue("is_block").asInstanceOf[Boolean]
          // call DSE to update userInfo and set block
          if (isBlock) "3" else if (retryTimes<3) {
            userInfo.setValue("tryTimes",Integer.valueOf(retryTimes.toInt+1))
            val bin = userInfo.getBytes()
            EntityAccessor.update(SVCConfig.cfgReqContext,userInfoSchemaName,uid,bin,userSchemaInfo)
            "0"
          } else "2"  // Need verification code
        } else {
          if (userInfo!=null) "1"
          else "0"
        })+"}"  // 0: means false, 1: true, 2: need verification Code
    }
  }
  def postRequest(funcName:String,params:Map[String,String],jsonBody:String):String = {
    ""
  }
}
