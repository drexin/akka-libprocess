package akka.libprocess

import org.apache.mesos.Protos.FrameworkInfo
import mesos.internal.Messages.{FrameworkRegisteredMessage, RegisterFrameworkMessage}
import akka.actor.{Props, ActorSystem}
import akka.libprocess.serde.{TransportMessage, MessageSerDe}
import scala.util.{Failure, Success, Try}
import com.typesafe.config.Config

object Main extends App {

  val msg = RegisterFrameworkMessage.newBuilder().setFramework(
    FrameworkInfo.newBuilder().setUser("corpsi").setName("akka-test")
  ).build()

  val system = ActorSystem("System")

  val manager = LibProcess(system).manager

  val slave = system.actorOf(Props[MyReceiver])

  val master = LibProcess(system).remoteRef(PID("192.168.2.101", 5050, "master"))

  import system.dispatcher

  master onSuccess {
    case x => x ! LibProcessMessage("slave", msg)
  }
}

class MyReceiver extends LibProcessActor {
  val name = "slave"

  def lpReceive = {
    case x =>
      log.info(s"received $x")
  }
}

class MesosSerDe(config: Config) extends MessageSerDe {
  override def deserialize(message: TransportMessage): Try[AnyRef] = message.messageName match {
    case "mesos.internal.FrameworkRegisteredMessage" =>
      Success(FrameworkRegisteredMessage.parseFrom(message.data))

    case name =>
      Failure(new ClassMappingException(s"No mapping found for message type '${name}'"))
  }

  override def serialize(obj: Any): Try[TransportMessage] = obj match {
    case m: RegisterFrameworkMessage =>
      Success(TransportMessage("mesos.internal.RegisterFrameworkMessage", m.toByteArray))

    case _ =>
      Failure(new ClassMappingException(s"No mapping found for class '${obj.getClass.getName}'"))
  }
}

class ClassMappingException(msg: String) extends Exception(msg)
