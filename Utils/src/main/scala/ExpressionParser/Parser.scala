package ExpressionParser
import scala.collection.mutable.{Queue,HashMap}
import TokenId._

class Parser(text:String,args:Array[Object]) {
  private var textPos : Int = 0
  private val textLen : Int = text.length()
  private var ch : Char = if (textPos<textLen) text(textPos) else '\0'
  private var token : Token = null
  private val externals =  new HashMap[String,Object]
  var ParamExp:Array[Expression] = new Array[Expression](if (args!=null) args.length else 0)
  
  if (Parser.keywords.size == 0) Parser.createKeywords
  if (Parser.symbols.size == 0) Parser.processPredefinedFuncs
  if (args!=null) processValues(args)
  nextToken

  def parse():Expression = {
    val ret = parseLogicalOr
    validateToken(TokenId.End,Parser.SyntaxError)
    ret
  }
  private def processValues(values:Array[Object]) = for (i<-0 until values.length) externals.put("@"+i.toString(),values(i))
  
  private def validateToken(t:TokenId, errorMessage:String) = if (token.id != t) throw parseError(errorMessage)
  private def tokenIdentifierIs(id:String): Boolean = {(token.id == TokenId.Identifier) && (id==token.text.toLowerCase())}
  
  private def getIdentifier():String = {
    validateToken(TokenId.Identifier,Parser.IdentifierExpected)
    var id = token.text
    if (id.length()>1 && id(0)=='@')id.substring(1)
    else id
  }
  private def setTextPos(pos:Int) = {textPos = pos; ch = if (textPos<textLen) text(textPos) else '\0'}
  private def nextChar = {if (textPos<textLen) textPos += 1; ch = if (textPos<textLen) text(textPos) else '\0'}
  private def nextToken = {
    def parseString = {
      do {nextChar} while (textPos < textLen && ch != '\'')
      if (textPos == textLen) throw new ParseException(Parser.UnterminatedStringLiteral, textPos)
      else nextChar
      TokenId.StringLiteral
    }
    while (ch.isWhitespace) nextChar
    
    var t:TokenId = TokenId.End
    val tokenPos = textPos
    ch match {
      case '!' => nextChar; t = if (ch=='=') {nextChar;TokenId.ExclamationEqual} else TokenId.Exclamation
      case '&' => nextChar; t = if (ch=='&') {nextChar;TokenId.DoubleAmphersand} else TokenId.Amphersand
      case '(' => nextChar; t = TokenId.OpenParen
      case ')' => nextChar; t = TokenId.CloseParen
      case ',' => nextChar; t = TokenId.Comma
      case '.' => nextChar; t = TokenId.Dot
      case '/' => nextChar; t = TokenId.Slash
      case ':' => nextChar; t = TokenId.Colon
      case '<' => nextChar; t = if (ch=='=') {nextChar;TokenId.LessThanEqual}
                                 else if (ch=='>') {nextChar;TokenId.LessGreater}
                                 else TokenId.LessThan
      case '=' => nextChar; t = if (ch=='=') {nextChar;TokenId.DoubleEqual} else TokenId.Equal
      case '>' => nextChar; t = if (ch=='=') {nextChar;TokenId.GreaterThanEqual} else TokenId.GreaterThan
      case '|' => nextChar; t = if (ch=='|') {nextChar;TokenId.DoubleBar} else TokenId.Bar
      case '\'' => t = parseString
      case _   => if (ch.isLetter || ch == '@' || ch == '_') {
                    do {nextChar} while (ch.isLetterOrDigit || ch == '_')
                    t = TokenId.Identifier
                  } else if (textPos == textLen) t = TokenId.End
                  else throw parseError(textPos, Parser.InvalidCharacter, text);
    }
    token = new Token()
    token.id = t
    token.text = text.substring(tokenPos,textPos)
    token.pos = textPos
  }
  private def parseError(pos:Int,format:String,args:Object*): ParseException = new ParseException(format.format(args),pos)
  private def parseError(format:String,args:Object*): ParseException = parseError(token.pos,format,args)

