package model

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}

import scala.language.postfixOps

object Boid {
  sealed trait Command
  final case class Update(boids: List[ActorRef[Command]]) extends Command
  final case class GetPosition(replyTo: ActorRef[BoidState]) extends Command

  private def normalizeVector(vx: Double, vy: Double): (Double, Double) = {
    val length = Math.sqrt(vx * vx + vy * vy)
    if (length == 0) (0.0, 0.0) else (vx / length, vy / length)
  }

  private def calculateAlignment(current: BoidState, nearbyBoids: List[BoidState]): (Double, Double) = {
    if (nearbyBoids.isEmpty) return (0.0, 0.0)

    val avgVx = nearbyBoids.map(_.vx).sum / nearbyBoids.size
    val avgVy = nearbyBoids.map(_.vy).sum / nearbyBoids.size

    normalizeVector(avgVx - current.vx, avgVy - current.vy)
  }

  private def calculateCohesion(current: BoidState, nearbyBoids: List[BoidState]): (Double, Double) = {
    if (nearbyBoids.isEmpty) return (0.0, 0.0)

    val centerX = nearbyBoids.map(_.x).sum / nearbyBoids.size
    val centerY = nearbyBoids.map(_.y).sum / nearbyBoids.size

    normalizeVector(centerX - current.x, centerY - current.y)
  }

  private def calculateSeparation(current: BoidState, nearbyBoids: List[BoidState], model: BoidsModel): (Double, Double) = {
    val nearbyBoidsWithinAvoidance = nearbyBoids.filter(b => current.distanceTo(b) < model.avoidanceRadius)
    if (nearbyBoidsWithinAvoidance.isEmpty) return (0.0, 0.0)

    val separationX = nearbyBoidsWithinAvoidance.map(b => current.x - b.x).sum / nearbyBoidsWithinAvoidance.size
    val separationY = nearbyBoidsWithinAvoidance.map(b => current.y - b.y).sum / nearbyBoidsWithinAvoidance.size

    normalizeVector(separationX, separationY)
  }

  private def updateState(state: BoidState, neighbors: List[BoidState], model: BoidsModel): BoidState = {
    // updating velocity
    val (alignmentX, alignmentY) = calculateAlignment(state, neighbors)
    val (cohesionX, cohesionY) = calculateCohesion(state, neighbors)
    val (separationX, separationY) = calculateSeparation(state, neighbors, model)

    var newVx = state.vx + alignmentX * model.alignment + cohesionX * model.cohesion + separationX * model.separation
    var newVy = state.vy + alignmentY * model.alignment + cohesionY * model.cohesion + separationY * model.separation

    val speed = Math.sqrt(newVx * newVx + newVy * newVy)

    if (speed > model.maxSpeed) {
      val (normVx, normVy) = normalizeVector(newVx, newVy)
      newVx = normVx * model.maxSpeed
      newVy = normVy * model.maxSpeed
    }

    var newX = state.x + newVx
    var newY = state.y + newVy

    // wrap around edges
    if (newX < 0) newX += model.width
    if (newX >= model.width) newX -= model.width
    if (newY < 0) newY += model.height
    if (newY >= model.height) newY -= model.height

    // updating position
    BoidState(newX, newY, newVx, newVy)
  }

  def apply(initialState: BoidState, model: BoidsModel): Behavior[Command] =
    Behaviors.setup { context =>
      var currentState = initialState
      Behaviors.receive { (context, message) =>
        message match {
          case Update(boids) =>
            import akka.actor.typed.scaladsl.AskPattern._
            import akka.actor.typed.Scheduler
            import akka.util.Timeout
            import scala.concurrent.duration._
            import context.executionContext

            implicit val timeout: Timeout = 20.millis
            implicit val scheduler: Scheduler = context.system.scheduler
            val boidsFutures = boids.filterNot(_ == context.self).map { ref =>
              ref.ask(GetPosition)
                .mapTo[BoidState]
                .recover { case _ => null }
            }

            scala.concurrent.Future.sequence(boidsFutures).foreach { positions =>
              val neighbors = positions.filter(pos => currentState.distanceTo(pos) < model.perceptionRadius)
              currentState = updateState(currentState, neighbors, model)
            }
            Behaviors.same

          case GetPosition(replyTo) =>
            replyTo ! currentState
            Behaviors.same
        }
      }
    }
}