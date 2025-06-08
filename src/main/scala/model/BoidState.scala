package model

final case class BoidState(x: Double, y: Double, vx: Double, vy: Double) {
  def distanceTo(other: BoidState): Double = {
    Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2))
  }
}
