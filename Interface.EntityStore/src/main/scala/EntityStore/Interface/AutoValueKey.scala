package EntityStore.Interface

/**
 * General implementation for auto increament key
 */
class AutoValueKey {
  var id:Long = 0L
  var fullSetName:String = null
  var intAutoValue:Int = 0
  var longAutoValue:Long = 0L
  var strAutoValue:String = null
  // This will be called to create an uniq key for auto value record
  def genKey = {id = (new java.util.Date()).getTime; id}
}
