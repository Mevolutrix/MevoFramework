package MySQLStore.client
import java.util
import EntityAccess._
import JSON.JsonString
import CSON.CSONDocument
import EntityInterface._
import EntityStore.Metadata._
import EntityStore.Interface._
import HandlerSocket.Protocol._
import java.nio.{ByteOrder, ByteBuffer}

object HandlerSocketClient {
import CompareOperator._
import LogicOperator._
import FilterType._
type Binary = Array[Byte]
  implicit def apply(al:util.ArrayList[HsCommand]):IndexedSeq[HsCommand] =
    for(i<-0 until al.size()) yield al.get(i)
  implicit def apply(filterAl:util.ArrayList[Filter]):Seq[Filter] =
    for(i<-0 until filterAl.size()) yield filterAl.get(i)
  private def prepareSetData(e:IEntityRandomAccess,schema:IEntitySchema):(OpenIndexSpec,String)={
    val setInfo = schema.asInstanceOf[IEntitySet]
    (HandlerSocketInstances(setInfo.appSpace).getIdxSpec(setInfo.setName,
      (s)=>{getSetIndexSpec(MetadataManager.getEntitySet(s))}),
     JsonString.getHS_Str(e.getValue(setInfo.primaryKey)))
  }
  private def getColData(es:IEntityRandomAccess,setSpec:OpenIndexSpec):Array[String] = {
    val columns = new Array[String](setSpec.columns.length) // primarykey+Index+data Columns
    for (i<-0 until setSpec.columns.length-1) {
      try { columns(i) = JsonString.getHS_Str(es.getValue(setSpec.columns(i))) }
      catch {
        case e:Exception => throw new IllegalArgumentException("PropertyName:"+setSpec.columns(i)+"|"+e.getMessage)
      }
    }
    // Check whether the last column name is _raw_DATA, no means this is a flatten set
    val lastColName = setSpec.columns(columns.length-1)
    try {
      if (es.getSchema.asInstanceOf[IEntitySet].isFlat)
        columns(columns.length - 1) = JsonString.getHS_Str(es.getValue(lastColName))
      else columns(columns.length - 1) = CSON2JSON(es.asInstanceOf[CSONDocument].toCSONElement)
    } catch {
      case e:Exception => throw new IllegalArgumentException("GetColumn Error:"+lastColName+"| setName:"+es.getSchema.entityId)
    }
    columns
  }
  def getInsertCmd(ins:InsertStatement,idxSpec:OpenIndexSpec):(OpenIndexSpec,IndexedSeq[HsCommand],(String,String,Array[String])) = {
    val schema = MetadataManager.getSchema(ins.schemaID)
    val ret = new Array[HsCommand](ins.records.length)
    val jsStatements =(ins.preOperationJS,ins.modifierJS,ins.jsParams)
    for (i<-0 until ret.length)
      ret(i)=Insert(getColData(new CSONDocument(schema,Some(ByteBuffer.wrap(ins.records(i).data).
                              order(ByteOrder.LITTLE_ENDIAN))),idxSpec))
    (idxSpec,ret,jsStatements)
  }
  def getDelCmd(del:DeleteStatement,idxSpec:OpenIndexSpec):(OpenIndexSpec,IndexedSeq[HsCommand],(String,String,Array[String])) =
    (idxSpec,Array[HsCommand](Delete(Eq(), JSONSerializer.getStringsFromRawValue(del.keys)))
      ,(del.preOperationJS,null,del.jsParams))