  private def parseMemberAccess(value:Expression):Expression = {
    val errorPos = token.pos
    val id = getIdentifier
    value.name += "." + token.text
    nextToken
    value
  }
  private def parseMethodCall(methodCall:MethodCallExpression):Expression = {
    val errorPos = token.pos
    nextToken
    new MethodExpression(methodCall.name, parseArgumentList)
  }
  private def parseArgumentList():Array[Expression] = {
    validateToken(TokenId.OpenParen, Parser.OpenParenExpected)
    nextToken
    val args = if (token.id != TokenId.CloseParen) parseArguments() else new Array[Expression](0)
    validateToken(TokenId.CloseParen, Parser.CloseParenOrCommaExpected)
    nextToken
    args
  }
  private def parseArguments():Array[Expression] = {
    val argList = new Queue[Expression]
    argList.enqueue(parseExpression)
    while (token.id == TokenId.Comma) {
      nextToken
      argList.enqueue(parseExpression)
    }
    argList.toArray[Expression]
  }
  private def parseExpression():Expression = parseLogicalOr()
  private def parseLogicalOr():Expression = {
    var left = parseLogicalAnd
    while (token.id == TokenId.DoubleBar || tokenIdentifierIs("or")) {
      nextToken
      left = Parser.BinaryExpAdd(left, "or", parseLogicalAnd)
    }
    left
  }
  private def parseLogicalAnd():Expression = {
    var left = parseComparison
    while (token.id == TokenId.DoubleAmphersand || tokenIdentifierIs("and")) {
      nextToken
      left = Parser.BinaryExpAdd(left, "and", parseComparison)
    }
    left
  }
  /**
   * =, ==, !=, <>, >, >=, <, <= operators 
   */ 
  private def parseComparison():Expression = {
    var left = parseAdditive
    while (token.id == TokenId.Equal || token.id == TokenId.DoubleEqual ||
         token.id == TokenId.ExclamationEqual || token.id == TokenId.LessGreater ||
           token.id == TokenId.GreaterThan || token.id == TokenId.GreaterThanEqual ||
           token.id == TokenId.LessThan || token.id == TokenId.LessThanEqual) {
      val op = token
      nextToken
      val right = parseAdditive

      op.id match {
        case TokenId.DoubleEqual | TokenId.Equal =>  left = new BinaryExpression(left, "Equal", right)
        case TokenId.ExclamationEqual => left = new BinaryExpression(left, "NotEqual", right)
        case TokenId.LessGreater => left = new BinaryExpression(left, "NotEqual", right)
        case TokenId.GreaterThan => left = new BinaryExpression(left, "GreaterThan", right)
        case TokenId.GreaterThanEqual => left = new BinaryExpression(left, "GreaterThanEqual", right)
        case TokenId.LessThan => left = new BinaryExpression(left, "LessThan", right)
        case TokenId.LessThanEqual => left = new BinaryExpression(left, "LessThanEqual", right)
      }
    }
    left
  }
  private def parseAdditive(): Expression = parseUnary
  private def parseUnary(): Expression = {
    if (tokenIdentifierIs("not")) {
      val op = token
      nextToken
      var expr = parseUnary
      new UnaryExpression(expr,"not")
    }
    else parsePrimary
  }
  private def parsePrimary():Expression = {
    var expr = parsePrimaryStart
    while (token.id == TokenId.Dot) {
      nextToken
      expr = parseMemberAccess(expr)
    }
    expr
  }
  private def parsePrimaryStart():Expression = {
    token.id match {
      case TokenId.Identifier =>  parseIdentifier
      case TokenId.OpenParen => parseParenExpression
      case _ => throw parseError(Parser.ExpressionExpected)
    }
  }
  private def parseIdentifier():Expression = {
    validateToken(TokenId.Identifier,Parser.IdentifierExpected)
    var value:Object = Parser.hasKeyword(token.text.toLowerCase()).getOrElse(null)
    
    if (value!=null){ nextToken; value.asInstanceOf[ConstantExpression] }
    else {
      value = Parser.hasSymbols(token.text).getOrElse(externals.get(token.text).getOrElse(null))
      if (value!=null){
        if (value.isInstanceOf[MethodCallExpression]) {
          parseMethodCall(value.asInstanceOf[MethodCallExpression])
        }
        else { // Parameter Expression. eg. @0, @1, @2
          val paramIndex = token.text.substring(1).toInt
          ParamExp(paramIndex) = new ConstantExpression(token.text,value)
          nextToken
          ParamExp(paramIndex)
        }
      }
      else {
          val s = token.text
          nextToken
          new MemberExpression(s)
      }
    }
  }
  private def parseParenExpression():Expression = {
      validateToken(TokenId.OpenParen, Parser.OpenParenExpected)
      nextToken
      val ret = parseExpression
      validateToken(TokenId.CloseParen, Parser.CloseParenOrOperatorExpected)
      nextToken
      ret
  }
}
object Parser{
  def createKeywords() = {
    keywords.put("true", new ConstantExpression("true",boolean2Boolean(true)))
    keywords.put("false", new ConstantExpression("false",boolean2Boolean(false)))
    keywords.put("null", new ConstantExpression("null",null))
  }
  def processPredefinedFuncs() = PreDefinedFunc.foreach((item:String)=>symbols.put(item, new MethodCallExpression(item)))
  
