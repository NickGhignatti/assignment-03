package model

case class Boid(var x: Double, var y: Double, var vx: Double, var vy: Double) {

  def getPosition: (Double, Double) = (this.x, this.y)

  def getVelocity: (Double, Double) = (this.vx, this.vy)

  private def distanceTo(other: Boid): Double = {
    Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2))
  }

  private def getNearbyBoids(boidsModel: BoidsModel): List[Boid] = {
    boidsModel.getBoids.filter(b => b != this && distanceTo(b) < boidsModel.perceptionRadius)
  }

  private def normalizeVector(vx: Double, vy: Double): (Double, Double) = {
    val length = Math.sqrt(vx * vx + vy * vy)
    if (length == 0) (0.0, 0.0) else (vx / length, vy / length)
  }

  private def calculateAlignment(nearbyBoids: List[Boid]): (Double, Double) = {
    if (nearbyBoids.isEmpty) return (0.0, 0.0)

    val avgVx = nearbyBoids.map(_.vx).sum / nearbyBoids.size
    val avgVy = nearbyBoids.map(_.vy).sum / nearbyBoids.size

    normalizeVector(avgVx - this.vx, avgVy - this.vy)
  }

  private def calculateCohesion(nearbyBoids: List[Boid]): (Double, Double) = {
    if (nearbyBoids.isEmpty) return (0.0, 0.0)

    val centerX = nearbyBoids.map(_.x).sum / nearbyBoids.size
    val centerY = nearbyBoids.map(_.y).sum / nearbyBoids.size

    normalizeVector(centerX - this.x, centerY - this.y)
  }

  private def calculateSeparation(nearbyBoids: List[Boid], model: BoidsModel): (Double, Double) = {
    val nearbyBoidsWithinAvoidance = nearbyBoids.filter(b => this.distanceTo(b) < model.avoidanceRadius)
    if (nearbyBoidsWithinAvoidance.isEmpty) return (0.0, 0.0)

    val separationX = nearbyBoidsWithinAvoidance.map(b => this.x - b.x).sum / nearbyBoidsWithinAvoidance.size
    val separationY = nearbyBoidsWithinAvoidance.map(b => this.y - b.y).sum / nearbyBoidsWithinAvoidance.size

    normalizeVector(separationX, separationY)
  }

  def updatePosition(model: BoidsModel): Unit = {
    // update position
    this.x += this.vx
    this.y += this.vy

    // wrap around edges
    if (this.x < -model.width / 2) this.x += model.width
    if (this.x >= model.width / 2) this.x -= model.width
    if (this.y < -model.height / 2) this.y += model.height
    if (this.y >= model.height / 2) this.y -= model.height
  }

  def updateVelocity(model: BoidsModel): Unit = {
    val nearbyBoids = getNearbyBoids(model)

    val (alignmentX, alignmentY) = calculateAlignment(nearbyBoids)
    val (cohesionX, cohesionY) = calculateCohesion(nearbyBoids)
    val (separationX, separationY) = calculateSeparation(nearbyBoids, model)

    // update velocity according to alignment, cohesion, and separation
    this.vx += model.alignment * alignmentX + model.cohesion * cohesionX + model.separation * separationX
    this.vy += model.alignment * alignmentY + model.cohesion * cohesionY + model.separation * separationY

    // limit speed
    val speed = Math.sqrt(this.vx * this.vx + this.vy * this.vy)

    if (speed > model.maxSpeed) {
      val (nx, ny) = normalizeVector(this.vx, this.vy)
      this.vx = nx * model.maxSpeed
      this.vy = ny * model.maxSpeed
    }
  }
}

