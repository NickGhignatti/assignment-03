package model

import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import model.Boid.GetPosition

import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class BoidsModel {
  var boids: List[ActorRef[Boid.Command]] = List()
  var separation: Double = 1.0
  var alignment: Double = 1.0
  var cohesion: Double = 1.0
  val width: Double = 800.0
  val height: Double = 800.0
  val maxSpeed: Double = 4.0
  val perceptionRadius: Double = 50.0
  val avoidanceRadius: Double = 20.0
  private val actorSystem: akka.actor.typed.ActorSystem[Nothing] = akka.actor.typed.ActorSystem(Behaviors.empty, "BoidsSystem")

  def getBoids: List[ActorRef[Boid.Command]] = boids

  def createBoids(count: Int): Unit =
    for (i <- 1 to count) {
      val x = -width / 2 + Math.random * width
      val y = -height / 2 + Math.random * height
      val vx = Math.random * maxSpeed / 2 - maxSpeed / 4
      val vy = Math.random * maxSpeed / 2 - maxSpeed / 4
      boids = boids :+ actorSystem.systemActorOf(Boid(BoidState(x, y, vx, vy), this), s"boid-$i")
    }

  def reset(): Unit = boids = List()

  def update(): Unit = {
    for (boid <- boids) {
      boid ! Boid.Update(boids.filterNot(_ == boid))
    }
  }

  def getBoidsStates: List[BoidState] =
    import akka.actor.typed.scaladsl.AskPattern._
    import akka.util.Timeout
    import scala.concurrent.duration._

    implicit val timeout: Timeout = 3.millis
    implicit val scheduler: Scheduler = actorSystem.scheduler
    implicit val executionContext: ExecutionContextExecutor = actorSystem.executionContext
    val boidsFutures: List[Future[BoidState]] = boids.map { ref =>
      ref.ask(GetPosition)
        .mapTo[BoidState]
        .recover { case _ => null }
    }

    val futureList: Future[List[BoidState]] = Future.sequence(boidsFutures)

    // Block and get the result (with timeout)
    Await.result(futureList, 800.millis).filter(_ != null)
}
