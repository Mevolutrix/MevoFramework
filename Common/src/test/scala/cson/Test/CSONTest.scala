package cson.Test
import java.text.SimpleDateFormat
import java.util._
import java.nio.{ByteOrder, ByteBuffer}
import CSON.Types.{CSONComplexType, CSONTypesArray, CSONTypes}
import EntityInterface.StorageType._
import EntityInterface._
import EntityInterface.DefaultValueType._
import EntityInterface.EntityType._
import EntityInterface.IndexType._
import EnumHelper._
import JSON.JsonString
import org.scalatest.FunSuite
import CSON.{CSONElement, CSONElementArray, CSONDocument}
import EntityAccess._

class ComplexObj {
  var name:String = "Test string."
  var defV:DefaultValueType = DefaultValueType.Preset
  var idxType:IndexType = IndexType.Default
  var storeT:StorageType = StorageType.Shared
  var orderTyp:OrderByType.OderByType = OrderByType.Ascending
  var entType:EntityType = EntityType.ComplexType
  var objid:java.util.UUID = java.util.UUID.randomUUID()
  var binary:Array[Byte] = Array[Byte](1,2,3,4)
}
class POJOTest {
  import EnumType._
  var intValue:Int =3
  var boolValue:Boolean = true
  var nullPtr:String = null
  var c1:ComplexObj = new ComplexObj()
  var cArr:Array[ComplexObj] = Array[ComplexObj](new ComplexObj(),null,new ComplexObj())
  var byteValue:Byte = 2.toByte
  var longValue:Long = Long.MaxValue
  val floatValue:Float = Float.MaxValue
  val doubleValue:Double = Double.MinPositiveValue
  val moneyValue:BigDecimal = BigDecimal(doubleValue)
  var timeStamp:Date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse("2014/08/26 10:22:19")
  var s:String = "Test is test!\n\0"
  var dValue:BigDecimal = BigDecimal("123.4567")
  var enum:EnumType = EnumType.a10
  var intArr:Array[Int] = Array[Int](1,0,3)

  override def equals(obj: Any):Boolean = {
    val testObj = obj.asInstanceOf[POJOTest]
    (intValue==testObj.intValue) &&
    (boolValue==testObj.boolValue) &&
    (byteValue==testObj.byteValue) &&
    (longValue==testObj.longValue) &&
    (timeStamp.compareTo(testObj.timeStamp)==0) &&
    (s==testObj.s) &&
    (dValue==testObj.dValue) &&
    (enum.id==testObj.enum.id)
  }
}
class CSONTest extends FunSuite{
  test("CSON serialize Object:") {
    val testObj = new POJOTest()
    val schema = GeneralEntitySchema(testObj.getClass)
    val serializer = GeneralEntityToCSON(testObj.getClass)
    var bb = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
    val (objCSON, csonBytes) = serializer.writeObjectToCSON(testObj, bb)
    csonBytes.flip()
    val newObj = serializer.getObject(new CSONDocument(schema, Some(csonBytes))).asInstanceOf[POJOTest]
    val (newCSON, _) = serializer.writeObjectToCSON(newObj, null)
    println("CSON bytes size:" + newCSON.asInstanceOf[CSONDocument].getBytes().length)
    assert(newObj == testObj)
    assert(objCSON.getElement(1).compare(newCSON.getElement(1)) == 0)
    assert(objCSON.getElement("dValue").compare(newCSON.getElement("dValue")) == 0)
    val e = objCSON.getElement("byteValue")
    val arr = objCSON.getElement("cArr").asInstanceOf[CSONElementArray]
    e.add(newCSON.getElement("byteValue"))
    assert(e.getValue() == 4.toByte)
    assert(arr.getValue(1)==null)
  }
  test("JSON serialize Object:") {
    val testObj = new POJOTest()
    val schema = GeneralEntitySchema(testObj.getClass)
    val serializer = GeneralEntityToCSON(testObj.getClass)
    val obj2JSON = JSONSerializer(testObj)
    val (csonOfObj, _) = serializer.writeObjectToCSON(testObj, null)
    val cson2JSON = JSONSerializer(csonOfObj)
    assert(obj2JSON == cson2JSON)
    val objFromJSON = JSONSerializer.unapply(cson2JSON, classOf[POJOTest]).asInstanceOf[POJOTest]
    val f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    assert(testObj == objFromJSON)
  }
  test("Schema test object:") {
    val schema = new IEntitySchema {
      def entityId: String = "TestCase.Schema"

      def getID(PropertyName: String): Int = if (PropertyName=="id") 0 else -1

      def count: Int = 1

      def getTypeCode(Property: Any): Byte = CSONTypes.Timestamp.id.toByte

      def getPropertyName(ID: Int): String = "id"

      def objType: Class[_] = null

      /**
       * Return the element IElementType if this property is an array,
       * return CSONComplexType if this property is a Complex Element.
       */
      def getElementType(Property: Any): IElementType =
        CSONTypesArray.CSONElementTypes(getTypeCode(Property))

      def containsProperty(property: String): Boolean = (property=="id")

      def schemaType: EntityType = EntityType.View
    }
    val csonDoc = new CSONDocument(schema,None)
    val curTime=new Date()
    csonDoc.setValue(0,curTime)
    // Test whether the timeStamp value is Long type and is the time ticket for the Date value
    assert(curTime.getTime==csonDoc.getValue(0))
    val binary = csonDoc.getRawValue(0)
    val hexStr = JsonString.binaryToString(binary)
    val newBinary = JsonString.hexStrToBinary(hexStr)
    assert(binary.length==newBinary.length)
    var flag:Boolean = true
    for (i<-0 until binary.length) if (binary(i)!=newBinary(i)) {
      flag = false
    }
    assert(flag)
  }
  test("Dynamic chema test object:") {
    type Binary = Array[Byte]
    val schema = new DynamicSchema("TestCase.Schema")
    val objSchema = GeneralEntitySchema(classOf[POJOTest])
    schema.add("id",CSONTypesArray.CSONElementTypes(CSONTypes.ObjectId.id))
    schema.add("content",new CSONComplexType(objSchema))
    schema.add("ne",CSONTypesArray.CSONElementTypes(CSONTypes.NullElement.id))
    val csonDoc = new CSONDocument(schema,None)
    val objId = java.util.UUID.randomUUID()
    csonDoc.setValue(0,objId)
    csonDoc.setValue("content",new POJOTest())
    csonDoc.setValue("ne",CSONElement.NullElement)
    // Test whether the timeStamp value is Long type and is the time ticket for the Date value
    assert(objId==csonDoc.getValue(0))
    val binary = csonDoc.getRawValue(0)
    val hexStr = JSONSerializer.getStringsFromRawValue(Array[Binary](binary)).apply(0)
    assert(objId==JSONSerializer.getValueFromString(hexStr,CSONTypes.ObjectId.id.toByte))
    assert(JSONSerializer(csonDoc)!=null)
  }
}
