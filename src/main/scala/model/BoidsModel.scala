package model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

class BoidsModelParams {
  var separation: Double = 1.0
  var alignment: Double = 1.0
  var cohesion: Double = 1.0
  val width: Double = 800.0
  val height: Double = 800.0
  val maxSpeed: Double = 4.0
  val perceptionRadius: Double = 50.0
  val avoidanceRadius: Double = 20.0
}

object BoidsModel {
  sealed trait Command
  final case class CreateBoids(count: Int) extends Command
  final case class UpdateBoids() extends Command

  var boids: List[ActorRef[Boid.Command]] = List.empty
  val model = new BoidsModelParams()

  def apply(): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      val actorSystem = context.system
      Behaviors.receiveMessage {
        case CreateBoids(count) =>
          println(s"Created $count boids.")
          for (i <- 1 to count) {
            val x = -model.width / 2 + Math.random * model.width
            val y = -model.height / 2 + Math.random * model.height
            val vx = Math.random * model.maxSpeed / 2 - model.maxSpeed / 4
            val vy = Math.random * model.maxSpeed / 2 - model.maxSpeed / 4
            boids = boids :+ actorSystem.systemActorOf(Boid(BoidState(x, y, vx, vy), model), s"boid-$i")
          }
          println(s"Boids: $boids")
          Behaviors.same
        case UpdateBoids() =>
          boids.foreach { boid =>
            boid ! Boid.Update(boids)
          }
          Behaviors.same
      }
    }
  }
}