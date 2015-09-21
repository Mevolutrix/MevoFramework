package EntityStore.Interface

class DeleteStatement extends OperationStatement {
  var es_name:String = null
  var tenantID:Int = 0
  var schemaID:String = null
  // Either keys or condition is valid
  var keys:Array[Binary] = null
  var condition:Condition = null
  /**
   * ִ在复杂条件下是否Delete取决于preOperationJS返回结果，为true则执行condition中的操作，为false不执行
   */
  var preOperationJS:String = null
  var jsParams:Array[String] = null
}