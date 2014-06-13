package akka.libprocess

import java.net.InetSocketAddress
import akka.actor._
import akka.io.{IO, Tcp}
import akka.io.Tcp.{Write, CommandFailed, Connected}
import com.google.protobuf.MessageLite
import akka.util.ByteString

private [libprocess] final class LibProcessRemoteActor(name: String, address: InetSocketAddress, messageMapping: Map[Class[_], String]) extends Actor with ActorLogging with Stash {
  import context.system

  var connection: ActorRef = _

  override def preStart(): Unit = {
    IO(Tcp) ! Tcp.Connect(address)
  }

  override def postStop(): Unit = {
    IO(Tcp) ! Tcp.Close
  }

  def receive = {
    case Connected(remote, _) =>
      log.info(s"Connected to remote libprocess actor at $remote.")
      connection = sender()
      connection ! Tcp.Register(self)
      context.become(ready)
      unstashAll()

    case CommandFailed(cmd: Tcp.Connect) =>
      log.error(s"Failed to connect to remote libprocess at ${cmd.remoteAddress}.")

    case x => stash()
  }

  def ready: Receive = {
    case LibProcessMessage(pid, msg) =>
      log.info(s"$messageMapping")
      messageMapping.get(msg.getClass) match {
        case Some(msgType) => connection ! Write(formatMessage(pid, msgType, msg))
        case None => log.warning(s"Could not send message with unmapped type ${msg.getClass.getName}")
      }

    case CommandFailed(cmd: Tcp.Write) =>
      log.error(s"Failed to send message to remote actor '$name' at $address.")
  }

  def formatMessage(pid: PID, msgType: String, msg: MessageLite): ByteString = {
    val builder = ByteString.newBuilder

    val payload = msg.toByteArray

    builder append ByteString(s"POST /master/$msgType HTTP/1.0\r\n")
    builder append ByteString(s"User-Agent: libprocess/${pid.toAddressString}\r\n")
    builder append ByteString("Connection: Keep-Alive\r\n")
    builder append ByteString("Connection: Keep-Alive\r\n")
    builder append ByteString(s"Content-Length: ${payload.size}\r\n")
    builder append ByteString("\r\n")

    builder append ByteString(payload)

    builder.result()
  }
}
