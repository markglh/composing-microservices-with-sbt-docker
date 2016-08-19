# core-kinesis-client

Kinesis client common to all services using Amazon's KCL (Kinesis Client Library).

See http://docs.aws.amazon.com/streams/latest/dev/kinesis-record-processor-implementation-app-java.html
and https://github.com/aws/aws-sdk-java/tree/master/src/samples/AmazonKinesis

## Considerations When Using Kinesis in a Distributed Environment

#### Required Minimum Sharding for Read-Based Applications

From http://docs.aws.amazon.com/streams/latest/dev/kinesis-record-processor-scaling.html:

> Typically, when you use the KCL, you should ensure that the number of instances
> does not exceed the number of shards (except for failure standby purposes).
> Each shard is processed by exactly one KCL worker and has exactly one corresponding
> record processor, so you never need multiple instances to process one shard. However,
> one worker can process any number of shards, so it's fine if the number of shards
> exceeds the number of instances.

For our purposes, this means *any core service reading from Kinesis should expect to
have a minimum of 30 shards defined*, assuming that we have 30 instances of the service
running in production.

Further investigation is needed to understand the overall monetary cost of this design.
Our initial estimates reveal a minimum cost of $4,000 for any Read-based application.

Note that the primary business requirements around initial Kinesis implementations are
Write-Based.  Since this approach doesn't require the minimum number of shards to meet
or exceed the number of application instances, the cost to "event out" core activity
to analytics team should be quite a bit lower.  Any number of producers can write to
a single shard.

#### DynamoDB Checkpoint Storage

Amazon KCL uses DynamoDB to checkpoint progress through reading the stream.  When DynamoDB
tables are provisioned automatically, for this purpose, they may have a relatively high
write-throughput, which can incur additional cost to the company.  

You should make sure that the DynamoDB table used for checkpointing your stream 

1. has a reasonable write throughput defined -- especially for DevStage, QA, and PSQA environments
    
2. is cleaned up when you're done with it -- KCL will not automatically delete it for you

## Usage: Consumer

```
    import com.typesafe.config.{Config, ConfigFactory}
    import com.weightwatchers.core.eventing.consumer.KinesisConsumer
    import com.weightwatchers.core.eventing.models.ConsumerEvent

    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits.global

    implicit val system = akka.actor.ActorSystem.create()
    val kinesisConf: Config = ConfigFactory.load().getConfig("kinesis")
    val processRecord = (record: ConsumerEvent) => Future { println(s"\n[!] Incoming message: ${record.payload}\n") }
    val consumer = KinesisConsumer(processRecord, kinesisConf, "testConsumer", system) // where testConsumer is the name in the configuration
    consumer.start()
```

If you invoke the above code on two different sbt consoles in `test` mode, using a Kinesis stream with two shards,
you'll see that messages get processed by one and only one worker.

**Very important!**

When implementing a core service which reads messages off Kinesis, you will need to provide a lambda which:

- processes a given record (e.g. saving data to cassandra)
- deals with failure cases

This can be seen in the type of the "processRecord" method in the above example:

```
val processRecord: ConsumerEvent => Future[Unit] = ...
```

## Usage: Producer

The KPL Client sends messages in batches, each message creates a Future which completes upon successful send or failure.

See Amazon's documentation for more information:
https://github.com/awslabs/amazon-kinesis-producer

### Actor Based Implementation

This implementation will optionally throttle of the number of futures currently in play,
according to the `max-outstanding-requests` property.

Each future is handled within the actor and a message will be returned to the sender upon completion.

The following messages are supported:

```
    /**
      * Send a message to Kinesis, registering a callback response of [[SendSuccessful]] or [[SendFailed]] accordingly.
      */
    case class SendWithCallback(producerEvent: ProducerEvent, messageId: String = UUID_GENERATOR.generate().toString)

    /**
      * Send a message to Kinesis witout any callbacks. Fire and forget.
      */
    case class Send(producerEvent: ProducerEvent)

    /**
      * Sent to the sender in event of a successful completion.
      *
      * @param messageId        The id of the event that was sent.
      * @param userRecordResult The Kinesis data regarding the send.
      */
    case class SendSuccessful(messageId: String, userRecordResult: UserRecordResult)

    /**
      * Sent to the sender in event of a failed completion.
      *
      * @param messageId The id of the event that failed.
      * @param reason    The exception causing the failure. Likely to be of type [[UserRecordFailedException]]
      */
    case class SendFailed(messageId: String, reason: Throwable)
```



#### Within an Actor (Strongly recommended)

```
import java.util.UUID

import akka.actor.Actor
import com.typesafe.config.Config
import com.weightwatchers.core.eventing.consumer.SomeActor.DoSomething
import com.weightwatchers.core.eventing.models.ProducerEvent
import com.weightwatchers.core.eventing.producer.KinesisProducerActor
import com.weightwatchers.core.eventing.producer.KinesisProducerActor.{SendFailed, SendSuccessful, SendWithCallback}

object SomeActor {
  case object DoSomething
}

class SomeActor(kinesisConfig: Config) extends Actor {

  val kpa = context.actorOf(
    KinesisProducerActor.props(kinesisConfig, "testProducer"))

  override def receive: Receive = {
    case DoSomething =>
      //Do something exciting!
      val producerEvent = ProducerEvent(UUID.randomUUID.toString, "{Some Payload}")
      kpa ! SendWithCallback(producerEvent)

    //Callbacks from the KinesisProducerActor
    case SendSuccessful(messageId, _) =>
      println(s"Successfully sent $messageId")

    case SendFailed(messageId, reason) =>
      println(s"Failed to send $messageId, cause: ${reason.getMessage}")
  }
}
```

