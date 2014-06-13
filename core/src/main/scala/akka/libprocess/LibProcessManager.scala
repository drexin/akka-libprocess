package akka.libprocess

import akka.actor._
import com.typesafe.config.Config
import com.google.protobuf.MessageLite
import akka.libprocess.LibProcessHTTPEndpoint.Started
import java.net.InetSocketAddress
import scala.collection.JavaConverters._

class LibProcessConfig(config: Config) {
  val port: Int = config.getInt("port")
  val address: String = config.getString("address")
  val classMapping: Map[String, Class[_]] = {
    val mapping = config.getObject("class-mapping").unwrapped().asScala.toMap

    mapping.mapValues { cls =>
      Class.forName(cls.toString)
    }
  }

  val messageMapping: Map[Class[_], String] = {
    classMapping.map { case (name, cls) =>
      cls -> name
    }.toMap
  }
}

class LibProcessManager(config: LibProcessConfig) extends Actor with ActorLogging with Stash {
  import LibProcessManager._

  val parser = new ProtobufParser(config.classMapping)

  context.actorOf(Props(classOf[LibProcessHTTPEndpoint], config.address, config.port, parser))

  var registry = Map.empty[String, ActorRef]
  var remoteRefCache = Map.empty[PID, ActorRef]

  def receive = {
    case Started =>
      context.become(ready)
      unstashAll()

    case _ => stash()
  }

  def ready: Receive = {
    case Register(name, receiver) =>
      if (registry.contains(name)) {
        sender ! RegistrationFailed(name)
      }
      else {
        registry += name -> receiver
        sender ! Registered(name)
      }

    case LibProcessInternalMessage(receiverName, msg) =>
      registry.get(receiverName) match {
        case Some(ref) => ref forward msg
        case None => log.warning(s"Received message for unregistered actor '$receiverName'.")
      }

    case GetRemoteRef(pid) =>
      val origSender = sender()
      remoteRefCache.get(pid) match {
        case Some(ref) => origSender ! ref
        case None =>
          val ref = context.actorOf(Props(classOf[LibProcessRemoteActor], pid.id, new InetSocketAddress(pid.ip, pid.port), config.messageMapping))
          remoteRefCache += pid -> ref
          origSender ! ref
      }

  }
}

object LibProcessManager {
  case class Register(name: String, receiver: ActorRef)
  case class LibProcessInternalMessage(receiverName: String, msg: AnyRef)
  case class GetRemoteRef(pid: PID)

  case class RegistrationFailed(name: String)
  case class Registered(name: String)
}

case class LibProcessMessage(sender: PID, msg: MessageLite)
