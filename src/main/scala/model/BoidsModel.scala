package model

import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.scaladsl.AskPattern.Askable

import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global

object BoidsModel {
  def apply(): Behavior[Command] =
    Behaviors.setup(context => new BoidsModel(context))

  sealed trait Command
  final case class UpdateBoids(replyTo: ActorRef[List[Boid]]) extends Command
  final case class CreateBoids(quantity: Int) extends Command
  private final case class UpdateFailed() extends Command
  private final case class UpdateFinished() extends Command
}

class BoidsModel(context: ActorContext[BoidsModel.Command]) extends AbstractBehavior[BoidsModel.Command](context) {
  var alignment: Double = 1.0
  var cohesion: Double = 1.0
  var separation: Double = 1.0
  val width: Double = 800.0
  val height: Double = 800.0
  val maxSpeed: Double = 4.0
  val avoidanceRadius: Double = 20.0
  val perceptionRadius: Double = 50.0

  var boids: List[ActorRef[Boid.Command]] = List.empty
  var effectiveBoids: List[Boid] = List.empty

  override def onMessage(msg: BoidsModel.Command): Behavior[BoidsModel.Command] =
    msg match {
      case BoidsModel.UpdateBoids(replyTo) =>
        import akka.util.Timeout
        import scala.concurrent.duration._

        implicit val timeout: Timeout = Timeout(20.millis)
        implicit val scheduler: Scheduler = context.system.scheduler

        context.pipeToSelf(Future.sequence(boids.map(boid => boid.ask(Boid.GetPosition.apply)))) {
          case Success(allBoids) =>
            boids.foreach { boid =>
              boid ! Boid.UpdatePosition(allBoids, this)
            }
            effectiveBoids =  allBoids
            replyTo ! effectiveBoids
            BoidsModel.UpdateFinished()
          case Failure(_) =>
            BoidsModel.UpdateFailed()
        }
        this

      case BoidsModel.UpdateFinished() =>
        this

      case BoidsModel.CreateBoids(quantity) =>
        for (i <- 0 until quantity) {
          val boid = Boid(this)
          boids = boids :+ context.spawn(boid, s"boid-$i")
        }
        this
    }
}