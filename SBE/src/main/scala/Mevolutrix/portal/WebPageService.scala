package Mevolutrix.portal
import java.io._
import java.util
import Encoding.B64GZIP
import spray.http._
import SBEServiceAccess._
import CSON.CSONDocument
import EntityStore.Client.EntityAccessor
import EntityStore.Metadata.MetadataManager
import HandlerSocket.Protocol.MySQLErrorCodes
import java.util.concurrent.ConcurrentHashMap
import EntityStore.Interface.{SystemRequestContext, ResultFormat}

class WebAppGenerator(appSpace:String) {
  def genResource(path:List[String]):(String,ContentType) = {
    val sb = new StringBuilder()
    sb.append(appSpace).append(path.mkString("/", "/", ""))
    EntityAccessor.loadByKey(sb.toString(),new SystemRequestContext(WebPageService.pageAlias,WebPageService.pageAppSpace,"admin","",0,0),
      WebPageService.pageSetName,WebPageService.pageSchema) match {
      case ret:util.ArrayList[CSONDocument] =>
        val items = ResultFormat.getCSONResult(ret,WebPageService.pageSchema)
        if (items.length>0) {
          (B64GZIP.decodeGZIP(items(0).getValue("data").asInstanceOf[String]),
            items(0).getValue("MediaType") match {
              case 0 => MediaTypes.`text/html`
              case 1 => MediaTypes.`text/css`
              case 2 => MediaTypes.`application/javascript`
              case 3 => MediaTypes.`image/x-icon`
              case 4 => MediaTypes.`image/gif`
              case 5 => MediaTypes.`image/jpeg`
              case 6 => MediaTypes.`application/xml`
              case 7 => MediaTypes.`application/xhtml+xml`
              case 8 => MediaTypes.`image/png`
              case v@_ => println("Web page generator encountered typeCode:"+v)
                MediaTypes.`text/html`
            })
        }
        else ("Resource not found:"+sb.toString(),MediaTypes.`text/html`)
      case error:(Int,Long) => 
        ("Load resource error:"+MySQLErrorCodes(error._2),MediaTypes.`text/html`)
    }
  }
}

object WebPageService {
  private val baseDir:String = ServiceConfig.sbePortalRoot
  private val pageGenCache = new ConcurrentHashMap[String,WebAppGenerator]()
  private def getPageGenerator(alias:String) = Option(pageGenCache.
    putIfAbsent(alias,new WebAppGenerator(alias))).getOrElse(pageGenCache.get(alias))
  val pageAlias = "MDE"
  val pageAppSpace = "System.Metadata"
  val pageSetName = "System.Metadata.ReleasePageResource"
  lazy val pageSchema = MetadataManager.getSchema(pageSetName)
  def apply(appSpace:String,path:List[String]): HttpResponse = {
      appSpace match {
        case "WEB" => try {
          val resourcePath: String = path.mkString("/", "/", "")
          loadResFromFile(resourcePath, getContentType(resourcePath))
        } catch {
          case e: Exception =>
            HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, e.getMessage))
        }
        case _ =>
          val webResourceGenerator = getPageGenerator(appSpace)
          val resource = webResourceGenerator.genResource(path)
          HttpResponse(entity = HttpEntity(resource._2, resource._1))
      }
  }
  private def getContentType(typeName:String):ContentType = ext2ContentType(getFileExt(typeName))
  private def getFileExt(fileName:String):String =
    fileName.substring(fileName.lastIndexOf(".")+1,fileName.length)
  def ext2ContentType(extName:String):ContentType = extName match {
      case "html"|"htm" => MediaTypes.`text/html`
      case "css" => MediaTypes.`text/css`
      case "js"|"coffee" => MediaTypes.`application/javascript`
      case "mp4"|"mp4v"|"mpg4" => MediaTypes.`video/mp4`
      case "ico" =>  MediaTypes.`image/x-icon`
      case "gif" =>  MediaTypes.`image/gif`
      case "jpeg"|"jpg"|"jpe" =>  MediaTypes.`image/jpeg`
      case "mpga"|"mp2"|"mp2a"|"mp3"|"m2a"|"m3a" =>  MediaTypes.`audio/mpeg`
      case "xml"|"xsl" => MediaTypes.`application/xml`
      case "xhtml"|"xht" => MediaTypes.`application/xhtml+xml`
      case "png" => MediaTypes.`image/png`
      case _ => MediaTypes.`application/xhtml+xml`
    }
  private def loadResFromFile(filePath:String,contType:ContentType):HttpResponse = {
    val fHandle = new File(if(filePath.charAt(0)=='/') baseDir + filePath else baseDir +  "/"+filePath)
    if(fHandle.exists()) {
      val file = new FileInputStream(fHandle)
      var binary = new Array[Byte](file.available())
      file.read(binary)
      file.close()
      HttpResponse(entity = HttpEntity(contType,binary))
    } else throw new Exception("file:"+fHandle.getName+" doesn't exist.")
  }
}
