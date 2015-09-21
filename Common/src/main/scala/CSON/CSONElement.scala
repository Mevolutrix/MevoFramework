package CSON
import CSON.Types._
import java.util.Date
import EntityInterface._
import scala.language.dynamics
import java.nio.{ByteOrder, ByteBuffer}

class CSONElement(typeCode:Byte) extends IElementValue {
  import CSONTypes._
  private val thisElementType = CSONTypesArray.CSONElementTypes(typeCode)
  private var value : Any = null
  def this(typeCode:Byte, cur:CSONCursor,index:Int) {
    this(typeCode)
    val tc = CSONTypes(typeCode)
    if (tc!=NullValue && tc!=NullElement)
      // read element Value from CSON raw buffer
      try {
        value = thisElementType.getValue(cur, index)
      } catch {
        case e:NullPointerException => throw new IllegalArgumentException("type "+tc.toString+" got null elementType")
        case a: Throwable => throw a
      }
  }
  def this (typeCode:Byte, v:Any) { this(typeCode); setValue(v) }
  override def add(that: IElementValue)= { value = thisElementType.addValue(value,that.getValue) }
  override def elementType : IElementType = thisElementType
  override def compare(that:IElementValue):Int = thisElementType.compare(value,that.getValue).asInstanceOf[Int]
  override def setValue(newValue:Any):Unit = {
    if (newValue.isInstanceOf[CSONElement])
      value = newValue.asInstanceOf[CSONElement].getValue
    else // Timestamp type element is a long value store the Unix ticket
      value = if (typeCode == CSONTypes.Timestamp.id.toByte && newValue.isInstanceOf[Date])
      // Timestamp type element is a long value store the Unix ticket
        newValue.asInstanceOf[Date].getTime()
      else newValue

  }
  override def getValue() = value
}

object CSONElement {
  val NullElement = new CSONElement(CSONTypes.NullElement.id.toByte)
  val NullValue = new CSONElement(CSONTypes.NullValue.id.toByte)
  /**
   * Static function to new (read value from dataBuffer pointed by cur(parent object(Complex or Array)) and index(point to the element)) 
   */
  def getElement(cur:CSONCursor,index:Int):CSONElement = {
  val elementTC = cur.reader.ReadElementType(cur, index)
  if (elementTC==CSONTypes.NullValue.id) CSONElement.NullValue
  else if (elementTC==CSONTypes.NullElement.id) CSONElement.NullElement
  else new CSONElement(elementTC,cur,index)
  }
  def getIndex(propertyIndex:Any):Int = propertyIndex match {
                      case i:Int => i
                    case s:String => s.toInt
                    case _ => throw new IllegalArgumentException
                  }
  def getSchema(schemaType:Any):IEntitySchema = schemaType match {
      case entitySchema: IEntitySchema => entitySchema
      case complexType: CSONComplexType => complexType.schema
      case arrayType: CSONArrayType => arrayType.elementSchema
      case _ => null
  }
  /**
   * Used by CSONArrayType constructor. If this array should contain a simple type element, the caller must send CSONSimpleType 
   */
  def getElementType(schemaOrType:Any):Option[CSONSimpleType] = schemaOrType match {
    case schemaOrType:CSONSimpleType=> Some(schemaOrType)
    case _ => None
  }
  def getElementTypeCode(schemaOrType:Any):Byte = getElementType(schemaOrType).getOrElse(CSONTypesArray.NullType).getTypeCode
  
  def getComplexType(schemaType:Any):CSONComplexType = {
    if (schemaType.isInstanceOf[IEntitySchema]) new CSONComplexType(schemaType.asInstanceOf[IEntitySchema])
    else schemaType.asInstanceOf[CSONComplexType]
  }
  def getArrayType(schemaType:Any):CSONArrayType = {
    if (schemaType.isInstanceOf[IEntitySchema]) new CSONArrayType(schemaType.asInstanceOf[IEntitySchema])
    else if (schemaType.isInstanceOf[CSONArrayType]) schemaType.asInstanceOf[CSONArrayType]
    else new CSONArrayType(null)
  }
}

