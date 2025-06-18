package model

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object Boid {
  def apply(model: BoidsModel): Behavior[Command] =
    val x = -model.width / 2 + Math.random * model.width
    val y = -model.height / 2 + Math.random * model.height
    val vx = Math.random * model.maxSpeed / 2 - model.maxSpeed / 4
    val vy = Math.random * model.maxSpeed / 2 - model.maxSpeed / 4
    Behaviors.setup(context => new Boid(context, (x, y), (vx, vy)))

  sealed trait Command
  final case class UpdatePosition(boids: List[Boid], model: BoidsModel) extends Command
  final case class GetPosition(replyTo: ActorRef[Boid]) extends Command
}

class Boid(context: ActorContext[Boid.Command], var position: (Double, Double), var velocity: (Double, Double)) extends AbstractBehavior[Boid.Command](context) {
  import Boid._

  private def currentPosition(): (Double, Double) = position
  private def currentVelocity(): (Double, Double) = velocity

  private def distanceTo(other: Boid): Double = {
    Math.sqrt(Math.pow(position._1 - other.currentPosition()._1, 2) + Math.pow(position._2 - other.currentPosition()._2, 2))
  }

  private def normalizeVector(vx: Double, vy: Double): (Double, Double) = {
    val length = Math.sqrt(vx * vx + vy * vy)
    if (length == 0) (0.0, 0.0) else (vx / length, vy / length)
  }

  private def calculateAlignment(nearbyBoids: List[Boid]): (Double, Double) = {
    if (nearbyBoids.isEmpty) return (0.0, 0.0)

    val avgVx = nearbyBoids.map(_.currentVelocity()._1).sum / nearbyBoids.size
    val avgVy = nearbyBoids.map(_.currentVelocity()._2).sum / nearbyBoids.size

    normalizeVector(avgVx - velocity._1, avgVy - velocity._2)
  }

  private def calculateCohesion(nearbyBoids: List[Boid]): (Double, Double) = {
    if (nearbyBoids.isEmpty) return (0.0, 0.0)

    val centerX = nearbyBoids.map(_.currentPosition()._1).sum / nearbyBoids.size
    val centerY = nearbyBoids.map(_.currentPosition()._2).sum / nearbyBoids.size

    normalizeVector(centerX - position._1, centerY - position._2)
  }

  private def calculateSeparation(nearbyBoids: List[Boid], model: BoidsModel): (Double, Double) = {
    val nearbyBoidsWithinAvoidance = nearbyBoids.filter(b => distanceTo(b) < model.avoidanceRadius)
    if (nearbyBoidsWithinAvoidance.isEmpty) return (0.0, 0.0)

    val separationX = nearbyBoidsWithinAvoidance.map(b => position._1 - b.currentPosition()._1).sum / nearbyBoidsWithinAvoidance.size
    val separationY = nearbyBoidsWithinAvoidance.map(b => position._2 - b.currentPosition()._2).sum / nearbyBoidsWithinAvoidance.size

    normalizeVector(separationX, separationY)
  }

  private def updatePosition(neighbors: List[Boid], model: BoidsModel): Unit = {
    // updating velocity
    val (alignmentX, alignmentY) = calculateAlignment(neighbors)
    val (cohesionX, cohesionY) = calculateCohesion(neighbors)
    val (separationX, separationY) = calculateSeparation(neighbors, model)

    var newVx = velocity._1 + alignmentX * model.alignment + cohesionX * model.cohesion + separationX * model.separation
    var newVy = velocity._2 + alignmentY * model.alignment + cohesionY * model.cohesion + separationY * model.separation

    val speed = Math.sqrt(newVx * newVx + newVy * newVy)

    if (speed > model.maxSpeed) {
      val (normVx, normVy) = normalizeVector(newVx, newVy)
      newVx = normVx * model.maxSpeed
      newVy = normVy * model.maxSpeed
    }

    var newX = position._1 + newVx
    var newY = position._2 + newVy

    // wrap around edges
    if (newX < 0) newX += model.width
    if (newX >= model.width) newX -= model.width
    if (newY < 0) newY += model.height
    if (newY >= model.height) newY -= model.height

    position = (newX, newY)
    velocity = (newVx, newVy)
  }

  override def onMessage(msg: Command): Behavior[Command] = msg match
    case GetPosition(replyTo) =>
      replyTo ! this
      this
    case UpdatePosition(boids, model) =>
      updatePosition(boids, model)
      this
}