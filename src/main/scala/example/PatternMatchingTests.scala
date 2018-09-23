package example

object PatternMatchingTests  extends App {

  case class A(name : String)
  case class B(name : String)
  case class Configuration(a : Option[A], b : Option[B])

  def work(configuration: Configuration): Unit ={

    (configuration.a, configuration.b) match {
      case (Some(a), Some(b)) => println(a.name,b.name)
      case (Some(_), None) => println("B was missing.")
      case (None, Some(_)) => println("A was missing.")
    }
  }

  val bb = Some(B("b"))
  val aa = Some(A("a"))
  val none = None

  work(Configuration(aa, bb))


}
