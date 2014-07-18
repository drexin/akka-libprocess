#akka-libprocess

##Usage

First of all we need to think about our message format. akka-libprocess is serialization format agnostic,
so any format can be used. Usually we use protobuf. To de-/serialize messages, we need to provide a class
that extends `akka.libprocess.serde.MessageSerDe` and takes a `com.typesafe.config.Config` as constructor parameter.
The interface looks as follows:

```scala
trait MessageSerDe {
  def deserialize(message: TransportMessage): Try[AnyRef]
  def serialize(obj: AnyRef): Try[TransportMessage]
}
```

The `TransportMessage` will contain the message name as a `String` and the raw data as `Array[Byte]`.
A typical `MessageSerDe` would look like this:

```scala
class MyMessageSerDe(config: Config) extends MessageSerDe {
  def deserialize(message: TransportMessage): Try[AnyRef] = message.messageName match {
    case "some.MyClass" => MyClass.fromBytes(message.data)
    // ...
  }

  def serialize(obj: AnyRef): Try[TransportMessage] = obj match {
    case m: MyClass => Success(TransportMessage("some.MyClass", m.toByteArray)
    case _ => Failure(new Exception(s"Unknown message type ${obj.getClass.getCanonicalName}"))
  }
}
```

In our application we have to create the `LibProcessExtension` and can register our local actors
with the `LibProcessManager`.

```scala
val system = ActorSystem("MySystem")

val someActorRef = system.actorOf(...)

LibProcess(system).manager ! Register("my-actor", someActorRef)
```

To communicate with a remote node, we can ask the `LibProcessExtension` for a remote ref by calling `remoteRef`:

```scala
LibProcess(system).remoteRef(PID("127.0.0.1", 5050, "master"))
```

`remoteRef` returns a `Future[ActorRef]`. The contained `ActorRef` will be a proxy to the libprocess
process on the remote node identified by the `PID`.

When sending messages to a remote ref, you have to wrap them in a `LibProcessMessage` to let the remote process
know to whom it should reply. The `LibProcessMessage` takes the receiver name (which is equal to the name used to
register with the `LibProcessManager`) and the original message:

```scala
remoteRef ! LibProcessMessage("my-actor", MyMessage(...))
```

##Config

The only thing that needs to be configured id the message de-/serialization.
This is done by setting the `akka.libprocess.message-serde` field to a relative path.
This path will be used to retrieve the actual config for the MessageSerDe. The config
section must contain a field `fqcn` which is the fully qualified class name of the MessageSerDe.

```
akka {
  libprocess {
    message-serde = "my-serde"

    my-serde {
      fqcn = "foo.bar.MyMessageSerDe"
    }
  }
}
```

Other settings are:

```
akka {
  libprocess {
    address = "127.0.0.1"
    port = 0
    timeout = 5s
    connection-retries = 5
    retry-timeout = 30s
  }
}
```

The address and the port are used to listen for remote messages and have to be accessible from all other nodes.