  def getUpdateCmd(update:UpdateStatement,idxSpec:OpenIndexSpec):(OpenIndexSpec,IndexedSeq[HsCommand],(String,String,Array[String])) = {
    val schema = MetadataManager.getSchema(update.schemaID)
    val ret = new Array[HsCommand](1)
    val jsStatements = (update.preOperationJS,update.modifierJS,update.jsParams)
    ret(0)=Update(Eq(),JSONSerializer.getStringsFromRawValue(Array[Binary](update.primaryKey)),
                 getColData(new CSONDocument(schema,Some(ByteBuffer.wrap(update.data).
                            order(ByteOrder.LITTLE_ENDIAN))),idxSpec))
    (idxSpec,ret,jsStatements)
  }
  def getQueryCmd(query:QueryStatement,idxSpec:OpenIndexSpec):(OpenIndexSpec,IndexedSeq[HsCommand],(String,String,Array[String])) = {
    // As reflection limitation, all enumeration field was converted to CSONTypes enum, so we have to convert again
    def getOp(cOp:CompareOperator) = CompareOperator(cOp.id)  match {
      case Equal => Eq()
      case Greater => Gt()
      case Less => Lt()
      case GreaterEqual => GE()
      case LessEqual => LE()
      case a@_ => throw new Exception("Unsupported compare operator."+a) //Todo: implement compare support for rest
    }
    def getSearchValue(search:SearchOperation) =
      if (search.paramertIndex<0) search.compareValue(0).value
      else query.argList(search.paramertIndex)
    def getQueries(query:Condition):util.ArrayList[HsCommand] = {
      new util.ArrayList[HsCommand]()  // Todo: implement the group conditions in recursive
    }
    def getQuery(query:Array[Condition]):HsCommand = null //Todo: Implement "And" query cmd
    def getSimpleQuery(searchOps:Array[SearchOperation],defaultKey:String):Get = {
      def getSimpleFilter(search:SearchOperation):Filter = Filter(FILTER, getOp(search.compareOperator),
        idxSpec.columns.lastIndexWhere(_==search.searchPropertyName),
        JsonString.getHS_Str(GeneralEntityToCSON.readRawValue2Obj(getSearchValue(search),false)) )
      if (searchOps==null) // selecct *
        Get(GE(),Array[String](defaultKey),(20000,0),null)
      else {
        // scan each search Operation(skip the pkey compare operation) and generate one Filter
        val pKeyIndex = searchOps.lastIndexWhere(_.searchPropertyName == null)
        val filters = new util.ArrayList[Filter]()
        for (i <- 0 until searchOps.length
             if (i != pKeyIndex)) filters.add(getSimpleFilter(searchOps(i)))
        if (pKeyIndex >= 0) getKeySearch(searchOps(pKeyIndex), filters)
        else {
          // search condition has no index field
          Get(GE(), Array[String](defaultKey), (20000, 0), filters)
        }
      }
    }
    def getKeySearch(search:SearchOperation, filters:Seq[Filter]=null):Get =
      Get(getOp(search.compareOperator),Array[String](JsonString.getHS_Str(GeneralEntityToCSON.
        readRawValue2Obj(getSearchValue(search),false))),(20000,0),filters)
    def getOpenIndexSpec(idxSpec:OpenIndexSpec,selects:Array[String],filter:Array[String]=null):OpenIndexSpec = {
      // all selects are fit in the index properties
      def selectIndex(selects:Array[String],cols:Array[String]):Boolean =
        selects.filter(cols.contains(_)).length == selects.length

      if (selects!=null && selectIndex(selects,idxSpec.columns)) {
        OpenIndexSpec(idxSpec.db, idxSpec.table, selects,idxSpec.index, idxSpec.filterCols)
      }
      else idxSpec
    }

    val es = MetadataManager.getEntitySet(query.query.queryEntitySet)
    val qc = query.query.queryCondition
    val select:Array[String] =
      if (query.query.queryProjections != null)
        query.query.queryProjections.map(_.alias)
      else null

    val queryCount = if (qc==null) 1 else if (qc.isAbstractGroup) qc.subConditions.length
                     else qc.queryOperations.length
    val ret = new util.ArrayList[HsCommand]()
    if (qc==null) {
      ret.add(getSimpleQuery(null,es.getPrimaryKeyMin))
    }
    else if (qc.isAbstractGroup) { // subConditions
      if (qc.operator == Or)
        for (i <- 0 until queryCount) {
          ret.addAll(getQueries(qc.subConditions(i)))
        }
      else ret.add(getQuery(qc.subConditions))// And
    }
    else {  // queryOperations
      if (qc.operator == Or)
        for(i<-0 until queryCount)
        {
          ret.add(getSimpleQuery(Array[SearchOperation](qc.queryOperations(i)),es.getPrimaryKeyMin))
        }
      // query by primary key
      else if (qc.queryOperations.length == 1 && qc.queryOperations(0).searchPropertyName == null)
        ret.add(getKeySearch(qc.queryOperations(0)))
        // if the search condition no include pKey, use min value of the pKey to search whole table
      else {
        ret.add(getSimpleQuery(qc.queryOperations,es.getPrimaryKeyMin))
      }
    }
    (getOpenIndexSpec(idxSpec,select),ret,null)
  }
  def getSetInfo(st:OperationStatement):IEntitySet = MetadataManager.getEntitySet(st match {
      case query:QueryStatement => query.query.queryEntitySet
      case ins:InsertStatement => ins.es_name
      case del:DeleteStatement => del.es_name
      case update:UpdateStatement => update.es_name
    })

  def getSetIndexSpec(setInfo:IEntitySet):OpenIndexSpec = {
    val setIndexProps = setInfo.indexProperties
    // pKey + index properties
    val filterCols = new Array[String](setIndexProps.length+1)
    // if the set is flatten( all fields is index and no raw_DATA) +1 else +2 (add _raw_DATA)
    val columns =new Array[String](if (!setInfo.isFlat) setIndexProps.length+2 else setIndexProps.length+1)
    columns(0) = setInfo.primaryKey
    filterCols(0) = columns(0)
    for (n<-0 until setIndexProps.length) {
      columns(n+1)=setIndexProps(n)
      filterCols(n+1) = columns(n+1)
    }
    if (!setInfo.isFlat) columns(setIndexProps.length+1)  = "_raw_DATA_"
    val dbName = MetadataManager.getAppSpace(setInfo.appSpace).storeConfig.dbConnString.split('/')(1)
    OpenIndexSpec(dbName,setInfo.setName.replace('.','_'),columns,"PRIMARY",filterCols)
    }
  def getHsCommand(st:OperationStatement,idxSpec:OpenIndexSpec):(OpenIndexSpec,IndexedSeq[HsCommand],(String,String,Array[String])) = st match {
    case ins:InsertStatement => getInsertCmd(ins,idxSpec)
    case del:DeleteStatement => getDelCmd(del,idxSpec)
    case upd:UpdateStatement => getUpdateCmd(upd,idxSpec)
    case query:QueryStatement => getQueryCmd(query,idxSpec)
  }
  // Generate OpenIndex and HandlerSocket commands. Result was collected in <TaskProcessContext>
  def parseRequests(operationStatements:util.ArrayList[OperationStatement]) =
    for (i <- 0 until operationStatements.size()) yield ({
      val st = operationStatements.get(i)
      val setIdxSpec = getSetIndexSpec(getSetInfo(st))
      getHsCommand(st, setIdxSpec)
    })
}
