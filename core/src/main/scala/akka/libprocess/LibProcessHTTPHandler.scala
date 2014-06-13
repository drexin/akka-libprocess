package akka.libprocess

import akka.actor._
import spray.http.{HttpResponse, HttpMethods, Uri, HttpRequest}
import scala.util.{Failure, Success}
import akka.libprocess.LibProcessManager.LibProcessInternalMessage

private [libprocess] class LibProcessHTTPHandler(parser: MessageParser) extends Actor with ActorLogging {
  val manager = LibProcess(context.system).manager

  val path = "/([^/]+)/(.*)".r

  override def receive = {
    case HttpRequest(HttpMethods.POST, Uri.Path(path(receiverName, "PING")), headers, entity, _) =>
      sender ! HttpResponse(200)

    case HttpRequest(HttpMethods.POST, Uri.Path(path(receiverName, cls)), headers, entity, _) =>
      val parsed = parser.parse(cls.toString, entity.data.toByteArray)

      parsed match {
        case Success(msg) =>
          manager ! LibProcessInternalMessage(receiverName.toString, msg)
        case Failure(t) =>
          log.error(t, s"Failed to parse body of type $cls")
      }
  }
}
