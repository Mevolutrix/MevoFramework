package ExpressionParser

object TokenId extends Enumeration {
  type TokenId = Value
  val Unknown,End,Identifier,StringLiteral,IntegerLiteral,RealLiteral,
        Exclamation,Percent,Amphersand,OpenParen,CloseParen,Asterisk,
        Plus,Comma,Minus,Dot,Slash,Colon,LessThan,Equal,GreaterThan,
        Question,OpenBracket,CloseBracket,Bar,ExclamationEqual,
        DoubleAmphersand,LessThanEqual,LessGreater,DoubleEqual,
        GreaterThanEqual,DoubleBar = Value

}
class Token {
  import TokenId._
  var id : TokenId = TokenId.End
  var text : String = null
  var pos : Int = 0
}

class Expression{
  var name : String = null
  var value : Object = null
}
class BinaryExpression(left:Expression,operand:String,right:Expression) extends Expression {
  private var expGroups = List(left,right)
  def leftadd(method:String,rightExpression:Expression): BinaryExpression = {
    if (method!=operand) new BinaryExpression(this,method,rightExpression)
    else { expGroups=expGroups:::List(rightExpression); this }
  }
  def rightadd(method:String,leftExpression:Expression): BinaryExpression = {
    if (method!=operand) new BinaryExpression(leftExpression,method,this)
    else { expGroups = List(leftExpression):::expGroups; this }
  }
  def getExpList = expGroups
  def getOperand = operand
}
class UnaryExpression(left:Expression,operand:String) extends Expression {
  
  def getLeft = left
  def getOperand = operand
}
class ConstantExpression(id:String,v:Object) extends Expression {
  name = id
  value = v
}
class MethodExpression(method:String,args:Array[Expression]) extends Expression {
  name = method
  def getArgList = args
}
class MethodCallExpression(method:String) extends Expression {name = method}
/**
 * PropertyMember
 */
class MemberExpression(memberName:String) extends Expression {
  name = memberName
}

class ParseException(errorMessage:String,index:Int) extends Exception("%s (at index %d)".format(errorMessage,index)) {
}