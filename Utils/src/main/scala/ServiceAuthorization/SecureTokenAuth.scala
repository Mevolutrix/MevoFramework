package ServiceAuthorization

import Encoding.B64GZIP

class UnauthorizedRequest(uri:String,uid:String) extends Exception{

}
/**
 * Control and verify SecurityToken
 */
object SecureTokenAuth {
  private def getShaOne(valStr:String):String = {
    val valToHash = new StringBuilder()
    valToHash.append(valStr).append("hash='mevo'")
    val md = java.security.MessageDigest.getInstance("SHA-1")
    new String(B64GZIP.b64Encoding(md.digest(valToHash.result().getBytes("ISO-8859-1"))),"ISO-8859-1")
  }
  def getSToken(username:String):String = {
    val sb = new StringBuilder()
    sb.append("uid='").append(username).append("'&")
    sb.append("role='user'").append("&")
    val val2Compute = sb.result()
    sb.append("SHA-1:'").append(getShaOne(val2Compute)).append("'")
    //println("ST B64:"+new String(B64GZIP.b64Encoding(sb.result().getBytes()),"ISO-8859-1"))
    sb.result()
  }

  def verifySToken(token:String):Boolean = {
    val strPair = token.split("SHA-1:")
    if (strPair.length<2) {
      println("Verify cookie incorrect with no SHA-1 hash value."+token)
      false
    }else {
      val strToVerify = new StringBuilder()
      strToVerify.append("'").append(getShaOne(strPair(0))).append("'")
      val ret = strToVerify.result()==strPair(1)
      if (!ret) println("Unmatched SHA-1: cookie SHA-1->"+strPair(1)+" |got SHA-1->"+strToVerify.result())
      ret
    }
  }
  def verifyPwd(pwdInMD5:String,pwdHash:String):Boolean = {
    //val verifyPwdSha1 = getShaOne(new String(B64GZIP.b64Decoding(pwdInBase64),"ISO-8859-1"))
    val verifyPwdSha1 = pwdInMD5
      verifyPwdSha1 == pwdHash
  }
  def getPwdHashVal(pwdInBase64:String) = getShaOne(new String(B64GZIP.b64Decoding(pwdInBase64),"ISO-8859-1"))
}
