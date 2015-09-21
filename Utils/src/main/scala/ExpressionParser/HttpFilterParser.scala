package ExpressionParser
import java.text.SimpleDateFormat

object EntityRequest {
  def getKey(s: String):(String,Object) = {
    val parser = new FilterParser(s)
    val args = parser.normalize().parseParams
    val EntityNameStr = parser.result
    val indexOfParen = EntityNameStr.indexOf('(')
    (if (indexOfParen>0) EntityNameStr.substring(0,indexOfParen) else EntityNameStr,
      if (args!=null && args.length>0) args(0) else null)
  }
  def parseFilter(f:String):(String,Array[Object]) = if (f!=null) {
    val parser = new FilterParser(f)
    val args = parser.normalize().convert2LinqSyntax().parseParams
    (parser.result,args)
  } else (null,null)
  def getField(args:Map[String,String],name:String):String =
    try args(name) catch { case _:Throwable => null }
  def parseRequest(req:String):Map[String,String] =
    try {
      req.split("&").map(_.split("=") match {
        case Array(x:String,y:String) =>(x,y)
        case _ => (null,null)
      }).toMap[String,String]
    } catch {
      case e:Exception => null
    }
  def getFieldValue(args:Map[String,String],name:String):Object =
    try {
      val parser = new FilterParser("arg="+args(name))
      val rets = parser.normalize().parseParams
      rets(0)
    } catch { case _:Throwable=>null }
}

class FilterParser(text:String) {
  private var content:String = text