class CSONComplexElement(schemaOrType:Any,rawData:Option[CSONCursor]) extends Dynamic
  with IEntityRandomAccess with IEntitySequenceAccess  with IElementValue {
  private var thisSchema : IEntitySchema = CSONElement.getSchema(schemaOrType)
  private var thisElementType : CSONComplexType = null
  private var cursor : CSONCursor = null
  private var keyValueIterator : Iterator[Tuple2[String,IElementValue]] = _
    // If this schema(this "standard" schema may only be the parent class's schema) has defined "GetChildSchema" function
    // Try to get the real child element Schema by let this function read data in cursor
    if ( thisSchema.getChildSchema != null && !(rawData.isEmpty) ) {
      thisSchema = thisSchema.getChildSchema(rawData.get)
      thisElementType = new CSONComplexType(thisSchema)
    }
    else thisElementType = CSONElement.getComplexType(schemaOrType)
  // This must be initiated after get the "real" schema
  private val elementList = new Array[IElementValue](thisSchema.count)
    if (rawData.isEmpty) for {i <- 0 until thisSchema.count} elementList(i) = null
    else cursor = rawData.get
    
  private def getIndex(propertyIndex:Any):Int = propertyIndex match {
                      case i:Int => i
                    case s:String => if (s.charAt(0).isDigit)s.toInt else thisSchema.getID(s)
                    case o:Any => throw new IllegalArgumentException
    }
  private def readElement(cur:CSONCursor,index:Int):IElementValue = {
    val elementTypeCode = cur.reader.ReadElementType(cur, index)

    CSONTypes(elementTypeCode) match {
      case CSONTypes.Array => new CSONElementArray(thisSchema.getElementType(index), cur.reader.GetChildElementCursor(cur, index));
      case CSONTypes.EmbeddedDocument => new CSONComplexElement(thisSchema.getElementType(index),cur.reader.GetChildElementCursor(cur, index));
      case _ => CSONElement.getElement(cur, index)
    }
  }

    // Complex element use lazy load strategy for accessing item inside of CSON buffer
    // The apply function will read element from CSON buffer and store item in elementList array
  def apply(index:Any):IElementValue = {
    val i = getIndex(index)
    if (i<thisSchema.count) {
      if (elementList(i) == null)
        if (cursor!=null) elementList(i) = readElement(cursor,i)
        else elementList(i) = CSONElement.NullValue
      elementList(i)
    }
    else throw new IllegalArgumentException
  }

  override def add(that: IElementValue) = throw new Exception("Can't do add operation on Complexe Element Object")
  override def elementType : IElementType = thisElementType
  override def compare(that:IElementValue):Int = throw new Exception("Illegal comparation!")
  override def setValue(newValue:Any) = {
    if (newValue.isInstanceOf[CSONComplexElement]) {
      val srcComplexElement = newValue.asInstanceOf[CSONComplexElement]
      for (i <- 0 until this.length) setValue(i, srcComplexElement(i))
    }
    else throw new Exception("Can't copy from a none ComplexElement object.")
  }
  override def getValue() = this

  override def getSchema : IEntitySchema = thisSchema
  override def getValue(index:Any): Any = apply(index).getValue
  override def getElement(index:Any):IElementValue = apply(index)

  override def getRawValue(index:Any,NeedTypeCode:Boolean=true):Array[Byte] = {
    val outBuffer = ByteBuffer.wrap(new Array[Byte](4096)).order(ByteOrder.LITTLE_ENDIAN)
    val childElement = getElement(index)
    val len = childElement.elementType.getRawValue(outBuffer, childElement.getValue, NeedTypeCode).flip().limit()
    val ret = new Array[Byte](len)
    outBuffer.get(ret)
    ret
  }
  override def setValue(index:Any, value:AnyRef) = {
    val i = getIndex(index)
    if (value.isInstanceOf[IElementValue]) elementList(i) = value.asInstanceOf[IElementValue]
    else {
      val item = elementList(i)
      if (item == null || item == CSONElement.NullValue) elementList(i) = new CSONElement(thisSchema.getTypeCode(i), value)
      else item.setValue(value)
    }
  }
  private var fullLoaded = false
    // As this complex structure will be lazy loaded. It must be performed a full load from buffer
  override def iterator : Iterator[IElementValue] = {
    if (!fullLoaded) {
      for (i <- 0 until length) apply(i);
      fullLoaded = true
    }

    elementList.iterator
  }
  def mapIterator = {
    iterator
    for(i<-0 until thisSchema.count) yield (thisSchema.getPropertyName(i),elementList(i))
  }
  override def append(value:Any) : Boolean = false
  override def length : Int = elementList.length
  def selectDynamic(index: String) {apply(index).getValue}
  def updateDynamic(index: String)(args: Any){
    val i = getIndex(index)
    if (args.isInstanceOf[IElementValue]) elementList(i) = args.asInstanceOf[IElementValue]
    else {
      val item = elementList(i)
      if (item == null || item == CSONElement.NullValue) elementList(i) = new CSONElement(thisSchema.getTypeCode(i), args)
      else item.setValue(args)
    }
  }
}
/**
 * CSON array. The input can be schema or CSONSimpleType of array element(If this array just contain element like int, Array[Byte]
 */
