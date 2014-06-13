package akka.libprocess

import akka.actor._
import java.net.InetSocketAddress
import scala.concurrent.Future
import akka.libprocess.LibProcessManager.GetRemoteRef
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

object LibProcess extends ExtensionId[LibProcessExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): LibProcessExtension =
    new LibProcessExtension(system)

  override def lookup(): ExtensionId[_ <: Extension] = this
}

class LibProcessExtension(system: ExtendedActorSystem) extends Extension {
  val config = system.settings.config.getConfig("akka.libprocess")
  val manager = system.actorOf(Props(classOf[LibProcessManager], new LibProcessConfig(config)))

  def remoteRef(pid: PID): Future[ActorRef] = {
    implicit val timeout: Timeout = config.getDuration("timeout", TimeUnit.MILLISECONDS).millis
    (manager ? GetRemoteRef(pid)).mapTo[ActorRef]
  }
}
