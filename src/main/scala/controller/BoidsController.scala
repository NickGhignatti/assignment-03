package controller

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.AskPattern.Askable
import model.BoidsModel
import model.BoidsModel.Command as ModelCommand
import view.BoidsViewActor
import view.BoidsViewActor.Command as ViewCommand
import scala.util.{Success, Failure}

import scala.concurrent.duration.*

object BoidsController {
  sealed trait Command
  case class StartSimulation(boidsCount: Int) extends Command
  case class PauseResumeSimulation() extends Command
  case class ResetSimulation() extends Command
  case class SetBoidsCount(count: Int) extends Command
  case class SimulationTick() extends Command
  case class PositionsUpdated(positions: List[(Double, Double)]) extends Command
  private case class PositionError(exception: Throwable) extends Command

  def apply(model: ActorRef[BoidsModel.Command], view: ActorRef[BoidsViewActor.Command]): Behavior[Command] =
    Behaviors.setup {
      context => new BoidsController(context, model, view).idle()
    }
}

class BoidsController(context: ActorContext[BoidsController.Command], model: ActorRef[ModelCommand], view: ActorRef[ViewCommand]) {
  import BoidsController._

  private var timer: Option[Cancellable] = None
  private var boidCount: Int = 0
  private var frameCount: Long = 0
  private var lastFrameTime: Long = System.currentTimeMillis()

  def idle(): Behavior[Command] = Behaviors.receiveMessage {
    case StartSimulation(boidsCount) =>
      this.boidCount = boidsCount
      model ! BoidsModel.CreateBoids(boidCount)
      startTimer()
      running()

    case SetBoidsCount(count) =>
      boidCount = count
      Behaviors.same

    case _ => Behaviors.same
  }

  def running(): Behavior[Command] = Behaviors.receiveMessage {
    case PauseResumeSimulation() =>
      cancelTimer()
      idle()

    case ResetSimulation() =>
      cancelTimer()
//      model ! BoidsModel.Reset
      view ! BoidsViewActor.ClearCanvas
      idle()

    case SimulationTick() =>
      model ! BoidsModel.UpdateBoids()

      import akka.util.Timeout
      import scala.concurrent.duration._

      implicit val timeout: Timeout = Timeout(50.millis)
      implicit val scheduler: Scheduler = context.system.scheduler

      context.pipeToSelf(model.ask(BoidsModel.GetBoids.apply)) {
        case Success(boids) => PositionsUpdated(boids.map(b => b.position))
        case Failure(exception) => PositionError(exception)
      }
      Behaviors.same

    case PositionError(exception) =>
//      println(exception.getMessage)
      Behaviors.same

    case PositionsUpdated(positions) =>
//      println(s"Received positions update: ${positions.size} boids")
      frameCount += 1
      val currentTime = System.currentTimeMillis()
      val fps = if (currentTime > lastFrameTime) {
        1000.0 / (currentTime - lastFrameTime)
      } else 60.0
      lastFrameTime = currentTime

      view ! BoidsViewActor.UpdateView(positions, boidCount, fps)
      Behaviors.same

    case _ => Behaviors.same
  }

  private def startTimer(): Unit = {
    timer = Some(
      context.system.scheduler.scheduleAtFixedRate(
        40.millis,
        40.millis
      )(() => context.self ! SimulationTick())(context.executionContext)
    )
  }

  private def cancelTimer(): Unit = {
    timer.foreach(_.cancel())
    timer = None
  }
}