  /**
   * Seek the input string and find the value and convert them to Object
   * @param text
   * @param index
   * @return (Object returned if this is a value, next Index Pos, isValue:Boolean)
   **/
  private def nextToken(text:String,index:Int=0):(Object,Int,Boolean) = {
    var textPos : Int = 0
    val textLen : Int = text.length
    var ch : Char = if (textPos<textLen) text.charAt(textPos) else '\0'
    def nextChar ={
      if (textPos<textLen) textPos += 1
      ch = if (textPos<textLen) text.charAt(textPos) else '\0'
    }
    def trim = while (ch.isWhitespace) nextChar
    def peekNextChar = if (textPos<textLen-1) text.charAt(textPos+1) else '\0'
    def parseString = {
      do {nextChar} while (textPos < textLen && ch != '\'')
      if (textPos == textLen && ch != '\'') throw new ParseException(Parser.UnterminatedStringLiteral, textPos)
      else nextChar
      TokenId.StringLiteral
    }
    def isIdentifier = ch.isLetter || ch == '_'
    def parseIdentifier = {
      do {nextChar} while (ch.isLetterOrDigit || ch == '_')
      TokenId.Identifier
    }
    def isNum = ch.isDigit || (ch=='-' && peekNextChar.isDigit)
    def parseNum = {
      var ret = TokenId.IntegerLiteral
      do {nextChar} while (ch.isDigit)
      if (ch == '.') {
        ret = TokenId.RealLiteral
        do {nextChar} while (ch.isDigit)
      }
      if (ch == 'E' || ch == 'e') {
        ret = TokenId.RealLiteral
        nextChar
        if (ch == '+' || ch == '-')
          do {nextChar} while (ch.isDigit)
      }
      ret
    }
    def getNumParam(value:String,tokenType:TokenId.TokenId):(Object,Int,Boolean) = {
      //过滤数字后面的标识，如果为L,M|m,f等
      ((if (textPos < textLen) peekNextChar
      else '\0') match {
        case 'M' | 'm' => nextChar; BigDecimal(value)
        case 'f' => nextChar; value.toFloat.asInstanceOf[Object]
        case 'L'=> nextChar;value.toLong.asInstanceOf[Object]
        case _ => if (tokenType==TokenId.RealLiteral) BigDecimal(value)
        else value.toInt.asInstanceOf[Object]
      }, index + textPos, true)
    }
    def parseError(pos:Int,format:String,args:Object*): ParseException =
      new ParseException(format.format(args),pos)

    trim                          // Skip the blank
    val tokenPos = textPos
    var tokenType = TokenId.StringLiteral
    tokenType = ch match {
      case '!' => nextChar; if (ch=='=') {nextChar;TokenId.ExclamationEqual} else TokenId.Exclamation
      case '&' => nextChar; if (ch=='&') {nextChar;TokenId.DoubleAmphersand} else TokenId.Amphersand
      case '(' => nextChar; TokenId.OpenParen
      case ')' => nextChar; TokenId.CloseParen
      case ',' => nextChar; TokenId.Comma
      case '.' => nextChar; TokenId.Dot
      case '/' => nextChar; TokenId.Slash
      case ':' => nextChar; TokenId.Colon
      case '<' => nextChar; if (ch=='=') TokenId.LessThanEqual else if (ch=='>') TokenId.LessGreater else TokenId.LessThan
      case '=' => nextChar; if (ch=='=') TokenId.DoubleEqual else TokenId.Equal
      case '>' => nextChar; if (ch=='=') TokenId.GreaterThanEqual else TokenId.GreaterThan
      case '|' => nextChar; if (ch=='|') TokenId.DoubleBar else TokenId.Bar
      case '\'' => parseString
      case _ =>
        if (isIdentifier) parseIdentifier
        else if (isNum) parseNum
        else if (textPos == textLen) TokenId.End
        else throw parseError(textPos, Parser.InvalidCharacter, ch.toString)
    }
    val valueStr = text.substring(tokenPos, textPos)
    tokenType match {
      case TokenId.End => ("",index+textPos,false)
      case TokenId.Identifier => valueStr match {
        case "datetime" => if (ch=='\'') {
          val start = textPos
          parseString
          val value = text.substring(start+1,textPos-1)
          (new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse(value),index+textPos,true)
        } else (valueStr,index+textPos,false)
        case "true"  => (true.asInstanceOf[Object],index+textPos,true)
        case "false" => (false.asInstanceOf[Object],index+textPos,true)
        case "guid"  => if (ch=='\'') {
          val start = textPos
          parseString
          val value = text.substring(start+1,textPos-1)
          (java.util.UUID.fromString(value),index+textPos,true)
        }else (valueStr,index+textPos,false)
        case _ => (valueStr,index+textPos,false)
      }
      case TokenId.StringLiteral =>(text.substring(tokenPos+1, textPos-1),index+textPos,true)
      case TokenId.IntegerLiteral | TokenId.RealLiteral => getNumParam(valueStr,tokenType)
      case _ => (valueStr,index+textPos,false)
    }
  }
  def normalize():FilterParser = {
    if (content.startsWith("'") && content.endsWith("'")) content = content.substring(1,content.length-2)
    content = content.replaceAll("\'\'","'")
    this
  }
  def convert2LinqSyntax():FilterParser = {
    content = content.replaceAll(" eq ","=").replaceAll(" ge ",">=").replaceAll(" gt ",">").
      replaceAll(" le ","<=").replaceAll(" lt ","<").replaceAll("/",".")
    this
  }
  def result = content
  def parseParams:Array[Object] = {
    val sb = new StringBuilder()
    val params = new Array[Object](64)

    def parse(s:String,i:Int,argNum:Int):Int = {
      if (s==null || s.isEmpty) argNum
      else {
        val (obj,index,isParam) = nextToken(s,i)
        val nextText = if (index<s.length) s.substring(index) else null
        if (isParam) {
          params(argNum) = obj
          sb.append("@"+argNum.toString+" ")
          parse(nextText,0,argNum+1)
        }
        else {
          sb.append(obj.asInstanceOf[String])
          parse(nextText,0,argNum)
        }
      }
    }

    val argCount = parse(content,0,0)
    content = sb.result()
    params.slice(0,argCount)
  }
}
