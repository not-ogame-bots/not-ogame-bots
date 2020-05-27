package not.ogame.bots.ghost

import java.util.UUID

import cats.effect.Resource
import cats.implicits._
import monix.catnap.ConcurrentQueue
import monix.eval.Task
import monix.execution.BufferCapacity
import monix.execution.Scheduler.Implicits.global
import monix.reactive.subjects.ConcurrentSubject
import monix.reactive.{Consumer, MulticastStrategy}

class ResourceTest extends munit.FunSuite {
  test("failing resource") {
    val r = Resource
      .make(Task.eval(println("acquire")))(_ => Task.eval(println("release")))
      .use { _ =>
        val executor = new SimpleTaskExecutor()
        Task.raceMany(List(executor.run(), executor.execute().void.handleError(_ => ()) >> Task.never))
      }
      .restartUntil(_ => false)

    r.runSyncUnsafe()
  }

  test("asdasd") {
    Task
      .raiseError[Unit](new RuntimeException("asd"))
      .flatMap { _ =>
        Task.eval(println("asdasd"))
      }
      .runSyncUnsafe()
  }

  class SimpleTaskExecutor {
    private val subject = ConcurrentSubject(MulticastStrategy.publish[UUID])
    private val queue = ConcurrentQueue[Task].unsafe[UUID](BufferCapacity.Unbounded())

    def run(): Task[Unit] = {
      queue.poll.flatMap { u =>
        println(u)
        Task.fromFuture(subject.onNext(u)).void >> run()
      }
    }

    def execute() = {
      val uuid = UUID.randomUUID()
      Task
        .parMap2(
          queue.offer(uuid),
          subject
            .filter { u =>
              u == uuid
            }
            .consumeWith(Consumer.head)
        ) { (a, b) =>
          b
        }
        .asyncBoundary
        .flatMap(_ => Task.raiseError(new RuntimeException("Asd")))
    }
  }
}