  def BinaryExpAdd(left:Expression, operand:String,right:Expression):BinaryExpression = {
    if (left.isInstanceOf[BinaryExpression]) left.asInstanceOf[BinaryExpression].leftadd(operand, right)
    else if (right.isInstanceOf[BinaryExpression]) right.asInstanceOf[BinaryExpression].rightadd(operand, left)
    else new BinaryExpression(left,operand,right)
  }
  val keywords = new HashMap[String,Object]
  val symbols = new HashMap[String, Object]
  val PreDefinedFunc = List("substringof","startswith","endswith","InSet")
  def hasKeyword(key:String):Option[Object] = keywords.get(key)
  def hasSymbols(key:String):Option[Object] = symbols.get(key)
  val DuplicateIdentifier = "The identifier '%s' was defined more than once";
  val ExpressionExpected = "Expression expected";
  val InvalidCharacterLiteral = "Character literal must contain exactly one character";
  val UnknownIdentifier = "Unknown identifier '%s'";
  val FirstExprMustBeBool = "The first expression must be of type 'Boolean'";
  val BothTypesConvertToOther = "Both of the types '{0}' and '{1}' convert to the other";
  val NeitherTypeConvertsToOther = "Neither of the types '{0}' and '{1}' converts to the other";
  val MissingAsClause = "Expression is missing an 'as' clause";
  val ArgsIncompatibleWithLambda = "Argument list incompatible with lambda expression";
  val TypeHasNoNullableForm = "Type '{0}' has no nullable form";
  val NoMatchingConstructor = "No matching constructor in type '{0}'";
  val AmbiguousConstructorInvocation = "Ambiguous invocation of '{0}' constructor";
  val CannotConvertValue = "A value of type '{0}' cannot be converted to type '{1}'";
  val NoApplicableMethod = "No applicable method '{0}' exists in type '{1}'";
  val MethodsAreInaccessible = "Methods on type '{0}' are not accessible";
  val MethodIsVoid = "Method '{0}' in type '{1}' does not return a value";
  val AmbiguousMethodInvocation = "Ambiguous invocation of method '{0}' in type '{1}'";
  val UnknownPropertyOrField = "No property or field '%s' exists in type '%s'";
  val NoApplicableAggregate = "No applicable aggregate method '{0}' exists";
  val CannotIndexMultiDimArray = "Indexing of multi-dimensional arrays is not supported";
  val InvalidIndex = "Array index must be an integer expression";
  val NoApplicableIndexer = "No applicable indexer exists in type '{0}'";
  val AmbiguousIndexerInvocation = "Ambiguous invocation of indexer in type '{0}'";
  val IncompatibleOperand = "Operator '{0}' incompatible with operand type '{1}'";
  val IncompatibleOperands = "Operator '{0}' incompatible with operand types '{1}' and '{2}'";
  val UnterminatedStringLiteral = "Unterminated string literal";
  val InvalidCharacter = "Syntax error '%s'";
  val DigitExpected = "Digit expected";
  val SyntaxError = "Syntax error";
  val TokenExpected = "{0} expected";
  val ParseExceptionFormat = "{0} (at index {1})";
  val ColonExpected = "':' expected";
  val OpenParenExpected = "'(' expected";
  val CloseParenOrOperatorExpected = "')' or operator expected";
  val CloseParenOrCommaExpected = "')' or ',' expected";
  val DotOrOpenParenExpected = "'.' or '(' expected";
  val OpenBracketExpected = "'[' expected";
  val CloseBracketOrCommaExpected = "']' or ',' expected";
  val IdentifierExpected = "Identifier expected";
}
