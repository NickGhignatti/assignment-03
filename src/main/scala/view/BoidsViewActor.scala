package view

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import scalafx.application.Platform
import scalafx.scene.paint.Color

object BoidsViewActor {
  sealed trait Command
  case class UpdateView(positions: List[(Double, Double)], boidCount: Int, fps: Double) extends Command
  case object ClearCanvas extends Command

  def apply(view: BoidsView): Behavior[Command] = Behaviors.setup { context =>
//    Platform.runLater {
//      view.startButton.onAction = _ => context.self ! StartSimulation(view.boidInput.text.value.toIntOption.getOrElse(0))
//      view.pauseButton.onAction = _ => context.self ! PauseResumeSimulation()
//      view.resetButton.onAction = _ => context.self ! ResetSimulation()
//    }

    new BoidsViewActor(view).ready()
  }

  private case class StartSimulation(boidsCount: Int) extends Command
  private case class PauseResumeSimulation() extends Command
  private case class ResetSimulation() extends Command
}

class BoidsViewActor(view: BoidsView) {
  import BoidsViewActor._

  def ready(): Behavior[Command] = Behaviors.receiveMessage {
    case UpdateView(positions, boidCount, fps) =>
      drawBoids(positions, boidCount, fps)
      Behaviors.same

    case ClearCanvas =>
      clearCanvas()
      Behaviors.same

    case StartSimulation(boidsCount) =>
      println(s"In actor starting simulation with $boidsCount boids")
      Behaviors.same

    case PauseResumeSimulation() =>
      Behaviors.same

    case ResetSimulation() =>
      Behaviors.same
  }

  private def drawBoids(positions: List[(Double, Double)], boidCount: Int, fps: Double): Unit = {
    Platform.runLater {
      val gc = view.canvas.graphicsContext2D
      gc.clearRect(0, 0, view.canvas.width.value, view.canvas.height.value)

      // Draw boids as circles
      gc.fill = Color.Blue
      positions.foreach { case (x, y) =>
        gc.fillOval(x, y, 5, 5)
      }

      // Update info text
      view.infoText.text = s"Num. Boids: $boidCount\nFramerate: ${fps.round}"
    }
  }

  private def clearCanvas(): Unit = {
    Platform.runLater {
      val gc = view.canvas.graphicsContext2D
      gc.clearRect(0, 0, view.canvas.width.value, view.canvas.height.value)
      view.infoText.text = "Num. Boids: 0\nFramerate: 0"
    }
  }
}