package model

import akka.actor.typed.ActorRef

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

  def getBoids: List[ActorRef[Boid.Command]] = boids

  def createBoids(count: Int): Unit =
    boids = List.fill(count) {
      val x = -width / 2 + Math.random * width
      val y = -height / 2 + Math.random * height
      val vx = Math.random * maxSpeed / 2 - maxSpeed / 4
      val vy = Math.random * maxSpeed / 2 - maxSpeed / 4
      context.spawn(Boid(BoidState(x, y, vx, vy), params), s"boid-${context.self.path.name}-${context.self.path.name}")
    }

  def reset(): Unit = boids = List()

  def update(): Unit = {
    for (boid <- boids) {
      boid ! Boid.Update(boids.filterNot(_ == boid))
    }
  }
}
