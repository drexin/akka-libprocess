package akka.libprocess.serde

import scala.util.Try

trait MessageSerDe {
  def deserialize(message: TransportMessage): Try[AnyRef]
  def serialize(obj: Any): Try[TransportMessage]
}

case class TransportMessage(messageName: String, data: Array[Byte])