class CSONElementArray(elementSchemaOrType:Any,rawData:Option[CSONCursor]) 
             extends IElementValue with IEntityRandomAccess with IEntitySequenceAccess {
  private val elementTypeCode = CSONElement.getElementTypeCode(elementSchemaOrType)
  private val thisElementType = CSONElement.getArrayType(elementSchemaOrType)
  private val arrayItems = new scala.collection.mutable.Queue[IElementValue]
  private var count : Int = 0

  if (rawData.isDefined) {
    val arrayCursor = rawData.get
    val ArrayCount = arrayCursor.reader.ReadCount(rawData.get)
    for {index <- 0 until ArrayCount} append(ReadElement(arrayCursor,index))
  }
  private def ReadElement(cur:CSONCursor,index:Int):IElementValue = {
    val elementTypeCode = cur.reader.ReadElementType(cur, index)

    CSONTypes(elementTypeCode) match {
      case CSONTypes.Array => throw new Exception("CSON array inside another array was read. It's illegal format.")
      case CSONTypes.EmbeddedDocument => new CSONComplexElement(thisElementType.elementSchema,cur.reader.GetChildElementCursor(cur, index));
      case _ => CSONElement.getElement(cur, index)
    }
  }

  def apply(index:Any):IElementValue = arrayItems(CSONElement.getIndex(index))

  override def getValue() : Any = this
  override def setValue(newValue:Any):Unit = throw new Exception("Can't perform replacing whole value on Element Array. Replace the element in parent structure.")
  override def iterator:Iterator[IElementValue] = arrayItems.toIterator
  override def append(value:Any) : Boolean = {
    // Can't support concurrenctly append. Otherwise Counter will be wrong
    count+=1
    arrayItems.enqueue(if (value.isInstanceOf[IElementValue]) value.asInstanceOf[IElementValue]
               else {
                 if (value==null) CSONElement.NullValue
                 else if (thisElementType.elementSchema==null) {
                   val element = new CSONElement(elementTypeCode)
                   element.setValue(value)
                   element
                 } // Can't append a complex object to an array. Instead you should serialize this object into ComplexElement then append
                 else throw new IllegalArgumentException
               })
    true
  }

  override def length : Int = count

  override def getValue(index:Any): Any = apply(index).getValue
  override def getSchema : IEntitySchema = thisElementType.elementSchema
  override def getElement(index:Any):IElementValue = apply(index)
  override def getRawValue(index:Any,NeedTypeCode:Boolean=true):Array[Byte] = {
    val outBuffer = ByteBuffer.wrap(new Array[Byte](4096)).order(ByteOrder.LITTLE_ENDIAN)
    val itemElement = apply(index)
    val len = itemElement.elementType.getRawValue(outBuffer, itemElement.getValue, NeedTypeCode).flip().limit()
    val ret = new Array[Byte](len)
    outBuffer.get(ret)
    ret
  }
  override def add(that: IElementValue) = throw new Exception("This operation can't perform on element Array.")
  override def elementType : IElementType = thisElementType
  override def compare(that:IElementValue):Int = throw new Exception("This operation can't perform on element Array.")
  def getElementSchemaOrType: Any = if (thisElementType.elementSchema!=null) thisElementType.elementSchema else thisElementType.elementType
  override def setValue(index:Any, value:AnyRef) = {
    val i = CSONElement.getIndex(index)
    if (i < count) {
      val value2Set = if (value == null) CSONElement.NullValue else value
      if (value2Set.isInstanceOf[IElementValue]) arrayItems.update(i, value2Set.asInstanceOf[IElementValue])
      else {
        if (apply(i) == CSONElement.NullValue) arrayItems.update(i, new CSONElement(elementTypeCode, value2Set))
        else arrayItems(i).setValue(value2Set)
      }
    }
  }
}