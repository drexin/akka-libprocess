package akka.libprocess

import org.apache.mesos.Protos.FrameworkInfo
import mesos.internal.Messages.RegisterFrameworkMessage
import akka.actor.{Props, ActorSystem}

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
    case x => x ! LibProcessMessage(PID("192.168.2.101", 7070, "slave"), msg)
  }
}

class MyReceiver extends LibProcessActor {
  val name = "slave"

  def lpReceive = {
    case x =>
      log.info(s"received $x")
  }
}
