package model

class BoidsModel(val offX: Double, val offY: Double, size: Double) {
  var boids: List[Boid] = List()
  var separation: Double = 1.0
  var alignment: Double = 1.0
  var cohesion: Double = 1.0
  val width: Double = offX + size
  val height: Double = offY + size
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
      boid.updatePosition(this, offX, offY, size)
    }
  }
}