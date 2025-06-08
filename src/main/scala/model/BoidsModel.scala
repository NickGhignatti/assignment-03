package model

class BoidsModel() {
  var boids: List[Boid] = List()
  var separation: Double = 1.0
  var alignment: Double = 1.0
  var cohesion: Double = 1.0
  val width: Double = 800.0
  val height: Double = 800.0
  val maxSpeed: Double = 4.0
  val perceptionRadius: Double = 50.0
  val avoidanceRadius: Double = 20.0

  def getBoids: List[Boid] = boids

  def createBoids(count: Int): Unit =
    boids = List.fill(count)(Boid(-width / 2 + Math.random * width,
      -height / 2 + Math.random * height,
      Math.random * maxSpeed / 2 - maxSpeed / 4,
      Math.random * maxSpeed / 2 - maxSpeed / 4))

  def reset(): Unit = boids = List()

  def update(): Unit = {
    for (boid <- boids) {
      boid.updateVelocity(this)
      boid.updatePosition(this)
    }
  }
}