#### From outside of an Actor

```
   import com.typesafe.config._
   import com.weightwatchers.core.eventing.models._
   import com.weightwatchers.core.eventing.producer.KinesisProducerActor
   import com.weightwatchers.core.eventing.producer.KinesisProducerActor.Send

   implicit val system = akka.actor.ActorSystem.create()

   val kinesisConfig: Config = ConfigFactory.load().getConfig("kinesis")

   // where testProducer is the name in the configuration
   val kpa = system.actorOf(KinesisProducerActor.props(kinesisConfig, "testProducer"))

   val producerEvent = ProducerEvent(UUID.randomUUID.toString, "{Some Payload}")
   kpa ! Send(producerEvent) //Send without a callback confirmation
```


### Pure Scala based implementation (simple wrapper around KPL)

```
  import com.typesafe.config._
  import com.weightwatchers.core.eventing.models._
  import scala.concurrent.ExecutionContext.Implicits.global

  val kinesisConfig: Config = ConfigFactory.load().getConfig("kinesis")
  val producerConfig: Config = kinesisConfig.getConfig("testProducer")
  val streamName: String = producerConfig.getString("stream-name")

  val kpl = KinesisProducerKPL(kinesisConfig.getConfig("kpl"), streamName)

  val producerEvent = ProducerEvent(UUID.randomUUID.toString, "{Some Payload}")

  val callback: Future[UserRecordResult] = kpl.addUserRecord(producerEvent)

  callback onSuccess {
    case result =>
      println("Success!!")
  }

  callback onFailure {
    case ex: UserRecordFailedException =>
      println(s"Failure! ${ex.getMessage}")
    case ex =>
      println(s"Critical Failure! ${ex.getMessage}")
  }
```

## Additional Setup Instructions

The stream you have configured must already exist in AWS, e.g.

```
aws kinesis create-stream --stream-name core-test-foo --shard-count 1
```

For local development, it's expected that you already have a file called `~/.aws/credentials`,
which contains an AWS access key and secret, *under the profile 'ww'* e.g.
```
[ww]
aws_access_key_id=AKIAXXXXXXXXX999999X
aws_secret_access_key=AAAAAAAAAAAA000000000+AAAAAAAAAAAAAAAAAA
``` 

### Defining a config file in the client application


You'll need some configuration values provided in the application which leverages this library,
We recommend as a minimum:

```
kinesis {

   application-name = "YourApplicationName"

   # The name of the this producer, we can have many producers per application.
   # MUST contain the stream-name as a minimum. Any additional settings defined will override
   # defaults in the kinesis.producer reference.conf for this producer only.
   yourProducer {
      # The name of the producer stream
      stream-name = "core-test-kinesis-reliability"

      kpl {
         Region = us-east-1
      }
   }

   # The name of the consumer, we can have many consumers per application
   yourConsumer {
      checkpoint {
         # The amount of time to wait after failing to checkpoint, usually due to an exception or dynamo throttling.
         backoffMillis = 3000
         # The standard delay between (successful) checkpoints
         intervalMillis = 20000
         # The delay between notification messages sent to the parent to indicate we're ready to checkpoint
         # This is for subsequent messages after the initial notification,
         # for cases where the parent had no messages to checkpoint.
         notificationDelayMillis = 2000
      }

      kcl {
         initialPosition = "LATEST"
         stream-name = "core-test-kinesis-reliability"
         batchTimeoutSeconds = 300
      }
   }
}
```
These values will override the default reference.conf.
See `src/main/resources/reference.conf` for a complete reference configuration.
and `src/it/resources/application.conf` for a more detailed override example.

Once these are defined, you can pass them into the Kinesis producer and consumer using a config object, e.g.

```
val conf: Config = ConfigFactory.load().getConfig("kinesis")
```


# Running the core-kinesis-client reliability test

### Delete & recreate kinesisstreams and dynamo table
Execute this command in a shell.  If you don't have access to WW AWS resources, you'll need it:
```
aws kinesis delete-stream --stream-name core-test-kinesis-reliability && aws dynamodb delete-table --table-name CoreKinesisReliabilitySpec && sleep 90 && aws kinesis create-stream --stream-name core-test-kinesis-reliability --shard-count 2
```

### Running the producer-consumer test

Open up two terminals, one for the kinesis producer, and one for the kinesis consumer.


In the consumer window, run `sbt it:console`, then execute the following:

```
import com.weightwatchers.core.eventing._
val consumer = system.actorOf(SimpleKinesisConsumer.props)
```

Now, wait for two messages that look like this to appear in the consumer window:
```
2016-06-14 20:09:24,938 c.w.c.e.KinesisRecordProcessingManager - Initializing record processor for shard: shardId-000000000001
2016-06-14 20:09:24,963 c.w.c.e.KinesisRecordProcessingManager - Initializing record processor for shard: shardId-000000000000
```

We're ready to test!


In the producer window, run `sbt it:console`, then execute the following:
```
import com.weightwatchers.core.eventing._
val producer = system.actorOf(SimpleKinesisProducer.props)
import  SimpleKinesisProducer._
producer ! Start
```
As the test progresses, watch the consumer window for a message of this format:
```
**** PIT STOP OK
```

You'll see some stats logged regarding messages/sec processed, near that line.

At the end the producer will print the number of unconfirmed and failed messages (both should be 0!)



