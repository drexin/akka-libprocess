package akka.libprocess

import scala.util.Try

trait MessageParser {
  def parse(messageName: String, data: Array[Byte]): Try[AnyRef]
}

class ProtobufParser(classMapping: Map[String, Class[_]]) extends MessageParser {
  def parse(messageClass: String, data: Array[Byte]): Try[AnyRef] = Try {
    classMapping.get(messageClass) match {
      case Some(cls) =>
        val parseFrom = cls.getMethod("parseFrom", classOf[Array[Byte]])
        parseFrom.invoke(null, data)

      case None => throw new ClassNotFoundException(s"Could not find class $messageClass")
    }
  }
}
