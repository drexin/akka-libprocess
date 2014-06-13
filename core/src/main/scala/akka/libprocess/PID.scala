package akka.libprocess

case class PID(ip: String, port: Int, id: String) {
  def toAddressString = s"$id@$ip:$port"
}
