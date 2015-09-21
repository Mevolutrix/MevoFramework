package Encoding
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Base64
import java.util.zip.{GZIPOutputStream, GZIPInputStream}

import scala.xml.dtd.PublicID

/**
 * Do BASE 64 encoding/decoding and GZIP compress/unzip
 */
object B64GZIP {
  def b64Decoding(v:String) = {
    val base64Decoder = Base64.getDecoder
    val ret =  base64Decoder.decode(v.getBytes("ISO-8859-1"))
    ret
  }
  def gzip(data:Array[Byte])= {
    val outStream = new ByteArrayOutputStream()
    val compressor = new GZIPOutputStream(outStream)
    compressor.write(data,0,data.length)
    compressor.finish()
    compressor.flush()
    outStream.toByteArray
  }
  def b64Encoding(data:Array[Byte]) = {
    val base64Encoder = Base64.getEncoder
    base64Encoder.encode(data)
  }
  def ungzip(data:Array[Byte]) = {
    val buf = new Array[Byte](8192)
    val outBufStream = new ByteArrayOutputStream()
    val zipBinary = new ByteArrayInputStream(data)
    val uncompressor = new GZIPInputStream(zipBinary)
    var length: Int = 0
    try {
      do {
        length = uncompressor.read(buf)
        if (length > 0) outBufStream.write(buf, 0, length)
      } while (length > 0)
    } catch {
      case e: Exception =>
        ("Error: decode the page Generated encountered gzip fault." +e).getBytes("UTF-8")
    }
    outBufStream.toByteArray
  }

  def encodeGZIP(s:String):String = encodeGZIP(s.getBytes("UTF-8"))
  def encodeGZIP(data:Array[Byte]):String = new String(b64Encoding(gzip(data)),"ISO-8859-1")
  def decodeGZIP(s:String):String = new String(ungzip(b64Decoding(s)),"UTF-8")
}
