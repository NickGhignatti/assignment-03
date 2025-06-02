package controller

import model._
import view._
import scalafx.scene.paint.Color

class BoidsController(model: BoidsModel, view: BoidsView) {
  private val frameRate = 25
  private var running = false

  private val timer = new javafx.animation.AnimationTimer {
    private var lastUpdate: Long = 0L
    private val intervalNanos = (1e9 / frameRate).toLong  // 1e9 nanoseconds in a second

    override def handle(now: Long): Unit = {
      if (running && now - lastUpdate >= intervalNanos) {
        model.update()
        draw()
        lastUpdate = now
      }
    }
  }

  view.startButton.onAction = _ => {
    val count = view.boidInput.text.value.toIntOption.getOrElse(0)
    model.createBoids(count)
    running = true
    timer.start()
  }

  view.pauseButton.onAction = _ => {
    running = !running
  }

  view.resetButton.onAction = _ => {
    model.reset()
    running = false
  }

  view.separationSlider.value.onChange { (_, _, newVal) =>
    model.separation = newVal.doubleValue()
  }

  view.alignmentSlider.value.onChange { (_, _, newVal) =>
    model.alignment = newVal.doubleValue()
  }

  view.cohesionSlider.value.onChange { (_, _, newVal) =>
    model.cohesion = newVal.doubleValue()
  }

  private def draw(): Unit = {
    val gc = view.canvas.graphicsContext2D
    gc.clearRect(0, 0, view.canvas.width(), view.canvas.height())
    for (boid <- model.boids) {
      gc.fill = Color.Black
      gc.fillOval(boid.x, boid.y, 5, 5)
    }
    view.infoText.text = s"Num. Boids: ${model.boids.length}\nFramerate: $frameRate"
  }
}
