package akka.libprocess

case class PID(ip: String, port: Int, id: String) {
  def toAddressString = s"$id@$ip:$port"
}

object PID {
  val addressStringRegex = """^([^@]+)@(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}):(\d+)$""".r

  def fromAddressString(pidStr: String): PID = pidStr match {
    case addressStringRegex(id, ip, port) => PID(ip, port.toInt, id)
  }
}
