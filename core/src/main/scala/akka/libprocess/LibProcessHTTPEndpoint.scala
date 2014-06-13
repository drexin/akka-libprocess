package akka.libprocess

import akka.io.IO
import spray.can.Http
import akka.actor._

private [libprocess] class LibProcessHTTPEndpoint(address: String, port: Int, parser: MessageParser) extends Actor with ActorLogging {
  import LibProcessHTTPEndpoint._
  import context.system

  val manager = LibProcess(system).manager

  override def preStart(): Unit = {
    IO(Http) ! Http.Bind(self, address, port)
  }

  override def postStop(): Unit = {
    IO(Http) ! Http.Unbind
  }

  def receive = {
    case Http.Bound(addr) =>
      log.info(s"Successfully bound Http endpoint to $addr.")
      context.become(ready)
      manager ! Started

    case Http.CommandFailed(cmd: Http.Bind) =>
      log.error(s"Failed to bind Http endpoint to ${cmd.endpoint}.")
      context.stop(self)
  }

  def ready: Receive = {
    case Http.Connected(remote, _) =>
      log.info(s"Incoming connection from $remote")
      val handler = context.actorOf(Props(classOf[LibProcessHTTPHandler], parser))
      sender() ! Http.Register(handler)
  }
}

private [libprocess] object LibProcessHTTPEndpoint {
  case object Started
}
