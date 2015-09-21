package EntityStore.Interface

class InsertStatement extends OperationStatement {
  var es_name:String = null
  var schemaID:String = null
  var tenantID:Int = 0
  var records:Array[InsertRecord] = null
  var preOperationJS:String = null
  var modifierJS:String = null
  var jsParams:Array[String] = null // parameters for JScript
}
class InsertRecord {
  var key:Array[Byte] = null
  var data:Array[Byte] = null
}