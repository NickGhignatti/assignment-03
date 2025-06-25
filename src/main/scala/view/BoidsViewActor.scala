package view

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

import scalafx.application.Platform
import scalafx.scene.paint.Color

import model.BoidsModel
import scala.util.{Failure, Success}

object BoidsViewActor {
  sealed trait Command
  private case class UpdateView() extends Command
  private case class PositionsUpdated() extends Command
  private case class PositionError(exception: Throwable) extends Command

  def apply(view: BoidsView, model: ActorRef[BoidsModel.Command]): Behavior[Command] =
    Behaviors.setup { context =>
      new BoidsViewActor(context, view, model).ready()
    }
}

class BoidsViewActor(
                      context: ActorContext[BoidsViewActor.Command],
                      view: BoidsView, model: ActorRef[BoidsModel.Command]
                    ) {
  import BoidsViewActor._

  private var timer: Option[Cancellable] = None

  private def startTimer(): Unit =
    import scala.concurrent.duration.*

    timer = Some(
      context.system.scheduler.scheduleAtFixedRate(
        40.millis,
        40.millis
      )(() => context.self ! UpdateView())(context.executionContext)
    )

  private def stopTimer(): Unit = timer.get.cancel()

  private def ready(): Behavior[BoidsViewActor.Command] =
    view.startButton.onAction = _ => {
      view.startButton.disable = true
      view.pauseButton.disable = false
      model ! BoidsModel.CreateBoids(view.boidInput.text.value.toIntOption.getOrElse(0))
      startTimer()
    }
    view.pauseButton.onAction = _ => {
      timer match
        case Some(x) =>
          if x.isCancelled then startTimer()
          else stopTimer()
        case None =>
    }
    view.resetButton.onAction = _ => {
      view.startButton.disable = false
      view.pauseButton.disable = true
      model ! BoidsModel.ResetBoids()
      stopTimer()
      clearCanvas()
    }
    view.alignmentSlider.setOnMouseReleased({ _ =>
      model ! BoidsModel.UpdateAlignment(view.alignmentSlider.value.doubleValue())
    })
    view.cohesionSlider.setOnMouseReleased({ _ =>
      model ! BoidsModel.UpdateCohesion(view.cohesionSlider.value.doubleValue())
    })
    view.separationSlider.setOnMouseReleased({ _ =>
      model ! BoidsModel.UpdateSeparation(view.separationSlider.value.doubleValue())
    })
    Behaviors.receiveMessage {
      case UpdateView() =>
        import akka.util.Timeout
        import scala.concurrent.duration._

        implicit val timeout: Timeout = Timeout(100.millis)
        implicit val scheduler: Scheduler = context.system.scheduler

        context.pipeToSelf(model.ask(BoidsModel.UpdateBoids.apply)) {
          case Success(boids) => {
            drawBoids(boids.map(b => b.position), boids.size, 25.0)
            PositionsUpdated()
          }
          case Failure(exception) =>
            PositionError(exception)
        }
        Behaviors.same
      case PositionsUpdated() =>
        Behaviors.same
      case PositionError(exception) =>
        println(s"Error updating positions: ${exception.getMessage}")
        Behaviors.same
    }

  private def drawBoids(positions: List[(Double, Double)], boidCount: Int, fps: Double): Unit = {
    Platform.runLater {
      val gc = view.canvas.graphicsContext2D
      gc.clearRect(0, 0, view.canvas.width.value, view.canvas.height.value)

      gc.fill = Color.Black
      positions.foreach { case (x, y) =>
        gc.fillOval(x, y, 5, 5)
      }

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