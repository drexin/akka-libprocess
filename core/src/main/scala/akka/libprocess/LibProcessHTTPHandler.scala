package akka.libprocess

import akka.actor._
import akka.libprocess.LibProcessManager.LibProcessInternalMessage
import akka.libprocess.serde.{MessageSerDe, TransportMessage}
import spray.can.Http
import spray.http.{HttpResponse, HttpMethods, HttpRequest, Uri}

import scala.util.{Failure, Success}

private [libprocess] class LibProcessHTTPHandler(messageSerDe: MessageSerDe) extends Actor with ActorLogging {
  val manager = LibProcess(context.system).manager

  val pathRegex = "/([^/]+)/(.+)".r
  val pidRegex = "libprocess/(.+)".r

  override def receive = {
    case HttpRequest(HttpMethods.POST, Uri.Path(pathRegex(receiverName, cls)), headers, entity, _) =>
      val parsed = messageSerDe.deserialize(TransportMessage(cls.toString, entity.data.toByteArray))
      val pidRegex(addressString) = headers.find(_.lowercaseName == "user-agent").get.value
      val pid = PID.fromAddressString(addressString)

      parsed match {
        case Success(msg) =>
          manager ! LibProcessInternalMessage(pid, receiverName.toString, msg)
        case Failure(t) =>
          log.error(t, s"Failed to parse body of type $cls")
      }
      sender() ! HttpResponse(200)

    case _: Http.CloseCommand =>
      log.info("Received close")
      context.stop(self)
  }
}
