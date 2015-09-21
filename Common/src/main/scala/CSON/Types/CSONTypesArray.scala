package CSON.Types

object CSONTypesArray {
  val CSONElementTypes = new Array[EntityInterface.IElementType](CSONTypes.Single.id+1)
  
  for{
      simpleType <- CSONTypes.values 
      if (simpleType!=CSONTypes.EmbeddedDocument && simpleType!=CSONTypes.Array)
      typeCode = simpleType.id.toByte 
  } CSONElementTypes(typeCode) = new CSONSimpleType(typeCode)
  
  val NullType = CSONElementTypes(CSONTypes.NullElement.id).asInstanceOf[CSONSimpleType]
  val BinaryType = CSONElementTypes(CSONTypes.BinaryData.id).asInstanceOf[CSONSimpleType]
  val IntElementType = CSONElementTypes(CSONTypes.Int32.id).asInstanceOf[CSONSimpleType]
  val StrElementType = CSONElementTypes(CSONTypes.UTF8String.id).asInstanceOf[CSONSimpleType]
  private def getInnerTypeName(classType:Class[_]) =if (classType.isEnum()
    ||classType.getName()=="scala.Enumeration$Value"
    ||classType.getName()=="scala.Enumeration$Val") "int"
  else classType.getName()
  def typeFactory(classType:Class[_]) = {
    getInnerTypeName(classType) match {
          case "boolean"|"java.lang.Boolean" => CSONElementTypes(CSONTypes.Boolean.id);
          case "byte" => CSONElementTypes(CSONTypes.Int8.id);
          case "short" => CSONElementTypes(CSONTypes.Int16.id);
          case "int"|"java.lang.Integer" => CSONElementTypes(CSONTypes.Int32.id);
          case "scala.math.BigDecimal" => CSONElementTypes(CSONTypes.Decimal.id);
          case "double" => CSONElementTypes(CSONTypes.FloatingPoint.id);
          case "float"=> CSONElementTypes(CSONTypes.Single.id);
          case "long"|"java.lang.Long"=> CSONElementTypes(CSONTypes.Int64.id);
          case "java.lang.String"=> CSONElementTypes(CSONTypes.UTF8String.id);
          case "java.util.Date"=> CSONElementTypes(CSONTypes.UTCDatetime.id);
          case "java.util.UUID"=> CSONElementTypes(CSONTypes.ObjectId.id);
          case "[B"=> CSONElementTypes(CSONTypes.BinaryData.id);
          case a@_=> null
    }
  }
  def typeCodeOf(classType:Class[_]):Byte = {
    (getInnerTypeName(classType) match {
      case "boolean" => CSONTypes.Boolean.id
      case "byte" => CSONTypes.Int8.id
      case "short" => CSONTypes.Int16.id
      case "int" => CSONTypes.Int32.id
      case "java.math.BigDecimal" => CSONTypes.Decimal.id
      case "double" => CSONTypes.FloatingPoint.id
      case "float" => CSONTypes.Single.id
      case "long" => CSONTypes.Int64.id
      case "java.lang.String" => CSONTypes.UTF8String.id
      case "java.util.Date" => CSONTypes.UTCDatetime.id
      case "java.util.UUID" => CSONTypes.ObjectId.id
      case "[B" => CSONTypes.BinaryData.id
      case _ => -1
    }).toByte
  }
}