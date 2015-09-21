package MevoClassLoader
import java.io.{ByteArrayOutputStream, File, FileInputStream}
import java.lang.reflect.Method
import java.util.jar.{JarEntry, JarFile}

/**
 * customized class loader for loading SME/SVC modules
 */
class ByteArrayClassLoader extends ClassLoader{
  // Follow JNDI "hack" to get the app level ClassLoader (破坏双亲委派模型)
  // 因为我们就是需要把外部class加载到当前应用Class Tree中
  private val cloader:ClassLoader = Thread.currentThread().getContextClassLoader
  // "defineClass" was Protected in System ClassLoader, hack it with reflection
  // to break through and set it "accessible"
  private val defineClassMd:Method = classOf[ClassLoader].getDeclaredMethod("defineClass",
    classOf[String],classOf[Array[Byte]],classOf[Int],classOf[Int])
  defineClassMd.setAccessible(true)
  def loadClassFromJar(filePath:String,objName:String) = {
    val jarFile = new JarFile(filePath)
    def readDataFromEntry(entry:JarEntry) = {
      val is = jarFile.getInputStream(entry)
      val outData = new ByteArrayOutputStream()
      val buffer = new Array[Byte](20480)
      var readLength = 0
      while (readLength != -1) {
        readLength = is.read(buffer)
        if (readLength>0) outData.write(buffer,0,readLength)
      }
      if (outData.size()>0) outData.toByteArray
      else null
    }
    def className(classFileName:String) =
      classFileName.substring(0,classFileName.length-6).replaceAll("/",".")

    val jarEntries = jarFile.entries()
    var ret:Class[_] = null
    while(jarEntries.hasMoreElements){
      val item = jarEntries.nextElement()
      if (item.getName().endsWith(".class")) {
        val loadClassName = className(item.getName())
        println("Load Jar:"+loadClassName)
        if (loadClassName == objName) ret = loadClass(loadClassName, readDataFromEntry(item))
        else loadClass(loadClassName, readDataFromEntry(item))
      }
    }
    ret
  }
  def loadClassFromFile(filePath:String,objName:String):Class[_] = {
    if (filePath.endsWith(".class")) {
      val file = new FileInputStream(new File(filePath))
      var binary = new Array[Byte](file.available())
      file.read(binary)
      loadClass(objName, binary)
    }else loadClassFromJar(filePath,objName)
  }
  def loadClass(objName:String,bytes:Array[Byte]):Class[_] =
    defineClassMd.invoke(cloader,objName,bytes,0.asInstanceOf[Object],
      bytes.length.asInstanceOf[Object]).asInstanceOf[Class[_]]
}
