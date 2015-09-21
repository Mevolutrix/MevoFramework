package EntityStore.Interface

object TransformerFunction extends Enumeration {
  type TransformerFunction = Value
  var JS_CODE = Value(0) // A piece of js code
  var REPLACE = Value(1) // execute this C++ function : data() = new_data
  var PATIAL_REPLACE = Value(2) // execute this C++ function : field(fieldIndex) = new_data
    }

class UpdateStatement extends OperationStatement {
  import TransformerFunction._
  var es_name:String = null
  var schemaID:String = null
  var tenantID:Int = 0
  var primaryKey:Binary = null
  var func:TransformerFunction = REPLACE
  var data:Binary = null
  var transformer : Array[Transformer] = null
  var condition:Condition = null
  var preOperationJS:String = null
  var modifierJS:String = null
  var jsParams:Array[String] = null
}

/**
 * Storing property update instruction and the updated properties
 */
class Transformer {
  var fieldIndex:String = null
  var data:Array[Byte] = null
}
